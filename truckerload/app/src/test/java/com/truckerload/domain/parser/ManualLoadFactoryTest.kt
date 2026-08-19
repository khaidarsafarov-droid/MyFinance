package com.truckerload.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualLoadFactoryTest {

    @Test
    fun buildsLoadFromTypedFieldsAndGeneratesTripId() {
        val load = ManualLoadFactory.build(
            tripId = "",
            date = "2026-08-17",
            rate = 2500.0,
            miles = 850.0,
            pointA = "Garner, NC",
            pointB = "Dallas, TX",
            nowMillis = 1_776_441_600_000L,
        )

        assertTrue(load.tripId.startsWith("M-"))
        assertEquals(2500.0, load.totalRate, 0.01)
        assertEquals(850.0, load.totalMiles, 0.01)
        assertEquals(1, load.puCount)
        assertEquals(1, load.delCount)
        assertTrue(load.route.contains("Garner") || load.pointA.contains("Garner"))
        assertTrue(load.route.contains("Dallas") || load.pointB.contains("Dallas"))
        assertTrue(load.weekNumber > 0)
        assertTrue(load.year >= 2026)
    }

    @Test
    fun keepsProvidedTripId() {
        val load = ManualLoadFactory.build(
            tripId = "T-MANUAL1",
            date = "2026-08-17",
            rate = 1000.0,
            miles = 400.0,
            pointA = "Austin, TX",
            pointB = "",
            nowMillis = 1_776_441_600_000L,
        )
        assertEquals("T-MANUAL1", load.tripId)
        assertEquals("T-MANUAL1", load.id)
        assertEquals(1, load.puCount)
        assertEquals(0, load.delCount)
    }

    @Test
    fun extraStopsBecomeChainAfterOrigin() {
        val load = ManualLoadFactory.build(
            tripId = "T-MULTI1",
            date = "2026-08-19",
            rate = 3200.0,
            miles = 1100.0,
            pointA = "Garner, NC",
            pointB = "Atlanta, GA",
            extraPoints = listOf("Nashville, TN", "Dallas, TX"),
            nowMillis = 1_776_441_600_000L,
        )
        assertEquals(4, load.stops.size)
        assertEquals(1, load.puCount)
        assertEquals(3, load.delCount)
        assertEquals("Garner, NC", load.stops[0].fullAddress)
        assertEquals("Atlanta, GA", load.stops[1].fullAddress)
        assertEquals("Nashville, TN", load.stops[2].fullAddress)
        assertEquals("Dallas, TX", load.stops[3].fullAddress)
        assertTrue(load.rawMessage.contains("Point C: Nashville, TN"))
        assertTrue(load.rawMessage.contains("Dallas"))
    }

    @Test
    fun blankMiddleStopIsSkipped() {
        val load = ManualLoadFactory.build(
            tripId = "T-SKIP1",
            date = "2026-08-19",
            rate = 900.0,
            miles = 200.0,
            pointA = "Austin, TX",
            pointB = "",
            extraPoints = listOf("Dallas, TX"),
            nowMillis = 1_776_441_600_000L,
        )
        assertEquals(2, load.stops.size)
        assertEquals("Austin, TX", load.stops[0].fullAddress)
        assertEquals("Dallas, TX", load.stops[1].fullAddress)
    }
}
