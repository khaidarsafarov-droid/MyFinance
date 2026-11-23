package com.truckerload.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.truckerload.domain.money.Money

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
        assertEquals(Money.fromDollars(2500.0), load.totalRate)
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
            Rate Confirmation IEL PO#: 4401901
            Estimated Rate (To Truck): $1,864.20
            Miles: 512.25
            Pick Ups
            Address: 400 DEMO DOCK RD CEDAR RAPIDS, IA 52404
            Deliveries
            Address: 900 SAMPLE LN BOISE, ID 83702
        """.trimIndent()
        val result = MessageParseService().parseLoadFromUserInput(text)
        assertTrue(result.isSuccess)
        val load = result.getOrThrow()
        assertEquals(Money.fromDollars(1864.20), load.totalRate)
        assertTrue(load.pointA.contains("CEDAR RAPIDS", ignoreCase = true))
    }

    @Test
    fun extractLoadFieldsDoesNotFailOnIncompleteOcr() {
        val draft = MessageParseService().extractLoadFields("Total Rate: 1000")
        assertEquals("1000", draft.rate)
        assertEquals("", draft.pointA)
    }
}
