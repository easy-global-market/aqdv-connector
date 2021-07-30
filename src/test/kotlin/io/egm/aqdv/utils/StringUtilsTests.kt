package io.egm.aqdv.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class StringUtilsTests {

    @ParameterizedTest(name = "the NGSI-LD property name should be compliant for {0}")
    @MethodSource("getNames")
    fun `it should transform an AQDV name to an NGSI-LD property`(aqdvName: String, expected: String) {
        assertEquals(expected, aqdvName.aqdvNameToNgsiLdProperty())
    }

    companion object {

        @JvmStatic
        fun getNames(): List<Arguments> =
            listOf(
                Arguments.of(
                    "Volume nominal réservoir - Seuil haut",
                    "volumeNominalRéservoirSeuilHaut"
                ),
                Arguments.of(
                    "Référence de la série de données points d'entrée disponibles",
                    "référenceDeLaSérieDeDonnéesPointsDentréeDisponibles"
                ),
                Arguments.of(
                    "Volume horaire de consommation de référence",
                    "volumeHoraireDeConsommationDeRéférence"
                )
            )
    }
}
