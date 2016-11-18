package net.corda.bank

import net.corda.bank.api.BankOfCordaClientApi
import net.corda.core.node.services.ServiceInfo
import net.corda.node.driver.driver
import net.corda.node.services.transactions.SimpleNotaryService
import net.corda.testing.getHostAndPort
import net.corda.bank.api.BankOfCordaWebApi.IssueRequestParams
import org.junit.Test

class BankOfCordaHttpAPITest {

    @Test fun `test issuer protocol via Http`() {
        driver(dsl = {

            val nodeBankOfCorda = startNode("Bank of Corda", setOf(ServiceInfo(SimpleNotaryService.type))).get()
            val nodeBankOfCordaApiAddr = nodeBankOfCorda.config.getHostAndPort("webAddress")

//            val nodeBankClientAddr = startNode("MegaCorp").get().config.getHostAndPort("webAddress")

            assert(BankOfCordaClientApi(nodeBankOfCordaApiAddr).requestWebIssue(IssueRequestParams(1000, "MegaCorp")))
        }, isDebug = true)
    }
}