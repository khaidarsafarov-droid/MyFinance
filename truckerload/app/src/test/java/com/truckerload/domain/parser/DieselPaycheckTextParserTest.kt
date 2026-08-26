package com.truckerload.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DieselPaycheckTextParserTest {

    @Test
    fun diesel_parseFullReceipt_extractsAmountsAndLocation() {
        val text = """
            Fuel Receipt
            Date: 07/21/2026
            Merchant: Pilot
            Location: Knoxville, TN
            120.5 gallons
            @ ${'$'}4.15/gal
            Total Amount: ${'$'}500.08
        """.trimIndent()

        val parsed = DieselTextParser.parse(text)!!

        assertEquals("2026-07-21", parsed.date)
        assertEquals(500.08, parsed.totalAmount, 0.01)
        assertEquals(120.5, parsed.gallons!!, 0.01)
        assertEquals(4.15, parsed.pricePerGallon!!, 0.01)
        assertEquals("Knoxville, TN", parsed.location)
    }

    @Test
    fun diesel_parseAmountDueWithoutOptionalFields_stillParsesTotal() {
        val parsed = DieselTextParser.parse("Diesel\nAmount Due: ${'$'}75.50")

        assertEquals(75.50, parsed!!.totalAmount, 0.01)
        assertNull(parsed.gallons)
        assertNull(parsed.pricePerGallon)
    }

    @Test
    fun diesel_rejectsReceiptWithoutPositiveTotal() {
        assertFalse(DieselTextParser.looksLikeDiesel("Fuel receipt\n120 gal"))
        assertNull(DieselTextParser.parse("Fuel receipt\nTotal Amount: ${'$'}0.00"))
    }

    @Test
    fun paycheck_parseSettlement_extractsDriverDatesAndAmounts() {
        val text = """
            Driver: Alex Driver
            Week Start: 07/14/2026
            Week End: 07/20/2026
            Gross Pay: ${'$'}3,400.00
            Net Pay: ${'$'}2,750.25
        """.trimIndent()

        val parsed = PaycheckTextParser.parse(text)!!

        assertEquals("Alex Driver", parsed.driverName)
        assertEquals("2026-07-14", parsed.weekStartDate)
        assertEquals("2026-07-20", parsed.weekEndDate)
        assertEquals(3400.0, parsed.grossAmount!!, 0.01)
        assertEquals(2750.25, parsed.netAmount, 0.01)
    }

    @Test
    fun paycheck_parseGrandTotal_acceptsAlternateNetMarker() {
        val parsed = PaycheckTextParser.parse("Grand Total: ${'$'}1,100.00")

        assertEquals(1100.0, parsed!!.netAmount, 0.01)
        assertEquals("high", parsed.confidence)
    }

    @Test
    fun paycheck_prefersNetPayOverGrandTotal() {
        val parsed = PaycheckTextParser.parse(
            """
            Gross Pay: ${'$'}3,400.00
            Net Pay: ${'$'}2,750.25
            Grand Total: ${'$'}3,400.00
            """.trimIndent(),
        )
        assertEquals(2750.25, parsed!!.netAmount, 0.01)
        assertEquals(3400.0, parsed.grossAmount!!, 0.01)
    }

    @Test
    fun paycheck_rejectsMissingNetAmount() {
        assertFalse(PaycheckTextParser.looksLikePaycheck("Driver Settlement\nGross Pay: ${'$'}900"))
        assertNull(PaycheckTextParser.parse("Net Pay: ${'$'}0.00"))
    }

    @Test
    fun paycheck_looksLikePaycheck_acceptsSettlementTotalMarker() {
        assertTrue(PaycheckTextParser.looksLikePaycheck("Settlement Total: ${'$'}950.00"))
    }
}
