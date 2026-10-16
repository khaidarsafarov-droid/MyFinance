package com.truckerload.utils.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrReadingOrderTest {

    @Test
    fun join_readsTopToBottomAndLeftToRightOnARow() {
        val text = OcrReadingOrder.join(
            listOf(
                OcrLine(top = 80, left = 400, text = "Dallas, TX"),
                OcrLine(top = 10, left = 20, text = "Total Rate:"),
                OcrLine(top = 14, left = 180, text = "$2,500.00"),
                OcrLine(top = 80, left = 20, text = "Pickup"),
            ),
        )
        assertEquals(
            "Total Rate: $2,500.00\nPickup Dallas, TX",
            text,
        )
    }
}
