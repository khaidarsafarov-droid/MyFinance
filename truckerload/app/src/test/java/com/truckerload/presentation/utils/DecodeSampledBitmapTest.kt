package com.truckerload.presentation.utils

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun computeInSampleSize_growsForLargeDimensions() {
        assertTrue(computeInSampleSize(100, 100, maxEdgePx = 512) == 1)
        val sample4k = computeInSampleSize(4000, 3000, maxEdgePx = 512)
        assertTrue("expected sample>=4 for 4k, got $sample4k", sample4k >= 4)
        val sample8k = computeInSampleSize(8000, 8000, maxEdgePx = 256)
        assertTrue("expected larger sample for 8k, got $sample8k vs $sample4k", sample8k > sample4k)
    }
}
