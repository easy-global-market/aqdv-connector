package io.egm.aqdv.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.coroutines.awaitObjectResult
import io.egm.aqdv.model.ScalarTimeSerie
import io.egm.aqdv.model.ScalarTimeSerieData
import io.egm.aqdv.model.ScalarTimeSerieDataDeserializer
import io.egm.aqdv.model.ScalarTimeSerieDeserializer
import kotlinx.coroutines.runBlocking
import org.eclipse.microprofile.config.ConfigProvider
import org.jboss.logging.Logger
import java.time.ZonedDateTime
import java.util.*
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class AqdvService {

    private val logger = Logger.getLogger(javaClass)

    init {
        FuelManager.instance.basePath = ConfigProvider.getConfig().getValue("application.aqdv.url", String::class.java)
    }

    fun retrieveTimeSeries(): Either<String, List<ScalarTimeSerie>> =
        runBlocking {
            Fuel.get("/scalartimeseries")
                .awaitObjectResult(ScalarTimeSerieDeserializer)
                .fold(
                    { data -> data.right() },
                    { error -> error.response.responseMessage.left() }
                )
        }

    fun retrieveTimeSerieData(
        scalarTimeSerieId: UUID,
        startTime: ZonedDateTime,
        endTime: ZonedDateTime
    ): Either<String, List<ScalarTimeSerieData>> =
        runBlocking {
            Fuel.get(
                "/scalartimeseries/$scalarTimeSerieId/data",
                listOf("startTime" to startTime, "endTime" to endTime)
            )
                .awaitObjectResult(ScalarTimeSerieDataDeserializer)
                .fold(
                    { data -> data.right() },
                    { error -> error.response.responseMessage.left() }
                )
        }
}
