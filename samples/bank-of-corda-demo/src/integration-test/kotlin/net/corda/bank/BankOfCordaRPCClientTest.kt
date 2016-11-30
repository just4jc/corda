package net.corda.bank

import net.corda.bank.api.BOC_ISSUER_PARTY_REF
import net.corda.bank.flow.IssuerFlow.IssuanceRequester
import net.corda.bank.flow.IssuerFlowResult
import net.corda.client.CordaRPCClient
import net.corda.core.contracts.DOLLARS
import net.corda.core.node.services.ServiceInfo
import net.corda.core.node.services.Vault
import net.corda.node.driver.driver
import net.corda.node.services.User
import net.corda.node.services.config.configureTestSSL
import net.corda.node.services.messaging.startFlow
import net.corda.node.services.startFlowPermission
import net.corda.node.services.transactions.SimpleNotaryService
import net.corda.testing.getHostAndPort
import org.junit.Test
import rx.subjects.PublishSubject
import kotlin.test.assertTrue

class BankOfCordaRPCClientTest {

    @Test fun `test issuer flow via RPC`() {
        driver(dsl = {
            val user = User("user1", "test", permissions = setOf(startFlowPermission<IssuanceRequester>()))
            val nodeBankOfCorda = startNode("BankOfCorda", setOf(ServiceInfo(SimpleNotaryService.type)), arrayListOf(user)).get()
            val nodeBankOfCordaApiAddr = nodeBankOfCorda.config.getHostAndPort("artemisAddress")
            val bankOfCordaName = nodeBankOfCorda.nodeInfo.legalIdentity.name
            val nodeBigCorporation = startNode("BigCorporation", rpcUsers = arrayListOf(user)).get()
            val bigCorporationName = nodeBigCorporation.nodeInfo.legalIdentity.name

            // Bank of Corda RPC Client
            val bocClient = CordaRPCClient(nodeBankOfCordaApiAddr, configureTestSSL())
            bocClient.start("user1","test")
            val bocProxy = bocClient.proxy()

            // Big Corporation RPC Client
            val bigCorpClient = CordaRPCClient(nodeBankOfCordaApiAddr, configureTestSSL())
            bigCorpClient.start("user1","test")
            val bigCorpProxy = bigCorpClient.proxy()

            // Register for Bank of Corda Vault updates
            val vaultUpdatesSubject = PublishSubject.create<Vault.Update>()
            val (vault, vaultUpdates) = bocProxy.vaultAndUpdates()

            // Register for Big Corporation Vault updates
            val bigCorpVaultUpdatesSubject = PublishSubject.create<Vault.Update>()
            val (vaultBigCorp, vaultUpdatesBigCorp) = bigCorpProxy.vaultAndUpdates()

            // Kick-off actual Issuer Flow
            val result = bocProxy.startFlow(::IssuanceRequester, 1000.DOLLARS, bigCorporationName, BOC_ISSUER_PARTY_REF, bankOfCordaName).returnValue.toBlocking().first()
            assertTrue { result is IssuerFlowResult.Success }

            // Check Bank of Corda Vault Updates
            val initialVaultUpdate = Vault.Update(setOf(), vault.toSet())
            vaultUpdatesSubject.subscribe {
                update ->
                println("BOC ===> ${update} <===")
                // EXPECT TO SEE an ISSUE and a MOVE
            }
            vaultUpdates.startWith(initialVaultUpdate).subscribe(vaultUpdatesSubject)

            // Check Big Corporation Vault Updates
            val bigCorpInitialVaultUpdate = Vault.Update(setOf(), vaultBigCorp.toSet())
            bigCorpVaultUpdatesSubject.subscribe {
                update ->
                println("BigCorp ===> ${update} <===")
                // EXPECT TO SEE an ISSUE and a MOVE
            }
            vaultUpdatesBigCorp.startWith(bigCorpInitialVaultUpdate).subscribe(bigCorpVaultUpdatesSubject)

            // Check Big Corporation Vault Updates
//            val bigCorpInitialVaultUpdate = Vault.Update(setOf(), vaultBigCorp.toSet())
//            bigCorpVaultUpdatesSubject.expectEvents {
//                sequence(
//                        // ISSUE
//                        expect { update ->
//                            require(update.consumed.size == 0) { update.consumed.size }
//                            require(update.produced.size == 1) { update.produced.size }
//                        },
//                        // MOVE
//                        expect { update ->
//                            require(update.consumed.size == 1) { update.consumed.size }
//                            require(update.produced.size == 0) { update.produced.size }
//                        }
//                )
//            }

        }, isDebug = true)
    }
}