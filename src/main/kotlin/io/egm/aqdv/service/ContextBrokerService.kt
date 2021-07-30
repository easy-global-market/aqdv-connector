package io.egm.aqdv.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.egm.aqdv.config.ApplicationProperties
import io.egm.aqdv.model.ApplicationException
import io.egm.aqdv.model.ApplicationException.ContextBrokerException
import io.egm.aqdv.model.ScalarTimeSerie
import io.egm.aqdv.utils.aqdvNameToNgsiLdProperty
import io.egm.kngsild.api.EntityService
import io.egm.kngsild.model.ResourceNotFound
import io.egm.kngsild.utils.*
import io.egm.kngsild.utils.JsonUtils.serializeObject
import io.egm.kngsild.utils.UriUtils.toUri
import org.jboss.logging.Logger
import java.time.ZonedDateTime
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

    fun createGenericAqdvEntity(): Either<ApplicationException, ResourceLocation> {
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
                    ).mapLeft {
                        ContextBrokerException(it.message)
                    }
                }
                else -> ContextBrokerException(retrieveResult.a.message).left()
            }
            is Either.Right -> {
                logger.debug("Generic Aqdv entity already exists, continuing")
                "/ngsi-ld/v1/entities/${(retrieveResult.b["id"] as String)}".right()
            }
        }
    }

    fun retrieveGenericAqdvEntity(keyValues: Boolean = false): Either<ApplicationException, NgsildEntity> {
        val params = if (keyValues)
            mapOf("options" to "keyValues")
        else
            emptyMap()

        return entityService.retrieve(
                applicationProperties.contextBroker().entityId(),
                params,
                applicationProperties.contextBroker().context()
        ).mapLeft {
            ContextBrokerException(it.message)
        }
    }

    fun prepareAttributesAppendPayload(
        aqdvEntity: NgsildEntity,
        scalarTimeSeries: List<ScalarTimeSerie>
    ): Map<String, List<NgsiLdAttribute>> =
        scalarTimeSeries.filter {
            !aqdvEntity.hasAttribute(it.name.aqdvNameToNgsiLdProperty(), "urn:ngsi-ld:Dataset:${it.id}".toUri())
        }.map {
            NgsiLdPropertyBuilder(it.name.aqdvNameToNgsiLdProperty())
                .withValue(0.0)
                .withObservedAt(ZonedDateTime.parse("1970-01-01T00:00:00Z"))
                .withUnitCode(it.unit)
                .withDatasetId("urn:ngsi-ld:Dataset:${it.id}".toUri())
                .withSubProperty("mnemonic", it.mnemonic)
                .build()
        }.groupBy {
            // it has only one key: the attribute name
            it.keys.first()
        }

    fun addTimeSeriesToGenericAqdvEntity(attributes: Map<String, List<NgsiLdAttribute>>): Either<ApplicationException, String> {
        return entityService.appendAttributes(
            applicationProperties.contextBroker().entityId(),
            serializeObject(attributes),
            applicationProperties.contextBroker().context()
        ).mapLeft {
            ContextBrokerException(it.message)
        }
    }
}
