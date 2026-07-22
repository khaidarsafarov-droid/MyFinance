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
}
