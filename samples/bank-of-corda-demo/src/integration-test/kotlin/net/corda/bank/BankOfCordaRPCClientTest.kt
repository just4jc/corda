package net.corda.bank

import net.corda.bank.api.BOC_ISSUER_PARTY_REF
import net.corda.bank.flow.IssuerFlow.IssuanceRequester
import net.corda.bank.flow.IssuerFlowResult
import net.corda.client.CordaRPCClient
import net.corda.core.contracts.DOLLARS
import net.corda.core.node.services.ServiceInfo
import net.corda.node.driver.driver
import net.corda.node.services.User
import net.corda.node.services.config.configureTestSSL
import net.corda.node.services.messaging.startFlow
import net.corda.node.services.startFlowPermission
import net.corda.node.services.transactions.SimpleNotaryService
import net.corda.testing.getHostAndPort
import org.junit.Test
import kotlin.test.assertTrue

class BankOfCordaRPCClientTest {
    @Test fun `test issuer flow via RPC`() {
        driver(dsl = {
            val user = User("user1", "test", permissions = setOf(startFlowPermission<IssuanceRequester>()))
            val nodeBankOfCorda = startNode("BankOfCorda", setOf(ServiceInfo(SimpleNotaryService.type)), arrayListOf(user)).get()
            val nodeBankOfCordaApiAddr = nodeBankOfCorda.config.getHostAndPort("artemisAddress")
            val bankOfCordaName = nodeBankOfCorda.nodeInfo.legalIdentity.name
            val nodeBigCorporation = startNode("BigCorporation").get()
            val bigCorporationName = nodeBigCorporation.nodeInfo.legalIdentity.name

            val client = CordaRPCClient(nodeBankOfCordaApiAddr, configureTestSSL())
            client.start("user1","test")
            val proxy = client.proxy()

            val result = proxy.startFlow(::IssuanceRequester, 1000.DOLLARS, bigCorporationName, BOC_ISSUER_PARTY_REF, bankOfCordaName).returnValue.toBlocking().first()
            assertTrue { result is IssuerFlowResult.Success }
        }, isDebug = true)
    }
}