package net.corda.bank.protocol

import co.paralleluniverse.fibers.Suspendable
import net.corda.bank.api.BOC_ISSUER_PARTY
import net.corda.bank.api.BOC_ISSUER_PARTY_REF
import net.corda.client.mock.generateIssueRef
import net.corda.core.contracts.Amount
import net.corda.core.contracts.FungibleAsset
import net.corda.core.contracts.Issued
import net.corda.core.contracts.issuedBy
import net.corda.core.crypto.Party
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StateMachineRunId
import net.corda.core.node.NodeInfo
import net.corda.core.node.PluginServiceHub
import net.corda.core.serialization.OpaqueBytes
import net.corda.core.utilities.ProgressTracker
import net.corda.flows.CashCommand
import net.corda.flows.CashFlow
import net.corda.flows.CashFlowResult
import java.util.*

/**
 *  This protocol enables a client to request issuance of some [FungibleAsset] from a
 *  server acting as an issuer (see [Issued]) of FungibleAssets
 *
 */
object IssuerFlow {

    data class IssuanceRequestState(val amount: Amount<Currency>, val issueToParty: Party, val issuerPartyRef: OpaqueBytes?)

    /*
     * IssuanceRequester refers to a Node acting as issuance requester of some FungibleAsset
     */
    class IssuanceRequester(val amount: Amount<Currency>, val issueToPartyName: String,
                            val otherParty: String): FlowLogic<IssuerFlowResult>() {

        @Suspendable
        override fun call(): IssuerFlowResult {

            val issueToParty = serviceHub.identityService.partyFromName(issueToPartyName)
            val bankOfCordaParty = serviceHub.identityService.partyFromName(otherParty)
            if (issueToParty == null || bankOfCordaParty == null) {
                return IssuerFlowResult.Failed("Unable to locate ${otherParty} in Network Map Service")
            }
            else {
                val issueRequest = IssuanceRequestState(amount, issueToParty, issuerPartyRef = BOC_ISSUER_PARTY_REF)
                return sendAndReceive<IssuerFlowResult>(bankOfCordaParty, issueRequest).unwrap { it }
            }
        }
    }

    /*
     * Issuer refers to a Node acting as a Bank Issuer of FungibleAssets
     */
    class Issuer(val otherParty: Party,
                 override val progressTracker: ProgressTracker = Issuer.tracker()): FlowLogic<IssuerFlowResult>() {

        companion object {
            object AWAITING_REQUEST : ProgressTracker.Step("Awaiting issuance request")

            object ISSUING : ProgressTracker.Step("Self issuing asset")

            object TRANSFERRING : ProgressTracker.Step("Transferring asset to issuance requester")

            object SENDING_CONIFIRM : ProgressTracker.Step("Confirming asset issuance to requester")

            fun tracker() = ProgressTracker(AWAITING_REQUEST, ISSUING, TRANSFERRING, SENDING_CONIFIRM)
        }

        @Suspendable
        override fun call(): IssuerFlowResult {

            progressTracker.currentStep = AWAITING_REQUEST
            val issueRequest = receive<IssuanceRequestState>(otherParty).unwrap { it }

            // TODO: parse request to determine Asset to issue
            try {
                val result = issueCashTo(issueRequest.amount, issueRequest.issueToParty, issueRequest.issuerPartyRef!!)
                val response = if (result is CashFlowResult.Success)
                    IssuerFlowResult.Success(fsm.id, "Amount ${issueRequest.amount} issued to ${issueRequest.issueToParty}")
                else
                    IssuerFlowResult.Failed((result as CashFlowResult.Failed).message)

                progressTracker.currentStep = SENDING_CONIFIRM
                send(otherParty, response)
                return response
            }
            catch(ex: Exception) {
                return IssuerFlowResult.Failed(ex.message)
            }
        }

        @Suspendable
        private fun issueCashTo(amount: Amount<Currency>,
                                issueTo: Party, issuerPartyRef: OpaqueBytes = BOC_ISSUER_PARTY_REF): CashFlowResult {

            val notaryNode: NodeInfo = serviceHub.networkMapCache.notaryNodes[0]

            // invoke Cash subprotocol to issue Asset
            progressTracker.currentStep = ISSUING

            val bankOfCordaParty = serviceHub.myInfo.legalIdentity
            val issueCashFlow = CashFlow(CashCommand.IssueCash(
                    amount, issuerPartyRef, bankOfCordaParty, notaryNode.notaryIdentity))
            val resultIssue = subFlow(issueCashFlow)
            // NOTE: issueCashFlow performs a Broadcast (which stores a local copy of the txn to the ledger)
            // TODO: use Exception propagation to handle failed sub protocol execution
            if (resultIssue is CashFlowResult.Failed) {
                logger.error("Problem issuing cash: ${resultIssue.message}");
                return resultIssue
            }
            // now invoke Cash subprotocol to Move issued assetType to issue requester
            progressTracker.currentStep = TRANSFERRING
            val moveCashFlow = CashFlow(CashCommand.PayCash(
                    amount.issuedBy(bankOfCordaParty.ref(issuerPartyRef)), issueTo))
            val resultMove = subFlow(moveCashFlow)
            if (resultMove is CashFlowResult.Success) {
                // Commit it to local storage.
                serviceHub.recordTransactions(listOf(resultMove.transaction).filterNotNull().asIterable())
            }
            return resultMove
        }

        class Service(services: PluginServiceHub) {
            init {
                services.registerFlowInitiator(IssuanceRequester::class) {
                    Issuer(it)
                }
            }
        }
    }
}

sealed class IssuerFlowResult {
    /**
     * @param transaction the transaction created as a result, in the case where the protocol completed successfully.
     */
    class Success(val id: StateMachineRunId, val message: String?) : IssuerFlowResult() {
        override fun toString() = "Issuer Success($message)"

        override fun equals(other: Any?): Boolean {
            return other is IssuerFlowResult.Success &&
                    this.id == other.id &&
                    this.message.equals(other.message)
        }
    }

    /**
     * State indicating the action undertaken failed, either directly (it is not something which requires a
     * state machine), or before a state machine was started.
     */
    class Failed(val message: String?) : IssuerFlowResult() {
        override fun toString() = "Issuer failed($message)"
    }
}

