package io.egm.aqdv.service

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.egm.aqdv.config.ApplicationProperties
import io.egm.aqdv.model.ScalarTimeSerie
import io.quarkus.test.junit.QuarkusTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
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
    fun `it should create a scalar timeserie entity`() {
        val uuid = UUID.randomUUID()
        val scalarTimeSerie = ScalarTimeSerie(uuid, "volume", "mnemonic volume", "L", null)

        stubFor(
            post(urlPathMatching("/ngsi-ld/v1/entities"))
                .willReturn(
                    created().withHeader("Location", "/ngsi-ld/v1/entities/urn:ngsi-ld:AqdvTimeSerie:$uuid")
                )
        )

        runBlocking {
            contextBrokerService.createScalarTimeSerieEntity(scalarTimeSerie)
                .fold({
                    fail("Should have not thrown an exception (reason: $it)!")
                }, {
                    it == "/ngsi-ld/v1/entities/urn:ngsi-ld:AqdvTimeSerie:$uuid"
                })
        }
    }
}
