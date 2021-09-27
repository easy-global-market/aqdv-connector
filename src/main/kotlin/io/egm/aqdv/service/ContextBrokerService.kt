package io.egm.aqdv.service

import arrow.core.Either
import arrow.core.computations.either
import arrow.core.left
import arrow.core.right
import io.egm.aqdv.config.ApplicationProperties
import io.egm.aqdv.model.AQDV_TS_TYPE
import io.egm.aqdv.model.ApplicationException
import io.egm.aqdv.model.ApplicationException.AqdvException
import io.egm.aqdv.model.ApplicationException.ContextBrokerException
import io.egm.aqdv.model.ScalarTimeSerie
import io.egm.aqdv.model.ScalarTimeSerieData
import io.egm.kngsild.api.EntityService
import io.egm.kngsild.api.TemporalService
import io.egm.kngsild.utils.*
import io.egm.kngsild.utils.JsonUtils.serializeObject
import io.egm.kngsild.utils.UriUtils.toUri
import org.jboss.logging.Logger
import java.net.URI
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

    suspend fun getNonExistingEntities(): Either<ApplicationException, List<URI>> {
        val idsOfExpectedEntities = applicationProperties.aqdv().knownTimeseries()
                .mapNotNull {
                    "urn:ngsi-ld:$AQDV_TS_TYPE:$it".toUri()
                }
        logger.info("Searching non existing entites among: $idsOfExpectedEntities")
        return either {
            val existingEntities = entityService.query(
                    mapOf(
                        "type" to AQDV_TS_TYPE,
                        "ids" to idsOfExpectedEntities.joinToString(",")
                    ),
                    applicationProperties.contextBroker().context()
            ).mapLeft {
                ContextBrokerException(it.message)
            }.bind()
            val existingsEntitiesIds = existingEntities.mapNotNull { (it["id"]!! as String).toUri() }
            idsOfExpectedEntities.minus(existingsEntitiesIds)
        }
    }

    suspend fun createScalarTimeSerieEntity(
        scalarTimeSerie: ScalarTimeSerie
    ): Either<ApplicationException, ResourceLocation> {
        val ngsiLdEntity = NgsiLdEntityBuilder(
            scalarTimeSerie.ngsiLdEntityId(),
            AQDV_TS_TYPE,
            listOf(applicationProperties.contextBroker().context())
        ).addAttribute(
            NgsiLdPropertyBuilder(applicationProperties.aqdv().targetProperty())
                .withValue(0)
                .withObservedAt(ZonedDateTime.parse("1970-01-01T00:00:00Z"))
                .withUnitCode(scalarTimeSerie.unit)
                .withSubProperty("name", scalarTimeSerie.name)
                .withSubProperty("mnemonic", scalarTimeSerie.mnemonic)
                .build()
        ).build()
        return entityService.create(serializeObject(ngsiLdEntity))
            .mapLeft {
                ContextBrokerException(it.message)
            }
    }

    suspend fun retrieveEntity(entityId: URI, keyValues: Boolean = false): Either<ApplicationException, NgsildEntity> {
        val params = if (keyValues)
            mapOf("options" to "keyValues")
        else
            emptyMap()

        return entityService.retrieve(
            entityId,
            params,
            applicationProperties.contextBroker().context()
        ).mapLeft {
            ContextBrokerException(it.message)
        }
    }

    fun addTimeSeriesDataToHistory(
        entityId: URI,
        scalarTimeSerie: ScalarTimeSerie,
        scalarTimeSeriesData: List<ScalarTimeSerieData>
    ): Either<ApplicationException, String> {
        // prepare a chunked list of temporal attribute instances to avoid too large payloads
        val chunkedNgsiLdTemporalAttributesInstances: List<NgsiLdTemporalAttributesInstances> =
            scalarTimeSeriesData.map { scalarTimeSerieData ->
                NgsiLdPropertyBuilder(applicationProperties.aqdv().targetProperty())
                    .withValue(scalarTimeSerieData.value)
                    .withObservedAt(scalarTimeSerieData.time)
                    .withUnitCode(scalarTimeSerie.unit)
                    .withSubProperty("name", scalarTimeSerie.name)
                    .withSubProperty("mnemonic", scalarTimeSerie.mnemonic)
                    .build()
            }.chunked(100).map { it.groupByProperty() }

        // send each chunk of temporal attribute instances
        val addInstancesResult = chunkedNgsiLdTemporalAttributesInstances.map { ngsiLdTemporalAttributesInstances ->
            temporalService.addAttributes(
                entityId,
                ngsiLdTemporalAttributesInstances,
                applicationProperties.contextBroker().context()
            ).mapLeft { applicationError ->
                ContextBrokerException(applicationError.message)
            }
        }

        // this is actually quite dirty
        // only considering the presence of one error among all the results
        return if (addInstancesResult.any { it.isLeft() })
            addInstancesResult.find { it.isLeft() }!!.mapLeft { it }
        else "".right()
    }

    fun updateTimeSerieDataLastValue(
        entityId: URI,
        scalarTimeSerie: ScalarTimeSerie,
        scalarTimeSeriesData: List<ScalarTimeSerieData>
    ): Either<ApplicationException, String> {
        val lastSample = scalarTimeSeriesData.maxByOrNull {
            it.time
        } ?: return AqdvException("No last element found in $scalarTimeSeriesData?!").left()
        val ngsiLdAttribute = NgsiLdPropertyBuilder(applicationProperties.aqdv().targetProperty())
            .withValue(lastSample.value)
            .withObservedAt(lastSample.time)
            .build()
        return entityService.partialAttributeUpdate(
            entityId,
            applicationProperties.aqdv().targetProperty(),
            ngsiLdAttribute.propertyValue,
            applicationProperties.contextBroker().context()
        ).mapLeft {
            ContextBrokerException(it.message)
        }
    }
}
