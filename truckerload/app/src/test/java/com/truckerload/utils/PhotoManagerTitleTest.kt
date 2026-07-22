package com.truckerload.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoManagerTitleTest {

    @Test
    fun resolveWatermarkTitle_prefersExplicitTitle() {
        assertEquals(
            "T-ABC",
            PhotoManager.resolveWatermarkTitle("T-ABC", "T-OTHER", "Truck Log"),
        )
    }

    @Test
    fun resolveWatermarkTitle_fallsBackToTripThenDefault() {
        assertEquals(
            "T-1",
            PhotoManager.resolveWatermarkTitle(null, "T-1", "Truck Log"),
        )
        assertEquals(
            "Truck Log",
            PhotoManager.resolveWatermarkTitle("  ", null, "Truck Log"),
        )
    }
}
