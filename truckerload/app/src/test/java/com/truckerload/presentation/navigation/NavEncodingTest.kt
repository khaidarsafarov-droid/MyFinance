package com.truckerload.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import android.net.Uri

/** Mirrors [com.truckerload.presentation.navigation.Routes] path encoding. */
object NavEncoding {
    fun encodePathSegment(value: String): String =
        Uri.encode(value.ifBlank { "_" }) ?: "_"
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NavEncodingTest {

    @Test
    fun blankBecomesUnderscore() {
        assertEquals("_", NavEncoding.encodePathSegment(""))
    }

    @Test
    fun roundTripsSlashAndPercent() {
        val raw = "T-1/2%3"
        val encoded = NavEncoding.encodePathSegment(raw)
        assertFalse(encoded.contains("/"))
        assertEquals(raw, Uri.decode(encoded))
    }

    @Test
    fun roundTripsSpaces() {
        val encoded = NavEncoding.encodePathSegment("T 1")
        assertEquals("T 1", Uri.decode(encoded))
    }

    @Test
    fun cameraRouteEncodesSlashAndPercentInTripId() {
        val tripId = "T-1/2%3"
        val loadId = "load-1"
        val loadDate = "2026-07-22"
        val path = "camera_load/${NavEncoding.encodePathSegment(loadId)}/" +
            "${NavEncoding.encodePathSegment(tripId)}/${NavEncoding.encodePathSegment(loadDate)}"
        val segments = path.split("/")
        assertEquals(4, segments.size)
        assertFalse(segments[2].contains("/"))
        assertEquals(tripId, Uri.decode(segments[2]))
    }
}
