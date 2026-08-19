package com.truckerload.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageParseServiceFlexibleTest {

    @Test
    fun pastedMessageWithoutTripIdStillSavesViaFallback() {
        val text = """
            Total Rate: 2500.00
            Total Loaded Miles: 850 mi
            Pu-address: SWF2, Garner, NC
            Del-address: Dallas, TX
        """.trimIndent()

        val result = MessageParseService().parseLoadFromUserInput(text)
        assertTrue(result.isSuccess)
        val load = result.getOrThrow()
        assertEquals(2500.0, load.totalRate, 0.01)
        assertEquals(850.0, load.totalMiles, 0.01)
        assertTrue(load.tripId.isNotBlank())
    }

    @Test
    fun extractLoadFieldsMapsOcrIntoFormBoxes() {
        val text = """
            Trip ID
            T-116KYL6KW
            Total Rate
            2500.00
            Total Loaded Miles
            850 mi
            Pu-address
            SWF2, Garner, NC
            Del-address
            Dallas, TX
        """.trimIndent()

        val draft = MessageParseService().extractLoadFields(text)
        assertEquals("T-116KYL6KW", draft.tripId)
        assertEquals("2500", draft.rate)
        assertEquals("850", draft.miles)
        assertTrue(draft.pointA.contains("Garner"))
        assertTrue(draft.pointB.contains("Dallas"))
    }

    @Test
    fun extractLoadFieldsDoesNotFailOnIncompleteOcr() {
        val draft = MessageParseService().extractLoadFields("Total Rate: 1000")
        assertEquals("1000", draft.rate)
        assertEquals("", draft.pointA)
    }
}
