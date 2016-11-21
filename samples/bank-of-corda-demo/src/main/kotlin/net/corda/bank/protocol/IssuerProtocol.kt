package net.corda.bank.protocol

import co.paralleluniverse.fibers.Suspendable
import net.corda.bank.api.BOC_ISSUER_PARTY
import net.corda.bank.api.BOC_ISSUER_PARTY_REF
import net.corda.core.contracts.Amount
import net.corda.core.contracts.FungibleAsset
import net.corda.core.contracts.Issued
import net.corda.core.contracts.issuedBy
import net.corda.core.crypto.Party
import net.corda.core.node.NodeInfo
import net.corda.core.node.PluginServiceHub
import net.corda.core.protocols.ProtocolLogic
import net.corda.core.protocols.StateMachineRunId
import net.corda.core.serialization.OpaqueBytes
import net.corda.core.utilities.ProgressTracker
import net.corda.protocols.CashCommand
import net.corda.protocols.CashProtocol
import net.corda.protocols.CashProtocolResult
import java.util.*

/**
 *  This protocol enables a client to request issuance of some [FungibleAsset] from a
 *  server acting as an issuer (see [Issued]) of FungibleAssets
 *
 */
object IssuerProtocol {

    data class IssuanceRequestState(val amount: Amount<Currency>, val issueToParty: Party, val issuerPartyRef: OpaqueBytes?)

    /*
     * IssuanceRequester refers to a Node acting as issuance requester of some FungibleAsset
     */
    class IssuanceRequester(val amount: Amount<Currency>, val issueToPartyName: String,
                            val otherParty: String): ProtocolLogic<IssuerProtocolResult>() {

        @Suspendable
        override fun call(): IssuerProtocolResult {

            val issueToParty = serviceHub.identityService.partyFromName(issueToPartyName)
            val bankOfCordaParty = serviceHub.identityService.partyFromName(otherParty)
            if (issueToParty == null || bankOfCordaParty == null) {
                return IssuerProtocolResult.Failed("Unable to locate ${otherParty} in Network Map Service")
            }
            else {
                val issueRequest = IssuanceRequestState(amount, issueToParty, issuerPartyRef = BOC_ISSUER_PARTY_REF)
                return sendAndReceive<IssuerProtocolResult>(bankOfCordaParty, issueRequest).unwrap { it }
            }
        }
    }

    /*
     * Issuer refers to a Node acting as a Bank Issuer of FungibleAssets
     */
    class Issuer(val otherParty: Party,
                 override val progressTracker: ProgressTracker = Issuer.tracker()): ProtocolLogic<IssuerProtocolResult>() {

        companion object {
            object AWAITING_REQUEST : ProgressTracker.Step("Awaiting issuance request")

            object ISSUING : ProgressTracker.Step("Self issuing asset")

            object TRANSFERRING : ProgressTracker.Step("Transfering asset to issuance requester")

            object SENDING_CONIFIRM : ProgressTracker.Step("Confirming asset issuance to requester")

            fun tracker() = ProgressTracker(AWAITING_REQUEST, ISSUING, TRANSFERRING, SENDING_CONIFIRM)
        }

        @Suspendable
        override fun call(): IssuerProtocolResult {

            progressTracker.currentStep = AWAITING_REQUEST
            val issueRequest = receive<IssuanceRequestState>(otherParty).unwrap { it }

            // TODO: parse request to determine Asset to issue
            try {
                val result = issueCashTo(issueRequest.amount, issueRequest.issueToParty, issueRequest.issuerPartyRef)
                var response: IssuerProtocolResult?
                if (result is CashProtocolResult.Success)
                    response = IssuerProtocolResult.Success(psm.id, "Amount ${issueRequest.amount} issued to ${issueRequest.issueToParty}")
                else
                    response = IssuerProtocolResult.Failed((result as CashProtocolResult.Failed).message)

                progressTracker.currentStep = SENDING_CONIFIRM
                send(otherParty, response)
                return response
            }
            catch(ex: Exception) {
                return IssuerProtocolResult.Failed(ex.message)
            }
        }

        @Suspendable
        private fun issueCashTo(amount: Amount<Currency>,
                                issueTo: Party, issuerPartyRef: OpaqueBytes? = BOC_ISSUER_PARTY_REF): CashProtocolResult {

            val notaryNode: NodeInfo = serviceHub.networkMapCache.notaryNodes[0]

            // invoke Cash subprotocol to issue Asset
            progressTracker.currentStep = ISSUING

            val bankOfCordaParty = serviceHub.identityService.partyFromName(BOC_ISSUER_PARTY.name)
            val issueCashProtocol = CashProtocol(CashCommand.IssueCash(
                    amount, issuerPartyRef!!, bankOfCordaParty!!, notaryNode.notaryIdentity))
            val resultIssue = subProtocol(issueCashProtocol)
            // NOTE: issueCashProtocol performs a Broadcast (which stores a local copy of the txn to the ledger)
            if (resultIssue is CashProtocolResult.Failed) {
                logger.error("Problem issuing cash: ${resultIssue.message}");
                return resultIssue
            }
            // now invoke Cash subprotocol to Move issued assetType to issue requester
            progressTracker.currentStep = TRANSFERRING
            val moveCashProtocol = CashProtocol(CashCommand.PayCash(
                    amount.issuedBy(bankOfCordaParty.ref(issuerPartyRef)), issueTo))
            val resultMove = subProtocol(moveCashProtocol) // , shareParentSessions = true)
            if (resultMove is CashProtocolResult.Success) {
                // Commit it to local storage.
                serviceHub.recordTransactions(listOf(resultMove.transaction).filterNotNull().asIterable())
            }
            return resultMove
        }

        class Service(services: PluginServiceHub) {
            init {
                services.registerProtocolInitiator(IssuanceRequester::class) {
                    Issuer(it)
                }
            }
        }
    }
}

sealed class IssuerProtocolResult {
    /**
     * @param transaction the transaction created as a result, in the case where the protocol completed successfully.
     */
    class Success(val id: StateMachineRunId, val message: String?) : IssuerProtocolResult() {
        override fun toString() = "Issuer Success($message)"

        override fun equals(other: Any?): Boolean {
            return other is IssuerProtocolResult.Success &&
                    this.id == other.id &&
                    this.message.equals(other.message)
        }
    }

    /**
     * State indicating the action undertaken failed, either directly (it is not something which requires a
     * state machine), or before a state machine was started.
     */
    class Failed(val message: String?) : IssuerProtocolResult() {
        override fun toString() = "Issuer failed($message)"
    }
}

