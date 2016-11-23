package net.corda.bank.api

import net.corda.bank.flow.IssuerFlow.IssuanceRequester
import net.corda.bank.flow.IssuerFlowResult
import net.corda.core.contracts.DOLLARS
import net.corda.core.node.ServiceHub
import net.corda.core.utilities.loggerFor
import java.time.LocalDateTime
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

// API is accessible from /api/bank. All paths specified below are relative to it.
@Path("bank")
class BankOfCordaWebApi(val services: ServiceHub) {
    data class IssueRequestParams(val amount: Int, val issueToPartyName: String)
    private companion object {
        val logger = loggerFor<BankOfCordaWebApi>()
    }
    @GET
    @Path("date")
    @Produces(MediaType.APPLICATION_JSON)
    fun getCurrentDate(): Any {
        return mapOf("date" to LocalDateTime.now(services.clock).toLocalDate())
    }
    /**
     *  Request asset issuance
     */
    @POST
    @Path("issue-asset-request")
    @Consumes(MediaType.APPLICATION_JSON)
    fun issueAssetRequest(params: IssueRequestParams): Response {
        // invoke client side of Issuer Flow: IssuanceRequester
        // The line below blocks and waits for the future to resolve.
        val result = services.invokeFlowAsync(IssuanceRequester::class.java, params.amount.DOLLARS, params.issueToPartyName, BOC_ISSUER_PARTY.name)
        if (result.resultFuture.get() is IssuerFlowResult.Success) {
            logger.info("Issue request completed successfully: ${params}")
            return Response.status(Response.Status.CREATED).build()
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build()
        }
    }
}