package io.egm.aqdv.service

import arrow.core.Either
import arrow.core.computations.either
import io.egm.aqdv.model.ApplicationException
import io.egm.kngsild.utils.ResourceLocation
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
class ScalaTimeSeriesInitializer(
    @Inject private val contextBrokerService: ContextBrokerService,
    @Inject private val aqdvService: AqdvService
) {

    suspend fun initializeScalarTimeSeries(): Either<ApplicationException, List<ResourceLocation>> =
        either {
            val nonExistingEntitiesIds = contextBrokerService.getNonExistingEntities().bind()
            val timeSeriesToCreate = nonExistingEntitiesIds.map {
                aqdvService.retrieveTimeSerie(UUID.fromString(it.toString().substringAfterLast(":"))).bind()
            }
            timeSeriesToCreate.map {
                contextBrokerService.createScalarTimeSerieEntity(it).bind()
            }
        }
}
