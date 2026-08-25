package com.truckerload.presentation.screens.add

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.truckerload.domain.parser.DieselReceiptExtractor
import com.truckerload.domain.parser.DieselReceiptFields

class DieselFormFillTest {

    private val labels = DieselScanLabels(
        gallons = "gallons",
        price = "price",
        discount = "discount",
        location = "location",
        noneMessage = "none",
        foundTemplate = "found: %1\$s",
    )

    @Test
    fun applyScan_fillsFoundFieldsAndLeavesOthersEmpty() {
        val next = DieselFormFill.applyScan(
            state = AddDieselUiState(),
            fields = DieselReceiptFields(gallons = 45.23, pricePerGallon = 3.899),
            rawText = "GALLONS 45.23",
            labels = labels,
        )
        assertEquals("45.23", next.gallonsText)
        assertEquals("3.899", next.pricePerGallonText)
        assertEquals("", next.discountPriceText)
        assertEquals("", next.locationText)
        assertEquals("GALLONS 45.23", next.rawExtractedText)
        assertEquals("found: gallons, price", next.scanMessage)
        assertEquals(false, next.isScanning)
    }

    @Test
    fun applyScan_keepsExistingWhenOcrMissesThem() {
        val next = DieselFormFill.applyScan(
            state = AddDieselUiState(gallonsText = "10", locationText = "Pilot"),
            fields = DieselReceiptFields(pricePerGallon = 4.0),
            rawText = "PPG 4.00",
            labels = labels,
        )
        assertEquals("10", next.gallonsText)
        assertEquals("4", next.pricePerGallonText)
        assertEquals("Pilot", next.locationText)
        assertTrue(next.scanMessage!!.contains("price"))
    }

    @Test
    fun applyScan_blankOcr_keepsFormAndShowsNone() {
        val next = DieselFormFill.applyScan(
            state = AddDieselUiState(gallonsText = "12"),
            fields = DieselReceiptExtractor.extract(""),
            rawText = "",
            labels = labels,
        )
        assertEquals("12", next.gallonsText)
        assertEquals("none", next.scanMessage)
    }
}
