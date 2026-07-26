package com.truckerload.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceReceiptTextParserTest {

    @Test
    fun parse_extractsGrandTotalDateAndService() {
        val text = """
            Love's Truck Tire Care
            Oil change + filters
            Date: 07/15/2026
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
    fun parse_blank_returnsNulls() {
        val result = ServiceReceiptTextParser.parse("   ")
        assertEquals(null, result.amount)
        assertEquals(null, result.date)
        assertEquals(null, result.serviceName)
        assertEquals(null, result.descriptionHint)
    }
}
