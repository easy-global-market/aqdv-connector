package io.egm.aqdv.api

import io.egm.aqdv.service.AqdvService
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/aqdv")
class AqdvResource(
    @Inject val aqdvService: AqdvService
) {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun scalarTimeSeries() =
        aqdvService.retrieveTimeSeries()
            .fold({
                it
            }, {
                it
            })
}
