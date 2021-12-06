package io.egm.aqdv.service

import arrow.core.Either
import arrow.core.computations.either
import arrow.core.left
import arrow.core.right
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.coroutines.awaitObjectResult
import io.egm.aqdv.config.ApplicationProperties
import io.egm.aqdv.config.ApplicationProperties.Aqdv.KnownTimeserie
import io.egm.aqdv.model.*
import io.egm.aqdv.model.ApplicationException.AqdvException
import io.egm.kngsild.utils.toNgsiLdFormat
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

    suspend fun retrieveKnownTimeSeries(): Either<ApplicationException, List<Pair<KnownTimeserie, ScalarTimeSerie>>> =
        either {
            applicationProperties.aqdv().knownTimeseries().map {
                Pair(it, retrieveTimeSerie(it.uuid()).bind())
            }
        }

    suspend fun retrieveTimeSerie(scalarTimeSerieId: UUID): Either<ApplicationException, ScalarTimeSerie> =
        Fuel.get("/scalartimeseries/$scalarTimeSerieId/")
            .awaitObjectResult(ScalarTimeSerieDeserializer)
            .also { result ->
                logger.trace(result)
            }
            .fold(
                { data -> data.right() },
                { error -> AqdvException(error.response.responseMessage).left() }
            )

    suspend fun retrieveTimeSerieData(
        scalarTimeSerieId: UUID,
        startTime: ZonedDateTime,
        endTime: ZonedDateTime
    ): Either<ApplicationException, List<ScalarTimeSerieData>> {
        logger.debug("Retrieving data for time serie $scalarTimeSerieId between $startTime and $endTime")
        return Fuel.get(
            "/scalartimeseries/$scalarTimeSerieId/data/",
            listOf("startTime" to startTime.toNgsiLdFormat(), "endTime" to endTime.toNgsiLdFormat())
        )
        .awaitObjectResult(ScalarTimeSerieDataDeserializer)
        .also { result ->
            logger.trace(result)
        }
        .fold(
            { data -> data.right() },
            { error ->
                logger.error("Error while retrieving timeserie data ${error.response}")
                AqdvException(error.response.responseMessage).left()
            }
        )
    }
}
