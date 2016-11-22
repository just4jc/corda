package net.corda.bank

import com.google.common.net.HostAndPort
import joptsimple.OptionParser
import net.corda.bank.api.BankOfCordaClientApi
import net.corda.bank.api.BankOfCordaWebApi.IssueRequestParams
import net.corda.bank.protocol.IssuerFlow
import net.corda.core.node.services.ServiceInfo
import net.corda.core.utilities.loggerFor
import net.corda.node.driver.driver
import net.corda.node.services.User
import net.corda.node.services.startFlowPermission
import net.corda.node.services.startFlowPermission
import net.corda.node.services.transactions.SimpleNotaryService
import org.slf4j.Logger
import kotlin.system.exitProcess

/**
 * This entry point allows for command line running of the Bank of Corda functions on nodes started by BankOfCordaDriver.kt.
 */
fun main(args: Array<String>) {
    BankOfCordaDriver().main(args)
}

private class BankOfCordaDriver {
    enum class Role {
        ISSUE_CASH_RPC,
        ISSUE_CASH_WEB,
        ISSUER
    }

    companion object {
        val logger: Logger = loggerFor<BankOfCordaDriver>()
    }

    fun main(args: Array<String>) {
        val parser = OptionParser()

        val roleArg = parser.accepts("role").withRequiredArg().ofType(Role::class.java).required()
        val options = try {
            parser.parse(*args)
        } catch (e: Exception) {
            logger.error(e.message)
            printHelp(parser)
            exitProcess(1)
        }

        // What happens next depends on the role.
        // The ISSUER will launch a Bank of Corda node
        // The ISSUE_CASH will request some Cash from the ISSUER on behalf of Big Corporation node
        // will contact the buyer and actually make something happen.
        val role = options.valueOf(roleArg)!!
        var result = true
        when (role) {
            Role.ISSUER ->  {
                driver(dsl = {
                    val user = User("user1", "test", permissions = setOf(startFlowPermission<IssuerFlow.IssuanceRequester>()))
                    startNode("Notary", setOf(ServiceInfo(SimpleNotaryService.type)))
                    startNode("BankOfCorda", rpcUsers = listOf(user))
                    startNode("BigCorporation")
                    waitForAllNodesToFinish()
                }, isDebug = true)
            }
            Role.ISSUE_CASH_RPC -> {
                logger.info("Requesting Cash via RPC ...")
                result = BankOfCordaClientApi(HostAndPort.fromString("localhost:10004")).requestRPCIssue(IssueRequestParams(1000, "BigCorporation"))
            }
            Role.ISSUE_CASH_WEB -> {
                logger.info("Requesting Cash via Web ...")
                result = BankOfCordaClientApi(HostAndPort.fromString("localhost:10005")).requestWebIssue(IssueRequestParams(1000, "BigCorporation"))
            }
        }
        if (result)
            logger.info("Successfully processed Cash Issue request")
    }

    fun printHelp(parser: OptionParser) {
        println("""
        Usage: bank-of-corda --role [ISSUE_REQUESTER|ISSUER]
        Please refer to the documentation in docs/build/index.html for more info.

        """.trimIndent())
        parser.printHelpOn(System.out)
    }
}


