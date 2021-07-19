package io.egm.aqdv.api

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.`is`
import org.junit.jupiter.api.Test

@QuarkusTest
class AqdvResourceTest {

    @Test
    fun testScalarTimeSeriesEndpoint() {
        given()
          .`when`().get("/aqdv")
          .then()
             .statusCode(200)
    }

}
