package io.egm.aqdv.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.coroutines.awaitObjectResult
import io.egm.aqdv.config.ApplicationProperties
import io.egm.aqdv.model.*
import io.egm.aqdv.model.ApplicationException.AqdvException
import kotlinx.coroutines.runBlocking
import org.jboss.logging.Logger
import java.time.ZonedDateTime
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
class AqdvService(
    @Inject private val applicationProperties: ApplicationProperties,
) {

    private val logger = Logger.getLogger(javaClass)

    init {
        FuelManager.instance.basePath = applicationProperties.aqdv().url()
    }

    fun retrieveTimeSeries(): Either<ApplicationException, List<ScalarTimeSerie>> =
        runBlocking {
            Fuel.get("/scalartimeseries/")
                .awaitObjectResult(ScalarTimeSerieDeserializer)
                .fold(
                    { data -> data.right() },
                    { error -> AqdvException(error.response.responseMessage).left() }
                )
        }

    fun retrieveTimeSerieData(
        scalarTimeSerieId: UUID,
        startTime: ZonedDateTime,
        endTime: ZonedDateTime
    ): Either<ApplicationException, List<ScalarTimeSerieData>> =
        runBlocking {
            Fuel.get(
                "/scalartimeseries/$scalarTimeSerieId/data",
                listOf("startTime" to startTime, "endTime" to endTime)
            )
                .awaitObjectResult(ScalarTimeSerieDataDeserializer)
                .fold(
                    { data -> data.right() },
                    { error -> AqdvException(error.response.responseMessage).left() }
                )
        }
}
