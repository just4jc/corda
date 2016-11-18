package net.corda.bank.api

import net.corda.bank.protocol.IssuerProtocol.IssuanceRequester
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

    data class IssueRequestParams(val amount: Int, val issueToParty: String)
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

        val issuerToParty = services.identityService.partyFromName(params.issueToParty)
        if (issuerToParty != null) {
            // invoke client side of Issuer Protocol: IssuanceRequester
            // The line below blocks and waits for the future to resolve.
            services.invokeProtocolAsync(IssuanceRequester::class.java, params.amount.DOLLARS, issuerToParty)
            logger.info("Issue request completed successfully: ${params}")
            return Response.status(Response.Status.CREATED).build()
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build()
        }
    }
}
