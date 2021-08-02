package io.egm.aqdv.model

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.ResponseDeserializable
import java.time.ZonedDateTime
import java.util.*

data class ScalarTimeSerie(
    val id: UUID,
    val name: String,
    val mnemonic: String?,
    val unit: String?,
    val lastSampleTime: ZonedDateTime?
)

data class ScalarTimeSerieData(
    val time: ZonedDateTime,
    val value: Double
)

object ScalarTimeSerieDeserializer : ResponseDeserializable<List<ScalarTimeSerie>> {
    override fun deserialize(content: String): List<ScalarTimeSerie> =
        jacksonObjectMapper.readValue(content)
}

object ScalarTimeSerieDataDeserializer : ResponseDeserializable<List<ScalarTimeSerieData>> {
    override fun deserialize(content: String): List<ScalarTimeSerieData> =
        jacksonObjectMapper.readValue(content)
}

private val jacksonObjectMapper = jacksonObjectMapper().apply {
    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    registerModule(JavaTimeModule())
}
