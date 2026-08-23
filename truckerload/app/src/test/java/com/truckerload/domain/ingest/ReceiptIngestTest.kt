package com.truckerload.domain.ingest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptKindClassifierTest {

    @Test
    fun loadBeatsGenericTotal() {
        val text = """
            Trip ID: T-116KYL6KW
            Total Rate: 2500.00
            Total Loaded Miles: 850 mi
            Pu-address: SWF2, Garner, NC
        """.trimIndent()
        assertEquals(ReceiptKind.LOAD, ReceiptKindClassifier.classify(text))
    }

    @Test
    fun paycheckFromNetPay() {
        val text = """
            Driver Settlement
            Net Pay: $2,750.25
            Gross Pay: $3,400.00
        """.trimIndent()
        assertEquals(ReceiptKind.PAYCHECK, ReceiptKindClassifier.classify(text))
    }

    @Test
    fun dieselFromFuelReceipt() {
        val text = """
            Fuel Receipt
            Pilot
            120.5 gallons
            Diesel
            Total Amount: $500.08
        """.trimIndent()
        assertEquals(ReceiptKind.DIESEL, ReceiptKindClassifier.classify(text))
    }

    @Test
    fun defBeatsDieselWhenDefIsNamed() {
        val text = """
            Love's
            DEF fill
            AdBlue
            Diesel Exhaust Fluid
            8.0 gal
            Total Amount: $42.10
        """.trimIndent()
        assertEquals(ReceiptKind.DEF, ReceiptKindClassifier.classify(text))
    }

    @Test
    fun russianPaycheckKeyword() {
        assertEquals(
            ReceiptKind.PAYCHECK,
            ReceiptKindClassifier.classify("Зарплата\nGrand Total: $1,100.00"),
        )
    }

    @Test
    fun rateConfirmationFilenameBeatsLovesMention() {
        val text = """
            Rate Confirmation
            Estimated Rate: $2,325.84
            Miles: 742.50
            Pick Ups
            Address: 1325 ENSELL RD LAKE ZURICH, IL 60047
            LOVES TRAVEL STOP #800
            Deliveries
            Address: 7091 TROY HILL DRIVE ELKRIDGE, MD 21075
        """.trimIndent()
        assertEquals(
            ReceiptKind.LOAD,
            ReceiptKindClassifier.classify(text, "2665704-Carrier-Rate-Confirmation.pdf"),
        )
    }
}

class ReceiptFieldExtractorTest {

    @Test
    fun extractsDieselAmountGallonsAndLocation() {
        val text = """
            Fuel Receipt
            Date: 07/21/2026
            Merchant: Pilot
            Location: Knoxville, TN
            Diesel
            120.5 gallons
            @ $4.15/gal
            Total Amount: $500.08
        """.trimIndent()
        val preview = ReceiptFieldExtractor.extract(text, fileName = "pilot.jpg")
        assertEquals(ReceiptKind.DIESEL, preview.kind)
        assertEquals(500.08, preview.amount!!, 0.01)
        assertEquals(120.5, preview.gallons!!, 0.01)
        assertEquals("Knoxville, TN", preview.location)
        assertEquals("pilot.jpg", preview.sourceFileName)
        assertTrue(preview.highlightToken!!.contains("500"))
    }

    @Test
    fun extractsPaycheckNet() {
        val preview = ReceiptFieldExtractor.extract(
            "Driver: Alex\nNet Pay: $2,750.25\nWeek Start: 07/14/2026",
        )
        assertEquals(ReceiptKind.PAYCHECK, preview.kind)
        assertEquals(2750.25, preview.amount!!, 0.01)
        assertEquals("Alex", preview.driverName)
    }
}

class ReceiptPreviewFormatterTest {

    @Test
    fun highlightsAmountAndEscapesHtml() {
        val html = ReceiptPreviewFormatter.highlightSnippet(
            text = "Pilot <store>\nTotal Amount: $42.10 paid",
            token = "$42.10",
        )
        assertTrue(html.contains("<b>\$42.10</b>"))
        assertTrue(html.contains("&lt;store&gt;"))
        assertTrue(!html.contains("<store>"))
    }
}

class DocumentBytesDecoderTest {

    @Test
    fun decodesPlainTextAndHtml() {
        val txt = DocumentBytesDecoder.decode(
            "Diesel Total Amount: $10.00".toByteArray(),
            fileName = "receipt.txt",
            mimeType = "text/plain",
        )
        assertTrue(txt!!.contains("Diesel"))
        val html = DocumentBytesDecoder.decode(
            "<html><body><p>Net Pay: $99.00</p></body></html>".toByteArray(),
            fileName = "pay.html",
            mimeType = "text/html",
        )
        assertTrue(html!!.contains("Net Pay"))
        assertTrue(!html.contains("<p>"))
    }

    @Test
    fun detectsPdfMagicAndSkipsDecode() {
        val bytes = "%PDF-1.4 fake".toByteArray()
        assertTrue(DocumentBytesDecoder.isPdf(bytes, "x.pdf", ""))
        assertEquals(null, DocumentBytesDecoder.decode(bytes, "x.pdf", "application/pdf"))
    }

    @Test
    fun jpegMagicIsImage() {
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        assertTrue(DocumentBytesDecoder.isImage(jpeg, "a.jpg", ""))
    }
}
