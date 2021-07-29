package io.egm.aqdv.service

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.egm.aqdv.config.ApplicationProperties
import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
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
            fail("Expected a success result but got ${it.message}")
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
            fail("Expected a success result but got ${it.message}")
        }, {
            assertEquals("/ngsi-ld/v1/entities/$aqdvEntityId", it)
        })

        assertTrue(findUnmatchedRequests().isEmpty())
    }
}
