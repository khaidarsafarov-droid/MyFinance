package com.truckerload.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Documents the intended Relay parse logic from RELAY_PARSE_EXAMPLES / AGENTS.md.
 */
class LoadMessageParserLogicTest {

    @Test
    fun basicOneLiner_facilityCityState() {
        val message = """
            Trip ID: T-116KYL6KW
            Total Rate: 2500.00
            Total Loaded Miles: 850 mi
            Pu-address: SWF2, Garner, NC
            Del-address: TOL3, Perrysburg, OH
        """.trimIndent()

        val load = LoadMessageParser.parseOne(message)
        assertNotNull(load)
        assertEquals("T-116KYL6KW", load!!.tripId)
        assertEquals(2500.0, load.totalRate, 0.01)
        assertEquals(850.0, load.totalMiles, 0.01)
        assertEquals("Garner, NC", load.pointA)
        assertEquals("Perrysburg, OH", load.pointB)
        assertEquals("SWF2", load.stops.first().facilityCode)
        assertEquals("TOL3", load.stops.last().facilityCode)
        assertEquals(1, load.puCount)
        assertEquals(1, load.delCount)
    }

    @Test
    fun multiStopWithoutPuHeaders_createsSeparateStops() {
        val message = """
            Trip ID: T-MULTI-003
            Total Rate: 3200
            Total Loaded Miles: 1180 mi
            Pu-address: ONT8, Moreno Valley, CA
            Pu-address: LAS1, North Las Vegas, NV
            Del-address: DEN5, Aurora, CO
            Del-address: MCI9, Liberty, MO
        """.trimIndent()

        val load = LoadMessageParser.parseOne(message)
        assertNotNull(load)
        assertEquals(2, load!!.puCount)
        assertEquals(2, load.delCount)
        assertEquals("Moreno Valley, CA", load.pointA)
        assertEquals("Liberty, MO", load.pointB)
        assertEquals(
            listOf("ONT8", "LAS1", "DEN5", "MCI9"),
            load.stops.map { it.facilityCode },
        )
    }

    @Test
    fun realRelayMultilineAddress_keepsFacilityAndCity() {
        val message = """
            Trip ID: T-116KYL6KW
            PU# 115S1Q2P1
            Note: Empty trailer
            Pu-time: 06/28 03:00 EDT
            Pu-address: SWF2
            76 Patriot Way
            Hopewell Junction, NY 12533
            Del-time: 06/29 07:00 EDT
            Del-address: TOL3
            9240 Fremont Pike
            PERRYSBURG, Ohio 43551
            Total Rate: ${'$'}2945.56
            Total Loaded Miles: 1198.03 mi
        """.trimIndent()

        val load = LoadMessageParser.parseOne(message)
        assertNotNull(load)
        assertEquals("Hopewell Junction, NY", load!!.pointA)
        assertEquals("PERRYSBURG, OH", load.pointB)
        assertEquals("SWF2", load.stops[0].facilityCode)
        assertEquals("TOL3", load.stops[1].facilityCode)
        assertEquals("12533", load.stops[0].zip)
        assertEquals("43551", load.stops[1].zip)
        assertTrue(load.totalRate > 2900)
    }

    @Test
    fun plainCityState_withoutFacilityStillWorks() {
        val message = """
            Trip ID: T-CITYONLY1
            Total Rate: 1500
            Total Loaded Miles: 400 mi
            Pu-address: Austin, TX
            Del-address: Dallas, TX
        """.trimIndent()

        val load = LoadMessageParser.parseOne(message)
        assertNotNull(load)
        assertEquals("Austin, TX", load!!.pointA)
        assertEquals("Dallas, TX", load.pointB)
        assertEquals(null, load.stops[0].facilityCode)
    }
}
