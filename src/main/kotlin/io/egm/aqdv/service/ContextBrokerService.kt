package io.egm.aqdv.service

import org.jboss.logging.Logger
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class ContextBrokerService {

    private val logger = Logger.getLogger(javaClass)

    fun createGenericAqdvEntity() {
        logger.info("Trying to create the generic Aqdv entity")
    }
}
