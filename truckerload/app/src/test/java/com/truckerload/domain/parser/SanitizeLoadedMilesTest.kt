package com.truckerload.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SanitizeLoadedMilesTest {

    @Test
    fun sanitizeLoadedMiles_fixesMissingDecimalTypoFromRelayExport() {
        // Real Telegram export typo: "Total Loaded Miles: 182781 mi" for a ~$4141 Miami multi-leg.
        val fixed = ParseUtils.sanitizeLoadedMiles(miles = 182_781.0, totalRate = 4_141.11)
        assertEquals(1_827.81, fixed, 0.01)
    }

    @Test
    fun sanitizeLoadedMiles_keepsNormalRelayMiles() {
        assertEquals(850.0, ParseUtils.sanitizeLoadedMiles(850.0, 2_500.0), 0.01)
        assertEquals(1_198.03, ParseUtils.sanitizeLoadedMiles(1_198.03, 2_945.56), 0.01)
        assertEquals(3_084.05, ParseUtils.sanitizeLoadedMiles(3_084.05, 7_337.47), 0.01)
    }

    @Test
    fun sanitizeLoadedMiles_doesNotRewriteCheapButPlausibleLongHaul() {
        // $0.40/mi over 2_000 mi is low but not an absurd 10k+ typo.
        assertEquals(2_000.0, ParseUtils.sanitizeLoadedMiles(2_000.0, 800.0), 0.01)
    }

    @Test
    fun loadMessageParser_appliesSanitizeOnParse() {
        val message = """
            Trip ID: T-113VLKDVS
            Total Rate: ${'$'}4141.11
            Total Loaded Miles: 182781 mi
            Pu-address: MIA7, Miami, FL
            Del-address: AUS2, Austin, TX
        """.trimIndent()

        val load = LoadMessageParser.parseOne(message)
        assertNotNull(load)
        assertEquals(1_827.81, load!!.totalMiles, 0.01)
        assertEquals(4_141.11, load.totalRate, 0.01)
    }
}
