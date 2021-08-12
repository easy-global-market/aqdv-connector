package io.egm.aqdv.service

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.time.ZoneOffset
import java.time.ZonedDateTime
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
            get(urlMatching("/aqdv-to-fiware/scalartimeseries/"))
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

    @Test
    fun `it should retrieve a list of scalar time series with null values`() {
        stubFor(
            get(urlMatching("/aqdv-to-fiware/scalartimeseries/"))
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
                                    "unit": null,
                                    "lastSampleTime": null
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
            assertNull(scalarTimeSerie.unit)
            assertNull(scalarTimeSerie.lastSampleTime)
        })
    }

    @Test
    fun `it should retrieve data from a scalar time serie`() {
        stubFor(
            get(urlPathMatching("/aqdv-to-fiware/scalartimeseries/bee5dc6a-973d-46df-967b-bc8ed6186e45/data"))
                .willReturn(
                    okJson(
                        """
                            [
                                {
                                    "time": "2021-07-01T00:00:00Z",
                                    "value": 4824.0
                                },
                                {
                                    "time": "2021-07-01T00:10:00Z",
                                    "value": 4807.0
                                }
                            ]
                        """.trimIndent()
                    )
                )
        )

        aqdvService.retrieveTimeSerieData(
            UUID.fromString("bee5dc6a-973d-46df-967b-bc8ed6186e45"),
            ZonedDateTime.now(),
            ZonedDateTime.now()
        ).fold({
            fail { "it should have returned a success result but got $it" }
        }, {
            assertEquals(2, it.size)
            assertTrue(
                it.all { scalarTimeserieData ->
                    listOf(4824.0, 4807.0).contains(scalarTimeserieData.value) &&
                            scalarTimeserieData.time.isAfter(ZonedDateTime.of(2021, 6, 30, 0, 0, 0, 0, ZoneOffset.UTC))
                }
            )
        })
    }
}
