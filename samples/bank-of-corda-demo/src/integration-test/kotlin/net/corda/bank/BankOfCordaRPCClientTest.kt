package net.corda.bank

import net.corda.bank.api.BOC_ISSUER_PARTY
import net.corda.bank.protocol.IssuerProtocol.IssuanceRequester
import net.corda.bank.protocol.IssuerProtocolResult
import net.corda.client.CordaRPCClient
import net.corda.core.contracts.DOLLARS
import net.corda.core.node.services.ServiceInfo
import net.corda.node.driver.driver
import net.corda.node.services.User
import net.corda.node.services.config.configureTestSSL
import net.corda.node.services.messaging.startProtocol
import net.corda.node.services.startProtocolPermission
import net.corda.node.services.transactions.SimpleNotaryService
import net.corda.testing.getHostAndPort
import org.junit.Test
import kotlin.test.assertTrue

class BankOfCordaRPCClientTest {

    @Test fun `test issuer protocol via RPC`() {
        driver(dsl = {

            val user = User("user1", "test", permissions = setOf(startProtocolPermission<IssuanceRequester>()))
            val nodeBankOfCorda = startNode("Bank of Corda", setOf(ServiceInfo(SimpleNotaryService.type)), arrayListOf(user)).get()
            val nodeBankOfCordaApiAddr = nodeBankOfCorda.config.getHostAndPort("artemisAddress")

            val client = CordaRPCClient(nodeBankOfCordaApiAddr, configureTestSSL())
            client.start("user1","test")
            val proxy = client.proxy()

            val result = proxy.startProtocol(::IssuanceRequester, 1000.DOLLARS, BOC_ISSUER_PARTY.name).returnValue.toBlocking().first()
            assertTrue { result is IssuerProtocolResult.Success }
        }, isDebug = true)
    }
}