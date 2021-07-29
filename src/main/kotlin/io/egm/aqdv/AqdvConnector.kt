package io.egm.aqdv

import io.egm.aqdv.service.ContextBrokerService
import io.quarkus.runtime.Quarkus
import io.quarkus.runtime.QuarkusApplication
import io.quarkus.runtime.annotations.QuarkusMain
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
        @Inject private val contextBrokerService: ContextBrokerService
    ): QuarkusApplication {

        override fun run(vararg args: String?): Int {
            contextBrokerService.createGenericAqdvEntity().fold({
                logger.error("Unable to create the generic AQDV entity (reason: $it), exiting")
                Quarkus.asyncExit(-1)
            }, {
                logger.info("Generic AQDV entity is ready to be used")
            })
            Quarkus.waitForExit()
            return 0
        }
    }
}
