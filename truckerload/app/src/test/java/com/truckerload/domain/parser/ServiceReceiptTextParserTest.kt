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
        assertTrue(result.lineItems.isEmpty())
    }

    @Test
    fun parse_invoiceTable_extractsEachServiceAndKeepsGrandTotal() {
        val text = """
            Invoice Date: 08/25/2026
            Due Date: 08/25/2026
            Item Description Unit Price Quantity Tax Amount
            VALVE ADJUSTMENT 500.00 1.00 500.00
            OIL FILTER 43.12 1.00 43.12
            FUEL FILTER 92.31 1.00 92.31
            OIL 15.40 198.21 1.00 198.21
            PM IOL CHANGE 180.00 1.00 180.00
            FILTER AIR MOUNT 24.11 2.00 48.22
            SHOP SUPLIES 23.00 1.00 23.00
            Subtotal 1084.86
            Total 1084.86
            Amount Paid 0.00
            Balance Due 1084.86
        """.trimIndent()

        val result = ServiceReceiptTextParser.parse(text)
        assertEquals(1084.86, result.amount!!, 0.01)
        assertEquals("2026-08-25", result.date)
        val lines = result.lineItems
        assertEquals(7, lines.size)
        assertEquals("VALVE ADJUSTMENT", lines[0].description)
        assertEquals(500.00, lines[0].amount, 0.01)
        assertEquals("OIL 15.40", lines[3].description)
        assertEquals(198.21, lines[3].amount, 0.01)
        assertEquals("FILTER AIR MOUNT", lines[5].description)
        assertEquals(48.22, lines[5].amount, 0.01)
        assertEquals(1084.86, lines.sumOf { it.amount }, 0.01)
    }

    @Test
    fun parse_lineItemOnFollowingLine() {
        val text = """
            Speedco
            Brake pads
            350.50
            TOTAL 350.50
        """.trimIndent()
        val result = ServiceReceiptTextParser.parse(text)
        assertEquals(1, result.lineItems.size)
        assertEquals("Brake pads", result.lineItems[0].description)
        assertEquals(350.50, result.lineItems[0].amount, 0.01)
    }
}
