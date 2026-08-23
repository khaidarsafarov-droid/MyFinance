package com.truckerload.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageClassifierTest {

    @Test
    fun classify_blankText_returnsUnknown() {
        assertEquals(MessageType.UNKNOWN, MessageClassifier.classify("   "))
    }

    @Test
    fun classify_relayTripId_returnsLoad() {
        val text = """
            Trip ID: T-116KYL6KW
            Total Rate: 2500.00
        """.trimIndent()

        assertEquals(MessageType.LOAD, MessageClassifier.classify(text))
    }

    @Test
    fun classify_paycheckNetPay_returnsPaycheck() {
        val text = """
            Driver Settlement
            Net Pay: ${'$'}1,234.56
        """.trimIndent()

        assertEquals(MessageType.PAYCHECK, MessageClassifier.classify(text))
    }

    @Test
    fun classify_dieselReceiptWithTotal_returnsDiesel() {
        val text = """
            Fuel Receipt
            Total Amount: ${'$'}450.25
            120.5 gallons
        """.trimIndent()

        assertEquals(MessageType.DIESEL, MessageClassifier.classify(text))
    }

    @Test
    fun classify_dieselKeywordWithoutAmount_returnsUnknown() {
        assertEquals(MessageType.UNKNOWN, MessageClassifier.classify("Diesel stop at Knoxville"))
    }

    @Test
    fun isLoadLike_requiresLoadMarker() {
        assertTrue(MessageClassifier.isLoadLike("PU# 12345"))
        assertFalse(MessageClassifier.isLoadLike("Net Pay: ${'$'}500.00"))
    }

    @Test
    fun classify_brokerRateConfirmation_returnsLoad() {
        val text = """
            Rate Confirmation IEL PO#: 2665704
            Estimated Rate (To Truck): ${'$'}2,325.84
            Miles: 742.50
        """.trimIndent()
        assertEquals(MessageType.LOAD, MessageClassifier.classify(text))
        assertTrue(MessageClassifier.isLoadLike(text))
    }
}
