package com.truckerload.domain.parser

import com.truckerload.domain.import.parser.RelayMessageParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageParserTest {

    private val parser = RelayMessageParser()

    @Test
    fun `relay parser extracts trip rate and miles`() {
        val message = """
            Trip ID: T-116KYL6KW
            Total Rate: ${'$'}2,500.00
            Total Loaded Miles: 850 mi
            PU# 115S1Q2P1
            Pu-address: SWF2, Hopewell Junction, NY
            Del-address: TOL3, Garner, NC
        """.trimIndent()

        val loads = parser.parse(message)
        assertTrue(loads.isNotEmpty())
        assertEquals("T-116KYL6KW", loads[0].tripId)
        assertEquals(2500.0, loads[0].totalRate, 0.01)
        assertEquals(850.0, loads[0].totalMiles, 0.01)
    }
}
