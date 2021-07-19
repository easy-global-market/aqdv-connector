package io.egm.aqdv.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.coroutines.awaitObjectResult
import io.egm.aqdv.model.ScalarTimeserie
import io.egm.aqdv.model.ScalarTimeserieDeserializer
import kotlinx.coroutines.runBlocking
import org.eclipse.microprofile.config.ConfigProvider
import org.jboss.logging.Logger
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class AqdvService {

    private val logger = Logger.getLogger(javaClass)

    init {
        FuelManager.instance.basePath = ConfigProvider.getConfig().getValue("application.aqdv.url", String::class.java)
    }

    fun retrieveTimeSeries(): Either<String, List<ScalarTimeserie>> =
        runBlocking {
            Fuel.get("/scalartimeseries")
                .awaitObjectResult(ScalarTimeserieDeserializer)
                .fold(
                    { data -> data.right() },
                    { error -> error.response.responseMessage.left() }
                )
        }
}
