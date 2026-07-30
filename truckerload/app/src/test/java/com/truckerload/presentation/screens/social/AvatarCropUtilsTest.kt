package com.truckerload.presentation.screens.social

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AvatarCropUtilsTest {

    @Test
    fun cropSquare_centeredImage_returnsRequestedOutputSize() {
        val source = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.RED)
        }
        val container = 1080f
        val cropDiameter = 720f
        val fitScale = AvatarCropUtils.fitScale(source.width, source.height, container, container)
        val userScale = AvatarCropUtils.minUserScale(cropDiameter, source.width, source.height, fitScale)

        val cropped = AvatarCropUtils.cropSquare(
            source = source,
            containerWidth = container,
            containerHeight = container,
            cropDiameter = cropDiameter,
            fitScale = fitScale,
            userScale = userScale,
            offset = Offset.Zero,
            outputSize = 256,
        )

        assertEquals(256, cropped.width)
        assertEquals(256, cropped.height)
    }

    @Test
    fun cropSquare_withPanOffset_preservesShiftedFraming() {
        val source = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.BLACK)
            // Bright marker near the right side of the bitmap.
            for (x in 800 until 1000) {
                for (y in 400 until 600) {
                    setPixel(x, y, Color.YELLOW)
                }
            }
        }
        val container = 1000f
        val cropDiameter = 500f
        val fitScale = AvatarCropUtils.fitScale(source.width, source.height, container, container)
        val userScale = AvatarCropUtils.minUserScale(cropDiameter, source.width, source.height, fitScale)

        val centered = AvatarCropUtils.cropSquare(
            source = source,
            containerWidth = container,
            containerHeight = container,
            cropDiameter = cropDiameter,
            fitScale = fitScale,
            userScale = userScale,
            offset = Offset.Zero,
            outputSize = 100,
        )
        // Pan image left on screen → crop window shows more of the right side (yellow marker).
        val pannedLeft = AvatarCropUtils.cropSquare(
            source = source,
            containerWidth = container,
            containerHeight = container,
            cropDiameter = cropDiameter,
            fitScale = fitScale,
            userScale = userScale,
            offset = Offset(-180f, 0f),
            outputSize = 100,
        )

        val centeredYellow = countYellow(centered)
        val pannedYellow = countYellow(pannedLeft)
        assertTrue(
            "Panning should bring the right-side marker into the crop (centered=$centeredYellow, panned=$pannedYellow)",
            pannedYellow > centeredYellow,
        )
        assertNotEquals(0, pannedYellow)
    }

    @Test
    fun cropSquare_differentOffsets_produceDifferentBitmaps() {
        val source = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888).apply {
            for (x in 0 until width) {
                for (y in 0 until height) {
                    setPixel(x, y, Color.rgb(x % 256, y % 256, 128))
                }
            }
        }
        val containerW = 900f
        val containerH = 1200f
        val cropDiameter = 600f
        val fitScale = AvatarCropUtils.fitScale(source.width, source.height, containerW, containerH)
        val userScale = AvatarCropUtils.minUserScale(cropDiameter, source.width, source.height, fitScale) * 1.4f

        val a = AvatarCropUtils.cropSquare(
            source = source,
            containerWidth = containerW,
            containerHeight = containerH,
            cropDiameter = cropDiameter,
            fitScale = fitScale,
            userScale = userScale,
            offset = Offset(0f, 0f),
            outputSize = 64,
        )
        val b = AvatarCropUtils.cropSquare(
            source = source,
            containerWidth = containerW,
            containerHeight = containerH,
            cropDiameter = cropDiameter,
            fitScale = fitScale,
            userScale = userScale,
            offset = Offset(0f, 120f),
            outputSize = 64,
        )

        assertTrue(bitmapsDiffer(a, b))
    }

    @Test
    fun clampOffset_keepsCropWindowInsideScaledImage() {
        val offset = AvatarCropUtils.clampOffset(
            offset = Offset(10_000f, -10_000f),
            userScale = 2f,
            fitScale = 1f,
            bitmapWidth = 500,
            bitmapHeight = 500,
            containerWidth = 500f,
            containerHeight = 500f,
            cropDiameter = 300f,
        )
        val halfDisplay = 500f * 2f / 2f
        val halfCrop = 150f
        assertEquals(halfDisplay - halfCrop, offset.x, 0.01f)
        assertEquals(halfCrop - halfDisplay, offset.y, 0.01f)
    }

    private fun countYellow(bitmap: Bitmap): Int {
        var count = 0
        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                if (r > 200 && g > 200 && b < 80) count++
            }
        }
        return count
    }

    private fun bitmapsDiffer(a: Bitmap, b: Bitmap): Boolean {
        if (a.width != b.width || a.height != b.height) return true
        for (x in 0 until a.width) {
            for (y in 0 until a.height) {
                if (a.getPixel(x, y) != b.getPixel(x, y)) return true
            }
        }
        return false
    }
}
