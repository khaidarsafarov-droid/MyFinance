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

    @Test
    fun shortToFromLabelsNeedWordBoundaries() {
        // "To" inside "Total Rate" / "From" inside "Platform" must not steal addresses
        val text = """
            Total Rate: 1800
            Miles: 620
            Platform: SWF2 dock
            Auto: night run
            Houston, TX
            Atlanta, GA
        """.trimIndent()

        val load = FlexibleLoadParser.parseOne(text, nowMillis = 1_776_441_600_000L)
        assertNotNull(load)
        assertTrue(load!!.pointA.contains("Houston"))
        assertTrue(load.pointB.contains("Atlanta"))
        assertTrue(!load.pointA.contains("SWF2 dock") && !load.pointB.contains("SWF2 dock"))
    }

    @Test
    fun extractFieldsFillsFormEvenWhenAddressMissing() {
        val draft = FlexibleLoadParser.extractFields(
            """
            Trip ID: T-116KYL6KW
            Total Rate: 2500.00
            Total Loaded Miles: 850 mi
            """.trimIndent(),
        )
        assertEquals("T-116KYL6KW", draft.tripId)
        assertEquals("2500", draft.rate)
        assertEquals("850", draft.miles)
        assertEquals("", draft.pointA)
        assertEquals("", draft.pointB)
        assertNull(FlexibleLoadParser.parseOne("Total Rate: 2500.00"))
    }

    @Test
    fun extractFieldsFromOcrLabelOnNextLine() {
        val text = """
            Trip ID
            T-116KYL6KW
            Total Rate
            $2,500.00
            Total Loaded Miles
            850 mi
            Pickup
            SWF2, Garner, NC
            Delivery
            Dallas, TX
        """.trimIndent()

        val draft = FlexibleLoadParser.extractFields(text)
        assertEquals("T-116KYL6KW", draft.tripId)
        assertEquals("2500", draft.rate)
        assertEquals("850", draft.miles)
        assertTrue(draft.pointA.contains("Garner"))
        assertTrue(draft.pointB.contains("Dallas"))
    }

    @Test
    fun extractFieldsFromBrokerRateConfirmation() {
        val text = """
            Load #: RC-99881
            Line Haul: $1800
            Miles: 620
            Origin: Houston, TX
            Destination: Atlanta, GA
            Date: 08/18/2026
        """.trimIndent()

        val draft = FlexibleLoadParser.extractFields(text)
        assertEquals("RC-99881", draft.tripId)
        assertEquals("1800", draft.rate)
        assertEquals("620", draft.miles)
        assertTrue(draft.pointA.contains("Houston"))
        assertTrue(draft.pointB.contains("Atlanta"))
        assertEquals("2026-08-18", draft.date)
    }

    @Test
    fun extractFieldsLeavesUnreadableOcrEmptyForManualFill() {
        val draft = FlexibleLoadParser.extractFields(
            "available and nen: vehicle. Property described above is receved in good order, except as noted",
        )
        assertEquals("", draft.tripId)
        assertEquals("", draft.rate)
        assertEquals("", draft.miles)
        assertEquals("", draft.pointA)
        assertEquals("", draft.pointB)
    }

    @Test
    fun joinLabelValueLinesMergesOcrStackedLabels() {
        val joined = FlexibleLoadParser.joinLabelValueLines(
            """
            Total Rate
            $2500.00
            Pickup
            Garner, NC
            """.trimIndent(),
        )
        assertTrue(joined.contains("Total Rate: $2500.00"))
        assertTrue(joined.contains("Pickup: Garner, NC"))
    }
}
