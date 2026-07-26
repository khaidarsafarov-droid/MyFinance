package com.truckerload.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiptDataTest {

    @Test
    fun roundTrip_archiveEntry_preservesFields() {
        val receipt = ReceiptData(
            id = 7,
            imageUri = "/data/files/receipts/receipt_1.jpg",
            serviceName = "Love's",
            date = ReceiptData.isoDateToEpochMillis("2026-07-15"),
            totalAmount = 350.50,
            description = "Oil change and filters",
            rawText = "Love's\nTotal \$350.50",
        )
        val entry = receipt.toArchiveEntry()
        assertEquals("Love's", entry.serviceName)
        assertEquals("2026-07-15", entry.serviceDate)
        assertEquals(350.50, entry.amount, 0.01)
        assertEquals("/data/files/receipts/receipt_1.jpg", entry.photoPath)
        assertEquals("Oil change and filters", entry.description)

        val back = ReceiptData.fromArchive(entry)
        assertEquals(receipt.serviceName, back.serviceName)
        assertEquals(receipt.totalAmount, back.totalAmount, 0.01)
        assertEquals(receipt.description, back.description)
        assertEquals(receipt.imageUri, back.imageUri)
        assertEquals("2026-07-15", ReceiptData.epochMillisToIsoDate(back.date))
    }
}
