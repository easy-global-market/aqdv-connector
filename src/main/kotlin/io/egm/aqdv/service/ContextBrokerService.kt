package io.egm.aqdv.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.egm.aqdv.config.ApplicationProperties
import io.egm.aqdv.model.ApplicationException
import io.egm.aqdv.model.ApplicationException.AqdvException
import io.egm.aqdv.model.ApplicationException.ContextBrokerException
import io.egm.aqdv.model.ScalarTimeSerie
import io.egm.aqdv.model.ScalarTimeSerieData
import io.egm.aqdv.utils.aqdvNameToNgsiLdProperty
import io.egm.kngsild.api.EntityService
import io.egm.kngsild.api.TemporalService
import io.egm.kngsild.model.ResourceNotFound
import io.egm.kngsild.utils.*
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
    private val temporalService = TemporalService(applicationProperties.contextBroker().url(), authUtils)

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
    ): List<NgsiLdAttributeNG> =
        scalarTimeSeries.filter {
            !aqdvEntity.hasAttribute(it.name.aqdvNameToNgsiLdProperty(), it.id.toDefaultDatasetId())
        }.map { scalarTimeSerie ->
            NgsiLdPropertyBuilder(scalarTimeSerie.name.aqdvNameToNgsiLdProperty())
                .withValue(0.0)
                .withObservedAt(ZonedDateTime.parse("1970-01-01T00:00:00Z"))
                .withUnitCode(scalarTimeSerie.unit)
                .withDatasetId(scalarTimeSerie.id.toDefaultDatasetId())
                .let {
                    if (scalarTimeSerie.mnemonic != null && scalarTimeSerie.mnemonic != "")
                        it.withSubProperty("mnemonic", scalarTimeSerie.mnemonic)
                    else
                        it
                }
                .build()
        }

    fun addTimeSeriesToGenericAqdvEntity(attributes: List<NgsiLdAttributeNG>): Either<ApplicationException, String> {
        return entityService.appendAttributes(
            applicationProperties.contextBroker().entityId(),
            attributes,
            applicationProperties.contextBroker().context()
        ).mapLeft {
            ContextBrokerException(it.message)
        }
    }

    fun addTimeSeriesDataToHistory(
        scalarTimeSerie: ScalarTimeSerie,
        scalarTimeSeriesData: List<ScalarTimeSerieData>
    ): Either<ApplicationException, String> {
        val ngsiLdTemporalAttributesInstances = scalarTimeSeriesData.map { scalarTimeSerieData ->
            NgsiLdPropertyBuilder(scalarTimeSerie.name.aqdvNameToNgsiLdProperty())
                .withValue(scalarTimeSerieData.value)
                .withObservedAt(scalarTimeSerieData.time)
                .withUnitCode(scalarTimeSerie.unit)
                .withDatasetId(scalarTimeSerie.id.toDefaultDatasetId())
                .let {
                    if (scalarTimeSerie.mnemonic != null)
                        it.withSubProperty("mnemonic", scalarTimeSerie.mnemonic)
                    else
                        it
                }
                .build()
        }.groupByProperty()

        return temporalService.addAttributes(
            applicationProperties.contextBroker().entityId(),
            ngsiLdTemporalAttributesInstances,
            applicationProperties.contextBroker().context()
        ).mapLeft {
            ContextBrokerException(it.message)
        }
    }

    fun updateTimeSerieDataLastValue(
        scalarTimeSerie: ScalarTimeSerie,
        scalarTimeSeriesData: List<ScalarTimeSerieData>
    ): Either<ApplicationException, String> {
        val lastSample = scalarTimeSeriesData.maxByOrNull {
            it.time
        } ?: return AqdvException("No last element found in $scalarTimeSeriesData?!").left()
        val ngsiLdAttribute = NgsiLdPropertyBuilder(scalarTimeSerie.name.aqdvNameToNgsiLdProperty())
            .withValue(lastSample.value)
            .withObservedAt(lastSample.time)
            .withDatasetId(scalarTimeSerie.id.toDefaultDatasetId())
            .build()
        return entityService.partialAttributeUpdate(
            applicationProperties.contextBroker().entityId(),
            scalarTimeSerie.name.aqdvNameToNgsiLdProperty(),
            ngsiLdAttribute.propertyValue,
            applicationProperties.contextBroker().context()
        ).mapLeft {
            ContextBrokerException(it.message)
        }
    }
}
