package com.truckerload.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoManagerTitleTest {

    @Test
    fun resolveWatermarkTitle_prefersExplicitTitle() {
        assertEquals(
            "T-ABC",
            PhotoManager.resolveWatermarkTitle("T-ABC", "T-OTHER", "TruckoRig"),
        )
    }

    @Test
    fun resolveWatermarkTitle_fallsBackToTripThenDefault() {
        assertEquals(
            "T-1",
            PhotoManager.resolveWatermarkTitle(null, "T-1", "TruckoRig"),
        )
        assertEquals(
            "TruckoRig",
            PhotoManager.resolveWatermarkTitle("  ", null, "TruckoRig"),
        )
    }

    @Test
    fun resolveGpsWatermarkLine_unknownWhenNoCoordinates() {
        assertEquals(
            "Unknown",
            PhotoManager.resolveGpsWatermarkLine(
                hasCoordinates = false,
                coordinatesLine = "1.000° N, 2.000° W",
                unknownLabel = "Unknown",
            ),
        )
    }

    @Test
    fun resolveGpsWatermarkLine_usesCoordinatesWhenPresent() {
        assertEquals(
            "35.123° N, 78.456° W",
            PhotoManager.resolveGpsWatermarkLine(
                hasCoordinates = true,
                coordinatesLine = "35.123° N, 78.456° W",
                unknownLabel = "Unknown",
            ),
        )
    }

    @Test
    fun locationData_withoutCoords_hasCoordinatesFalse() {
        val empty = LocationData()
        assertFalse(empty.hasCoordinates)
        assertEquals("—", empty.coordinatesLine)

        val withGps = LocationData(latitude = 1.0, longitude = -2.0)
        assertTrue(withGps.hasCoordinates)
    }
}
