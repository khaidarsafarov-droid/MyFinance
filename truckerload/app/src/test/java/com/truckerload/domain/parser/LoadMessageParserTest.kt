package com.truckerload.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadMessageParserTest {

    @Test
    fun `unicode Trip ID is primary key not first PU number`() {
        val message = """
            𝗧𝗿𝗶𝗽 𝗜𝗗:  T-116P1ZLK1  PU# 1162L65P5 Note: Empty trailer
            Pu-time: 07/03 07:49 CDT
            Pu-address: DAL91400 Southport Parkway
            WILMER, TX 75172
            Del-time: 07/03 09:29 CDT
            Del-address: VENDOR-10122706905600 Mark IV Pky
            FORT WORTH, TX 76131
            -----------------------------------
            PU# 113RDYNXF Note: Preloaded
            Pu-time: 07/03 09:30 CDT
            Pu-address: VENDOR-10122706905600 Mark IV Pky
            FORT WORTH, TX 76131
            Del-time: 07/05 07:00 EDT
            Del-address: XCH22150 DEAN FOREST RD
            Garden City, GA 31408
            Total Rate: ${'$'}2710.550048828125
            Total Loaded Miles: 1121.81 mi
        """.trimIndent()

        val loads = LoadMessageParser.parseAll(message)
        assertEquals(1, loads.size)
        assertEquals("T-116P1ZLK1", loads[0].tripId)
        assertEquals(2710.55, loads[0].totalRate, 0.01)
        assertEquals(1121.81, loads[0].totalMiles, 0.01)
        assertEquals(2, loads[0].puCount)
        assertEquals("1162L65P5", loads[0].stops.first { it.puNumber != null }.puNumber)
    }

    @Test
    fun `message without Trip ID is not parsed from PU number`() {
        val message = """
            PU# 1162L65P5 Note: Empty trailer
            Pu-address: WILMER, TX 75172
            Del-address: Garden City, GA 31408
            Total Rate: ${'$'}2710.55
            Total Loaded Miles: 1121.81 mi
        """.trimIndent()

        assertNull(LoadMessageParser.parseOne(message))
    }

    @Test
    fun `multiple Trip ID blocks produce separate loads`() {
        val message = """
            Trip ID: T-AAA111
            Total Rate: ${'$'}1000
            Total Loaded Miles: 500 mi
            PU# PU1
            Pu-address: Austin, TX
            Del-address: Dallas, TX

            Trip ID: T-BBB222
            Total Rate: ${'$'}2000
            Total Loaded Miles: 600 mi
            PU# PU2
            Pu-address: Houston, TX
            Del-address: San Antonio, TX
        """.trimIndent()

        val loads = LoadMessageParser.parseAll(message)
        assertEquals(2, loads.size)
        assertEquals("T-AAA111", loads[0].tripId)
        assertEquals("T-BBB222", loads[1].tripId)
    }

    @Test
    fun `same Trip ID with changed PU numbers stays one trip`() {
        val original = """
            Trip ID: T-116P1ZLK1
            Total Rate: ${'$'}2710.55
            Total Loaded Miles: 1121.81 mi
            PU# 1162L65P5
            Pu-address: WILMER, TX 75172
            Del-address: Garden City, GA 31408
        """.trimIndent()
        val updated = """
            Trip ID: T-116P1ZLK1
            Total Rate: ${'$'}2800.00
            Total Loaded Miles: 1121.81 mi
            PU# 999NEWPU
            Pu-address: DALLAS, TX 75201
            Del-address: Garden City, GA 31408
        """.trimIndent()

        val first = LoadMessageParser.parseOne(original)!!
        val second = LoadMessageParser.parseOne(updated)!!
        assertEquals(first.tripId, second.tripId)
        assertTrue(second.stops.any { it.puNumber == "999NEWPU" })
    }
}
