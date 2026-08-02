package com.truckerload.domain.import.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvLoadParserTest {

    @Test
    fun splitCsvLine_respectsQuotedCommas() {
        val cols = CsvLoadParser.splitCsvLine(
            """T-1,2500,"SWF2, Garner, NC","TOL3, Perrysburg, OH",850""",
        )
        assertEquals(listOf("T-1", "2500", "SWF2, Garner, NC", "TOL3, Perrysburg, OH", "850"), cols)
    }

    @Test
    fun parse_facilityCityStateAddress() {
        val csv = """
            tripId,rate,miles,origin,destination
            T-116KYL6KW,2500.00,850,"SWF2, Garner, NC","TOL3, Perrysburg, OH"
        """.trimIndent()

        val loads = CsvLoadParser().parse(csv)
        assertEquals(1, loads.size)
        val load = loads.single()
        assertEquals("Garner", load.stops.first().city)
        assertEquals("NC", load.stops.first().state)
        assertEquals("SWF2", load.stops.first().facilityCode)
        assertEquals("Perrysburg", load.stops.last().city)
        assertEquals("OH", load.stops.last().state)
    }

    @Test
    fun parse_sanitizesAbsurdMilesTypo() {
        val csv = """
            tripId,rate,miles,origin,destination
            T-TYPO1,1827.81,182781,Austin TX,Dallas TX
        """.trimIndent()
        val loads = CsvLoadParser().parse(csv)
        assertEquals(1, loads.size)
        assertTrue(loads.single().totalMiles < 10_000.0)
        assertEquals(1827.81, loads.single().totalMiles, 0.01)
    }
}
