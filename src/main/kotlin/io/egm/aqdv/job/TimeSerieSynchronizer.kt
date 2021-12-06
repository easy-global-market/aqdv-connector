package io.egm.aqdv.job

import arrow.core.computations.either
import io.egm.aqdv.config.ApplicationProperties
import io.egm.aqdv.model.ApplicationException
import io.egm.aqdv.service.AqdvService
import io.egm.aqdv.service.ContextBrokerService
import io.egm.kngsild.utils.getAttribute
import io.quarkus.scheduler.Scheduled
import kotlinx.coroutines.runBlocking
import org.jboss.logging.Logger
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
class TimeSerieSynchronizer(
    @Inject private val aqdvService: AqdvService,
    @Inject private val contextBrokerService: ContextBrokerService,
    @Inject private val applicationProperties: ApplicationProperties
) {

    private val logger = Logger.getLogger(javaClass)

    /**
     * Synchronize the time series data from AQDV to the context broker:
     *   - Retrieves the list of known time series from AQDV
     *   - If the last synchronized date is null or before the last sample time,
     *      call AQDV to get the last data for this time series
     *   - Send all the time series data to the CB (5.6.12)
     *   - Send the last value of each time series to the CB (partial attibute update)
     */
    @Scheduled(cron = "{application.aqdv.cron.time-series-data}")
    fun synchronizeTimeSeriesData(): Unit =
         runBlocking {
            either<ApplicationException, List<String>> {
                val scalarTimeSeries = aqdvService.retrieveKnownTimeSeries().bind()

                // for each time serie, check if last sample time is after the observed at value in the CB
                // if this is the case:
                //  - get the values from observed at and add them in the temporal history
                //  - update the last value and observed at in the CB
                val timeSeriesToUpdate = scalarTimeSeries.filter {
                    it.second.lastSampleTime != null
                }.map {
                    val scalarTimeSerieEntity = contextBrokerService.retrieveEntity(it.second.ngsiLdEntityId()).bind()
                    val ngsiLdAttribute =
                        scalarTimeSerieEntity.getAttribute(applicationProperties.aqdv().targetProperty())
                    val lastSampleSync = ZonedDateTime.parse(ngsiLdAttribute?.get("observedAt") as String)
                    logger.debug("Entity ${it.second.ngsiLdEntityId()} has last observation date at $lastSampleSync")
                    Triple(it.first, it.second, lastSampleSync)
                }
                timeSeriesToUpdate.map { (knownTimeserie, scalarTimeserie, lastObservedDate) ->
                    val computedStartDate =
                        minOf(Instant.now().atZone(ZoneOffset.UTC), lastObservedDate)
                        .let {
                            if (knownTimeserie.mutablePeriodMinutes() != 0)
                                it.minusMinutes(knownTimeserie.mutablePeriodMinutes().toLong())
                            else
                                it
                        }.withNano(0)

                    val entityId = scalarTimeserie.ngsiLdEntityId()
                    val timeSerieData =
                        aqdvService.retrieveTimeSerieData(scalarTimeserie.id, computedStartDate, scalarTimeserie.lastSampleTime!!).bind()
                    contextBrokerService.addTimeSeriesDataToHistory(entityId, scalarTimeserie, timeSerieData).bind()
                    contextBrokerService.updateTimeSerieDataLastValue(entityId, scalarTimeserie, timeSerieData).bind()
                }
            }.fold({
                logger.error("Error while synchronizing time series data: $it")
            }, {
                logger.info("Successfully synchronized time series data!")
            })
        }
}
