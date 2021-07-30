package io.egm.aqdv.job

import arrow.core.Either
import arrow.core.computations.either
import io.egm.aqdv.model.ApplicationException
import io.egm.aqdv.service.AqdvService
import io.egm.aqdv.service.ContextBrokerService
import io.quarkus.scheduler.Scheduled
import org.jboss.logging.Logger
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

            contextBrokerService.prepareAttributesAppendPayload(aqdvEntity, scalarTimeSeries).let {
                contextBrokerService.addTimeSeriesToGenericAqdvEntity(it).bind()
            }

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
    fun synchronizeTimeSeriesData(): Unit = TODO()
}
