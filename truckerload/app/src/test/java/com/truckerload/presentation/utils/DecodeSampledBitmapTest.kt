package com.truckerload.presentation.utils

import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DecodeSampledBitmapTest {

    @Test
    fun decodeSampledBitmap_missingFile_returnsNull() {
        val missing = File("/tmp/truckerload_missing_${System.nanoTime()}.jpg")
        assertNull(decodeSampledBitmap(missing.absolutePath, maxEdgePx = 512))
    }

    @Test
    fun decodeSampledBitmap_emptyPath_returnsNull() {
        assertNull(decodeSampledBitmap("", maxEdgePx = 256))
    }
}
