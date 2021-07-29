package io.egm.aqdv.service

import arrow.core.Either
import arrow.core.right
import io.egm.aqdv.config.ApplicationProperties
import io.egm.kngsild.api.EntityService
import io.egm.kngsild.model.ApplicationError
import io.egm.kngsild.model.ResourceNotFound
import io.egm.kngsild.utils.AuthUtils
import io.egm.kngsild.utils.ResourceLocation
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

    fun createGenericAqdvEntity(): Either<ApplicationError, ResourceLocation> {
        val aqdvEntityId = applicationProperties.contextBroker().entityId()
        logger.info("Trying to create the generic Aqdv entity: $aqdvEntityId")
        val retrieveResult = entityService.retrieve(
            applicationProperties.contextBroker().entityId(),
            emptyMap(),
            applicationProperties.contextBroker().context()
        )
        return when (retrieveResult) {
            is Either.Left -> when (retrieveResult.a) {
                is ResourceNotFound -> {
                    logger.debug("Generic Aqdv entity does not exist, creating it")
                    entityService.create(
                        """
                        {
                            "id": "$aqdvEntityId",
                            "type": "AqdvEntity",
                            "@context": "${applicationProperties.contextBroker().context()}"
                        }
                        """.trimIndent()
                    )
                }
                else -> retrieveResult
            }
            is Either.Right -> {
                logger.debug("Generic Aqdv entity already exists, continuing")
                "/ngsi-ld/v1/entities/${(retrieveResult.b["id"] as String)}".right()
            }
        }
    }
}
