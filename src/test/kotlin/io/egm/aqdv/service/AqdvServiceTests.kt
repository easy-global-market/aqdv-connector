package io.egm.aqdv.service

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.*
import javax.inject.Inject

@QuarkusTest
class AqdvServiceTests {

    @Inject
    private lateinit var aqdvService: AqdvService

    private lateinit var wireMockServer: WireMockServer

    @BeforeAll
    fun beforeAll() {
        wireMockServer = WireMockServer(wireMockConfig().port(8089))
        wireMockServer.start()
        // If not using the default port, we need to instruct explicitly the client (quite redundant)
        configureFor(8089)
    }

    @AfterAll
    fun afterAll() {
        wireMockServer.stop()
    }

    @Test
    fun `it should retrieve a list of scalar time series`() {
        stubFor(
            get(urlMatching("/aqdv-to-fiware/scalartimeseries"))
                .willReturn(
                    okJson(
                        """
                            [
                                {
                                    "id": "BEE5DC6A-973D-46DF-967B-BC8ED6186E45",
                                    "name": "Consommation",
                                    "mnemonic": "Mnémonique consommation",
                                    "data": {
                                        "href": "http://localhost:8089/data"
                                    },
                                    "unit": "m3",
                                    "lastSampleTime": "2021-07-19T00:00:00Z"
                                }
                            ]
                        """.trimIndent()
                    )
                )
        )

        aqdvService.retrieveTimeSeries().fold({
            fail { "it should have returned a success result but got $it" }
        }, {
            assertEquals(1, it.size)
            val scalarTimeSerie = it[0]
            assertEquals(UUID.fromString("bee5dc6a-973d-46df-967b-bc8ed6186e45"), scalarTimeSerie.id)
            assertEquals("Consommation", scalarTimeSerie.name)
            assertEquals("Mnémonique consommation", scalarTimeSerie.mnemonic)
            assertEquals("m3", scalarTimeSerie.unit)
            assertEquals("2021-07-19T00:00Z[UTC]", scalarTimeSerie.lastSampleTime.toString())
        })
    }
}
