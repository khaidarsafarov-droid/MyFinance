package com.truckerload.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ServiceReceiptTextParserTest {

    @Test
    fun parse_prefersGrandTotalOverLineItemsAndSubtotal() {
        val text = """
            Love's Truck Tire Care
            Oil change + filters
            Date: 07/15/2026
            Labor $999.00
            Filter $45.00
            Subtotal $180.00
            Tax $20.00
            Grand Total: $200.00
            Thank you
        """.trimIndent()

        val result = ServiceReceiptTextParser.parse(text)
        assertEquals(200.0, result.amount!!, 0.01)
        assertEquals("2026-07-15", result.date)
        assertNotNull(result.serviceName)
        assertTrue(result.serviceName!!.contains("Love", ignoreCase = true))
        assertEquals("Oil change + filters", result.descriptionHint)
    }

    @Test
    fun parse_ignoresTaxAndPicksAmountDue() {
        val text = """
            Speedco #12
            Brake pads
            Parts $120.00
            Tax $9.60
            Amount Due: $350.50
        """.trimIndent()
        val result = ServiceReceiptTextParser.parse(text, defaultDate = "2026-01-01")
        assertEquals(350.50, result.amount!!, 0.01)
        assertTrue(result.serviceName!!.contains("Speedco", ignoreCase = true))
    }

    @Test
    fun parse_russianItogo() {
        val text = """
            Автосервис Север
            Замена масла
            Дата: 15.07.2026
            Итого: 450.50
        """.trimIndent()
        val result = ServiceReceiptTextParser.parse(text)
        assertEquals(450.50, result.amount!!, 0.01)
        assertEquals("2026-07-15", result.date)
        assertEquals("Автосервис Север", result.serviceName)
        assertEquals("Замена масла", result.descriptionHint)
    }

    @Test
    fun parse_isoDateAndAmountKeyword() {
        val text = """
            TA Truck Service #421
            2026-03-01
            Brake pads
            Amount Due: $350.50
        """.trimIndent()
        val result = ServiceReceiptTextParser.parse(text)
        assertEquals(350.50, result.amount!!, 0.01)
        assertEquals("2026-03-01", result.date)
        assertTrue(result.serviceName!!.contains("TA Truck Service", ignoreCase = true))
    }

    @Test
    fun parse_textMonthDate() {
        val text = """
            Pilot Flying J
            Oil filter
            Date: Jul 15, 2026
            TOTAL $88.00
        """.trimIndent()
        val result = ServiceReceiptTextParser.parse(text)
        assertEquals("2026-07-15", result.date)
        assertEquals(88.0, result.amount!!, 0.01)
        assertTrue(result.serviceName!!.contains("Pilot", ignoreCase = true))
    }

    @Test
    fun parse_defaultsDateWhenMissing() {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val text = """
            Petro Stopping Centers
            DEF fill
            TOTAL $42.00
        """.trimIndent()
        val result = ServiceReceiptTextParser.parse(text)
        assertEquals(today, result.date)
        assertEquals(42.0, result.amount!!, 0.01)
    }

    @Test
    fun parse_blank_returnsDefaultDateOnly() {
        val result = ServiceReceiptTextParser.parse("   ", defaultDate = "2026-07-26")
        assertNull(result.amount)
        assertEquals("2026-07-26", result.date)
        assertNull(result.serviceName)
        assertNull(result.descriptionHint)
    }
}
