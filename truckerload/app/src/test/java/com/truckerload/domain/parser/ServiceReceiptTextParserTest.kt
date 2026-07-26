package com.truckerload.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ServiceReceiptTextParserTest {

    @Test
    fun parse_extractsGrandTotalAndDate() {
        val text = """
            Truck Service Center
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
        assertNotNull(result.descriptionHint)
    }

    @Test
    fun parse_russianItogo() {
        val text = """
            Автосервис
            Замена масла
            Дата: 15.07.2026
            Итого: 450.50
        """.trimIndent()
        val result = ServiceReceiptTextParser.parse(text)
        assertEquals(450.50, result.amount!!, 0.01)
        assertEquals("2026-07-15", result.date)
    }
}
