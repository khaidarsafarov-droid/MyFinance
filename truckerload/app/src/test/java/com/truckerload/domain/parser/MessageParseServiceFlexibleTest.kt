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
    fun inboundRateConfirmationParsesLoadFields() {
        val text = """
            Rate Confirmation IEL PO#: 2665704
            Estimated Rate (To Truck): $2,325.84
            Miles: 742.50
            Pick Ups
            Address: 1325 ENSELL RD LAKE ZURICH, IL 60047
            Deliveries
            Address: 7091 TROY HILL DRIVE ELKRIDGE, MD 21075
        """.trimIndent()
        val result = MessageParseService().parseLoadFromUserInput(text)
        assertTrue(result.isSuccess)
        val load = result.getOrThrow()
        assertEquals(2325.84, load.totalRate, 0.01)
        assertTrue(load.pointA.contains("LAKE ZURICH", ignoreCase = true))
    }

    @Test
    fun extractLoadFieldsDoesNotFailOnIncompleteOcr() {
        val draft = MessageParseService().extractLoadFields("Total Rate: 1000")
        assertEquals("1000", draft.rate)
        assertEquals("", draft.pointA)
    }
}
