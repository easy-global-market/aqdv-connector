package io.egm.aqdv.service

import io.egm.aqdv.config.ApplicationProperties
import io.egm.kngsild.api.EntityService
import io.egm.kngsild.utils.AuthUtils
import org.jboss.logging.Logger
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
class ContextBrokerService(
    @Inject private val applicationProperties: ApplicationProperties,
) {

    private val logger = Logger.getLogger(javaClass)

    private val authUtils = AuthUtils(
        applicationProperties.authServer().url(),
        applicationProperties.authServer().clientId(),
        applicationProperties.authServer().clientSecret(),
        applicationProperties.authServer().grantType()
    )
    private val entityService = EntityService(applicationProperties.contextBroker().url(), authUtils)

    fun createGenericAqdvEntity() {
        logger.info("Trying to create the generic Aqdv entity: ${applicationProperties.contextBroker().entityId()}")
        entityService.retrieve(
            applicationProperties.contextBroker().entityId(),
            emptyMap(),
            applicationProperties.contextBroker().context()
        ).fold({
            logger.error("Error while retrieving entity: $it")
        }, {
            logger.info("Aqdv entity already exists, that's fine!")
        })
    }
}
