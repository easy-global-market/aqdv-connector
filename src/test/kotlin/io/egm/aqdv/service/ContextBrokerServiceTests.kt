package io.egm.aqdv.service

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.egm.aqdv.config.ApplicationProperties
import io.egm.aqdv.model.ScalarTimeSerie
import io.egm.kngsild.utils.NgsiLdAttributeNG
import io.egm.kngsild.utils.NgsiLdEntityBuilder
import io.egm.kngsild.utils.UriUtils.toUri
import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.util.*
import javax.inject.Inject

@QuarkusTest
class ContextBrokerServiceTests {

    @Inject
    private lateinit var contextBrokerService: ContextBrokerService

    @Inject
    private lateinit var applicationProperties: ApplicationProperties

    private lateinit var wireMockServer: WireMockServer

    @BeforeAll
    fun beforeAll() {
        wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().port(8088))
        wireMockServer.start()
        // If not using the default port, we need to instruct explicitly the client (quite redundant)
        configureFor(8088)

        stubFor(
            post("/auth/realms/stellio-dev/protocol/openid-connect/token")
                .willReturn(
                    okJson(
                        """
                            {
                                "access_token": "access_token"
                            }
                        """.trimIndent()))
        )
    }

    @AfterAll
    fun afterAll() {
        wireMockServer.stop()
    }

    @Test
    fun `it should not try to create the generic Aqdv entity if it already exists`() {
        val aqdvEntityId = applicationProperties.contextBroker().entityId()
        stubFor(
            get(urlPathMatching("/ngsi-ld/v1/entities/$aqdvEntityId"))
                .willReturn(
                    okJson(
                        """
                        {
                            "id": "$aqdvEntityId",
                            "type": "AqdvEntity"
                        }
                        """.trimIndent()
                    )
                )
        )

        val createResult = contextBrokerService.createGenericAqdvEntity()

        createResult.fold({
            fail("Expected a success result but got $it")
        }, {
            assertEquals("/ngsi-ld/v1/entities/$aqdvEntityId", it)
        })

        // to check it has not tried to create another Aqdv entity
        assertTrue(findUnmatchedRequests().isEmpty())
    }

    @Test
    fun `it should create the generic Aqdv entity if it does not already exist`() {
        val aqdvEntityId = applicationProperties.contextBroker().entityId()
        stubFor(
            get(urlPathMatching("/ngsi-ld/v1/entities/$aqdvEntityId"))
                .willReturn(notFound())
        )
        stubFor(
            post(urlPathMatching("/ngsi-ld/v1/entities"))
                .willReturn(
                    status(201)
                        .withHeader("Location", "/ngsi-ld/v1/entities/$aqdvEntityId")
                )
        )

        val createResult = contextBrokerService.createGenericAqdvEntity()

        createResult.fold({
            fail("Expected a success result but got $it")
        }, {
            assertEquals("/ngsi-ld/v1/entities/$aqdvEntityId", it)
        })

        assertTrue(findUnmatchedRequests().isEmpty())
    }

    @Test
    fun `it should prepare the payload to append attributes`() {
        val ngsiLdEntity = NgsiLdEntityBuilder("urn:ngsi-ld:Entity:123".toUri()!!, "Entity").build()
        val scalarTimeSeries = listOf(
            ScalarTimeSerie(UUID.randomUUID(), "volume", "mnemonic volume", "L", null),
            ScalarTimeSerie(UUID.randomUUID(), "consommation", null, "L", null)
        )

        val appendPayload = contextBrokerService.prepareAttributesAppendPayload(ngsiLdEntity, scalarTimeSeries)

        assertEquals(2, appendPayload.size)
        assertEquals(listOf("volume", "consommation"), appendPayload.map { it.propertyName })
        val volumeAttribute = appendPayload.find { it.propertyName == "volume" }
        assertNotNull(volumeAttribute)
        assertTrue(volumeAttribute!!.propertyValue.containsKey("mnemonic"))
    }

    @Test
    fun `it should ask to append one property to the generic AQDV entity`() {
        val aqdvEntityId = applicationProperties.contextBroker().entityId()
        stubFor(
            post(urlPathMatching("/ngsi-ld/v1/entities/$aqdvEntityId/attrs"))
                .willReturn(status(204))
        )

        val ngsiLdAttribute = listOf(
            NgsiLdAttributeNG(
                "volume",
                mapOf(
                    "type" to "Property",
                    "value" to 2.0
                )
            )
        )

        val appendResult = contextBrokerService.addTimeSeriesToGenericAqdvEntity(ngsiLdAttribute)

        assertTrue(appendResult.isRight())
    }
}
