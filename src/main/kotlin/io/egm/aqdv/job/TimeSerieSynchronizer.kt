package io.egm.aqdv.job

import arrow.core.computations.either
import io.egm.aqdv.model.ApplicationException
import io.egm.aqdv.service.AqdvService
import io.egm.aqdv.service.ContextBrokerService
import io.egm.aqdv.utils.aqdvNameToNgsiLdProperty
import io.egm.kngsild.utils.getAttribute
import io.egm.kngsild.utils.toDefaultDatasetId
import io.quarkus.scheduler.Scheduled
import org.jboss.logging.Logger
import java.time.ZonedDateTime
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
class TimeSerieSynchronizer(
    @Inject private val aqdvService: AqdvService,
    @Inject private val contextBrokerService: ContextBrokerService
) {

    private val logger = Logger.getLogger(javaClass)

    /**
     * Synchronize the time series from AQDV to the context broker:
     *   - Retrieves the list of time series from AQDV
     *   - If a time serie is not yet in the target entity, prepares an attribute append
     *   - If a time serie is already in the target entity, do nothing
     *   - When it has inspected all the time series, sends the operation to the CB
     */
    @Scheduled(cron = "{application.aqdv.cron.time-series}")
    fun synchronizeTimeSeriesReferential(): Unit =
        either.eager<ApplicationException, String> {
            val scalarTimeSeries = aqdvService.retrieveTimeSeries().bind()
            val aqdvEntity = contextBrokerService.retrieveGenericAqdvEntity().bind()

            val ngsiLdAttributes = contextBrokerService.prepareAttributesAppendPayload(aqdvEntity, scalarTimeSeries)
            contextBrokerService.addTimeSeriesToGenericAqdvEntity(ngsiLdAttributes).bind()

        }.fold({
            logger.error("Error while synchronizing time series referential: $it")
        }, {
            logger.debug("Successfully synchronized with result : $it")
        })

    /**
     * Synchronize the time series data from AQDV to the context broker:
     *   - Retrieves the list of time series from DB
     *   - If the last synchronized date is null or before the last sample time,
     *      call AQDV to get the last data for this time series
     *   - Send all the time series data to the CB (5.6.12)
     *   - Send the last value of each time series to the CB (partial attibute update)
     */
    @Scheduled(cron = "{application.aqdv.cron.time-series-data}")
    fun synchronizeTimeSeriesData(): Unit =
        either.eager<ApplicationException, List<String>> {
            val aqdvEntity = contextBrokerService.retrieveGenericAqdvEntity().bind()
            val scalarTimeSeries = aqdvService.retrieveTimeSeries().bind()

            // for each time serie, check if last sample time is after the observed at value in the CB
            // if this is the case:
            //  - get the values from observed at and add them in the temporal history
            //  - update the last value and observed at in the CB
            val timeSeriesToUpdate = scalarTimeSeries.filter {
                it.lastSampleTime != null
            }.map {
                val ngsiLdAttribute =
                    aqdvEntity.getAttribute(it.name.aqdvNameToNgsiLdProperty(), it.id.toDefaultDatasetId())
                val lastSampleSync = ZonedDateTime.parse(ngsiLdAttribute?.get("observedAt") as String)
                Pair(it, lastSampleSync)
            }.filter {
                it.second.isBefore(it.first.lastSampleTime)
            }
            timeSeriesToUpdate.map {
                val timeSerieData =
                    aqdvService.retrieveTimeSerieData(it.first.id, it.second, it.first.lastSampleTime!!).bind()
                contextBrokerService.addTimeSeriesDataToHistory(it.first, timeSerieData).bind()
                contextBrokerService.updateTimeSerieDataLastValue(it.first, timeSerieData).bind()
            }
        }.fold({
            logger.error("Error while synchronizing time series data: $it")
        }, {
            logger.debug("Successfully synchronized time series data with result : $it")
        })
}
