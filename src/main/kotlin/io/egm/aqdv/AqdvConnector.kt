package io.egm.aqdv

import io.egm.aqdv.service.ScalaTimeSeriesInitializer
import io.quarkus.runtime.Quarkus
import io.quarkus.runtime.QuarkusApplication
import io.quarkus.runtime.annotations.QuarkusMain
import kotlinx.coroutines.runBlocking
import org.jboss.logging.Logger
import javax.inject.Inject

@QuarkusMain
object Main {

    private val logger = Logger.getLogger(javaClass)

    @JvmStatic
    fun main(args: Array<String>) {
        Quarkus.run(AqdvConnector::class.java, *args)
    }

    class AqdvConnector(
        @Inject private val scalaTimeSeriesInitializer: ScalaTimeSeriesInitializer
    ): QuarkusApplication {

        override fun run(vararg args: String?): Int {
            runBlocking {
                scalaTimeSeriesInitializer.initializeScalarTimeSeries().fold({
                    logger.error("Unable to create the required AQDV entities (reason: $it), exiting")
                    Quarkus.asyncExit(-1)
                }, {
                    logger.info("AQDV entities are ready to be used")
                })
            }
            Quarkus.waitForExit()
            return 0
        }
    }
}
