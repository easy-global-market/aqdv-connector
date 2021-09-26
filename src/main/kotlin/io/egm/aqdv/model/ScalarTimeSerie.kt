package io.egm.aqdv.model

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.ResponseDeserializable
import io.egm.kngsild.utils.UriUtils.toUri
import java.net.URI
import java.time.ZonedDateTime
import java.util.*

data class ScalarTimeSerie(
    val id: UUID,
    val name: String,
    val mnemonic: String?,
    val unit: String?,
    val lastSampleTime: ZonedDateTime?
) {
    fun ngsiLdEntityId(): URI = "urn:ngsi-ld:AqdvTimeSerie:$id".toUri()!!
}

data class ScalarTimeSerieData(
    val time: ZonedDateTime,
    val value: Double
)

// it seems there is some unexpected unicode character hidden in the received payloads, remove it
// see https://salesforce.stackexchange.com/a/187321
private fun String.removeHiddenOffendingCharacter() =
    this.trim().replace("\uFEFF", "")

object ScalarTimeSerieDeserializer : ResponseDeserializable<ScalarTimeSerie> {
    override fun deserialize(content: String): ScalarTimeSerie =
        jacksonObjectMapper.readValue(content.removeHiddenOffendingCharacter())
}

object ScalarTimeSerieDataDeserializer : ResponseDeserializable<List<ScalarTimeSerieData>> {
    override fun deserialize(content: String): List<ScalarTimeSerieData> =
        // it seems there is some unexpected unicode character hidden in the received payloads, remove it
        // see https://salesforce.stackexchange.com/a/187321
        jacksonObjectMapper.readValue(content.removeHiddenOffendingCharacter())
}

private val jacksonObjectMapper = jacksonObjectMapper().apply {
    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    registerModule(JavaTimeModule())
}
