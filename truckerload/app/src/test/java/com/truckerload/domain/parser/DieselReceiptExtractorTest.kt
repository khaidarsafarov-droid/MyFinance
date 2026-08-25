package com.truckerload.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DieselReceiptExtractorTest {

    @Test
    fun pumpDisplay_readsGallonsAndPpgWithoutTotal() {
        val text = """
            GALLONS
            45.230
            PRICE/GAL
            $3.899
        """.trimIndent()

        val fields = DieselReceiptExtractor.extract(text)

        assertEquals(45.230, fields.gallons!!, 0.001)
        assertEquals(3.899, fields.pricePerGallon!!, 0.001)
        assertNull(fields.totalAmount)
        assertTrue(fields.hasAnyField)
    }

    @Test
    fun pumpDisplay_inlineGalAndSlashPrice() {
        val fields = DieselReceiptExtractor.extract("102.450 GAL\n$3.459 / GAL")

        assertEquals(102.450, fields.gallons!!, 0.001)
        assertEquals(3.459, fields.pricePerGallon!!, 0.001)
    }

    @Test
    fun receipt_fillsKnownStopAndLeavesDiscountEmpty() {
        val text = """
            LOVE'S TRAVEL STOP
            4120 I-40, Oklahoma City, OK
            Diesel
            120.5 gallons
            Price $4.15 / gal
            Total Amount: $500.08
        """.trimIndent()

        val fields = DieselReceiptExtractor.extract(text)

        assertEquals(120.5, fields.gallons!!, 0.01)
        assertEquals(4.15, fields.pricePerGallon!!, 0.01)
        assertEquals(500.08, fields.totalAmount!!, 0.01)
        assertNull(fields.discountPricePerGallon)
        assertEquals("Love's Travel Stop", fields.vendor)
        assertTrue(fields.location!!.contains("Love's Travel Stop"))
        assertTrue(fields.location!!.contains("Oklahoma City, OK"))
    }

    @Test
    fun partialOcr_onlyGallons_leavesPriceEmpty() {
        val fields = DieselReceiptExtractor.extract("SALE GALS 88.1")

        assertEquals(88.1, fields.gallons!!, 0.01)
        assertNull(fields.pricePerGallon)
        assertNull(fields.totalAmount)
        assertNull(fields.location)
    }

    @Test
    fun derivesPpgWhenTotalAndGallonsPresent() {
        val fields = DieselReceiptExtractor.extract(
            "Diesel\nGallons: 50\nTotal Amount: $199.50",
        )

        assertEquals(50.0, fields.gallons!!, 0.01)
        assertEquals(3.99, fields.pricePerGallon!!, 0.01)
    }

    @Test
    fun pricePerGalLabel_isNotParsedAsGallons() {
        val fields = DieselReceiptExtractor.extract("PRICE/GAL\n3.899")
        assertEquals(3.899, fields.pricePerGallon!!, 0.001)
        assertNull(fields.gallons)
    }

    @Test
    fun blankText_hasNoFields() {
        val fields = DieselReceiptExtractor.extract("   ")
        assertFalse(fields.hasAnyField)
    }

    @Test
    fun formatField_trimsTrailingZeros() {
        assertEquals("45.23", DieselReceiptExtractor.formatField(45.230))
        assertEquals("4", DieselReceiptExtractor.formatField(4.0))
        assertEquals("3.899", DieselReceiptExtractor.formatField(3.899))
    }
}
