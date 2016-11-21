package net.corda.bank.api

import com.google.common.net.HostAndPort
import net.corda.bank.api.BankOfCordaWebApi.IssueRequestParams
import net.corda.bank.protocol.IssuerProtocol.IssuanceRequester
import net.corda.bank.protocol.IssuerProtocolResult
import net.corda.client.CordaRPCClient
import net.corda.client.fxutils.lift
import net.corda.core.contracts.DOLLARS
import net.corda.node.services.config.configureTestSSL
import net.corda.node.services.messaging.startProtocol
import net.corda.testing.http.HttpApi
import javax.ws.rs.core.Response

/**
 * Interface for communicating with Bank of Corda node
 */
class BankOfCordaClientApi(val hostAndPort: HostAndPort) {
    private val api = HttpApi.fromHostAndPort(hostAndPort, apiRoot)

    /**
     * HTTP API
     */
    // TODO: security controls required
    fun requestWebIssue(params: IssueRequestParams): Boolean {
        return api.postJson("issue-asset-request", params)
    }

    /**
     * RPC API
     */
    fun requestRPCIssue(params: IssueRequestParams): Boolean {
        val client = CordaRPCClient(hostAndPort, configureTestSSL())
        // TODO: privileged security controls required
        client.start("user1","test")
        val proxy = client.proxy()

        val result = proxy.startProtocol(::IssuanceRequester, params.amount.DOLLARS, params.issueToPartyName, BOC_ISSUER_PARTY.name).returnValue.toBlocking().first()
        return (result is IssuerProtocolResult.Success)
    }

    private companion object {
        private val apiRoot = "api/bank"
    }
}
