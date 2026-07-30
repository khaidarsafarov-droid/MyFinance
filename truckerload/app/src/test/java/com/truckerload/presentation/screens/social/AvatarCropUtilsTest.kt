package com.truckerload.presentation.screens.social

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.exifinterface.media.ExifInterface
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
    fun cropSquare_pannedOffset_samplesExpectedRegion() {
        val source = Bitmap.createBitmap(1000, 500, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.BLUE)
        for (x in 0 until 500) {
            for (y in 0 until source.height) {
                source.setPixel(x, y, Color.RED)
            }
        }

        val containerWidth = 1080f
        val containerHeight = 1600f
        val cropDiameter = 720f
        val fitScale = AvatarCropUtils.fitScale(source.width, source.height, containerWidth, containerHeight)
        val userScale = AvatarCropUtils.minUserScale(cropDiameter, source.width, source.height, fitScale)
        val offset = Offset(x = 180f, y = 0f)

        val cropped = AvatarCropUtils.cropSquare(
            source = source,
            containerWidth = containerWidth,
            containerHeight = containerHeight,
            cropDiameter = cropDiameter,
            fitScale = fitScale,
            userScale = userScale,
            offset = offset,
            outputSize = 64,
        )

        var redCount = 0
        for (x in 0 until cropped.width) {
            for (y in 0 until cropped.height) {
                if (cropped.getPixel(x, y) == Color.RED) redCount++
            }
        }
        assertTrue(redCount > cropped.width * cropped.height / 2)
    }

    @Test
    fun rotateBitmapForExif_rotate90_swapsDimensions() {
        val source = Bitmap.createBitmap(1200, 800, Bitmap.Config.ARGB_8888)
        val rotated = AvatarCropUtils.rotateBitmapForExif(source, ExifInterface.ORIENTATION_ROTATE_90)
        assertEquals(800, rotated.width)
        assertEquals(1200, rotated.height)
        if (rotated !== source) rotated.recycle()
        source.recycle()
    }

    @Test
    fun rotateBitmapForExif_normal_keepsBitmap() {
        val source = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
        val rotated = AvatarCropUtils.rotateBitmapForExif(source, ExifInterface.ORIENTATION_NORMAL)
        assertEquals(source, rotated)
        source.recycle()
    }

    @Test
    fun clampOffset_panBeyondBounds_isLimitedToCropWindow() {
        val container = 1080f
        val cropDiameter = 720f
        val fitScale = 1f
        val userScale = 2f
        val bitmapSize = 1000

        val clamped = AvatarCropUtils.clampOffset(
            offset = Offset(500f, 500f),
            userScale = userScale,
            fitScale = fitScale,
            bitmapWidth = bitmapSize,
            bitmapHeight = bitmapSize,
            containerWidth = container,
            containerHeight = container,
            cropDiameter = cropDiameter,
        )

        assertNotEquals(500f, clamped.x)
        assertNotEquals(500f, clamped.y)
        assertTrue(clamped.x < 500f)
        assertTrue(clamped.y < 500f)
    }
}
