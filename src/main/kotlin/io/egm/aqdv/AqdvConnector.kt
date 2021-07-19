package io.egm.aqdv

import io.egm.aqdv.service.ContextBrokerService
import io.quarkus.runtime.Quarkus
import io.quarkus.runtime.QuarkusApplication
import io.quarkus.runtime.annotations.QuarkusMain
import javax.inject.Inject

@QuarkusMain
object Main {

    @JvmStatic
    fun main(args: Array<String>) {
        Quarkus.run(AqdvConnector::class.java, *args)
    }

    class AqdvConnector(
        @Inject private val contextBrokerService: ContextBrokerService
    ): QuarkusApplication {

        override fun run(vararg args: String?): Int {
            contextBrokerService.createGenericAqdvEntity()
            Quarkus.waitForExit()
            return 0
        }
    }
}
