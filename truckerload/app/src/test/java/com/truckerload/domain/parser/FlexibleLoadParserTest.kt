package com.truckerload.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlexibleLoadParserTest {

    @Test
    fun parsesPastedMessageWithoutTripId() {
        val text = """
            Total Rate: 2500.00
            Total Loaded Miles: 850 mi
            Pu-address: SWF2, Garner, NC
            Del-address: Dallas, TX
        """.trimIndent()

        val load = FlexibleLoadParser.parseOne(text, nowMillis = 1_776_441_600_000L)
        assertNotNull(load)
        assertEquals(2500.0, load!!.totalRate, 0.01)
        assertEquals(850.0, load.totalMiles, 0.01)
        assertTrue(load.pointA.contains("Garner") || load.rawMessage.contains("Garner"))
        assertTrue(load.tripId.isNotBlank())
    }

    @Test
    fun parsesLooseLabeledText() {
        val text = """
            Rate: $1800
            Miles: 620
            From: Houston, TX
            To: Atlanta, GA
        """.trimIndent()

        val load = FlexibleLoadParser.parseOne(text, nowMillis = 1_776_441_600_000L)
        assertNotNull(load)
        assertEquals(1800.0, load!!.totalRate, 0.01)
        assertEquals(620.0, load.totalMiles, 0.01)
        assertTrue(load.pointA.contains("Houston"))
        assertTrue(load.pointB.contains("Atlanta"))
    }

    @Test
    fun rejectsPaycheckText() {
        val text = """
            Driver Settlement
            Grand Total: $4500
            Net Pay: $3900
        """.trimIndent()
        assertNull(FlexibleLoadParser.parseOne(text))
    }

    @Test
    fun rejectsTextWithoutAddress() {
        assertNull(FlexibleLoadParser.parseOne("Total Rate: 1000"))
    }
}
