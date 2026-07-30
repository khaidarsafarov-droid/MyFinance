package com.truckerload.presentation.screens.social

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.exifinterface.media.ExifInterface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
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
        assertEquals(Color.RED, cropped.getPixel(128, 128))
    }

    @Test
    fun cropSquare_panOffset_changesWhichPixelsAreKept() {
        val source = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888).apply {
            for (x in 0 until 500) {
                for (y in 0 until height) setPixel(x, y, Color.RED)
            }
            for (x in 500 until width) {
                for (y in 0 until height) setPixel(x, y, Color.BLUE)
            }
        }
        val container = 1080f
        val cropDiameter = 720f
        val fitScale = AvatarCropUtils.fitScale(source.width, source.height, container, container)
        val userScale = AvatarCropUtils.minUserScale(cropDiameter, source.width, source.height, fitScale)
        val maxPan = AvatarCropUtils.clampOffset(
            offset = Offset(10_000f, 0f),
            userScale = userScale,
            fitScale = fitScale,
            bitmapWidth = source.width,
            bitmapHeight = source.height,
            containerWidth = container,
            containerHeight = container,
            cropDiameter = cropDiameter,
        ).x

        val centered = AvatarCropUtils.cropSquare(
            source = source,
            containerWidth = container,
            containerHeight = container,
            cropDiameter = cropDiameter,
            fitScale = fitScale,
            userScale = userScale,
            offset = Offset.Zero,
            outputSize = 64,
        )
        val pannedRight = AvatarCropUtils.cropSquare(
            source = source,
            containerWidth = container,
            containerHeight = container,
            cropDiameter = cropDiameter,
            fitScale = fitScale,
            userScale = userScale,
            offset = Offset(maxPan, 0f),
            outputSize = 64,
        )
        val pannedLeft = AvatarCropUtils.cropSquare(
            source = source,
            containerWidth = container,
            containerHeight = container,
            cropDiameter = cropDiameter,
            fitScale = fitScale,
            userScale = userScale,
            offset = Offset(-maxPan, 0f),
            outputSize = 64,
        )

        assertEquals(Color.RED, centered.getPixel(8, 32))
        assertEquals(Color.BLUE, centered.getPixel(56, 32))
        assertEquals(Color.RED, pannedRight.getPixel(32, 32))
        assertEquals(Color.BLUE, pannedLeft.getPixel(32, 32))
        assertNotEquals(pannedRight.getPixel(32, 32), pannedLeft.getPixel(32, 32))
    }

    @Test
    fun clampOffset_limitsPanSoCropStaysInsideImage() {
        val fitScale = 1f
        val userScale = 1f
        val clamped = AvatarCropUtils.clampOffset(
            offset = Offset(10_000f, -10_000f),
            userScale = userScale,
            fitScale = fitScale,
            bitmapWidth = 1000,
            bitmapHeight = 1000,
            containerWidth = 1000f,
            containerHeight = 1000f,
            cropDiameter = 500f,
        )
        assertEquals(250f, clamped.x, 0.01f)
        assertEquals(-250f, clamped.y, 0.01f)
    }

    @Test
    fun applyExifOrientation_normal_returnsSameBitmap() {
        val source = Bitmap.createBitmap(20, 10, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.GREEN)
        }
        val result = AvatarCropUtils.applyExifOrientation(source, ExifInterface.ORIENTATION_NORMAL)
        assertSame(source, result)
    }

    @Test
    fun applyExifOrientation_rotate90_swapsWidthAndHeight() {
        val source = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.GREEN)
        }
        val rotated = AvatarCropUtils.applyExifOrientation(source, ExifInterface.ORIENTATION_ROTATE_90)
        assertEquals(100, rotated.width)
        assertEquals(200, rotated.height)
    }

    @Test
    fun cropSquare_verticalPan_isPreserved() {
        val source = Bitmap.createBitmap(800, 800, Bitmap.Config.ARGB_8888).apply {
            for (y in 0 until 400) {
                for (x in 0 until width) setPixel(x, y, Color.YELLOW)
            }
            for (y in 400 until height) {
                for (x in 0 until width) setPixel(x, y, Color.MAGENTA)
            }
        }
        val container = 900f
        val cropDiameter = 600f
        val fitScale = AvatarCropUtils.fitScale(source.width, source.height, container, container)
        val userScale = AvatarCropUtils.minUserScale(cropDiameter, source.width, source.height, fitScale)
        val maxPan = AvatarCropUtils.clampOffset(
            offset = Offset(0f, 10_000f),
            userScale = userScale,
            fitScale = fitScale,
            bitmapWidth = source.width,
            bitmapHeight = source.height,
            containerWidth = container,
            containerHeight = container,
            cropDiameter = cropDiameter,
        ).y

        val pannedDown = AvatarCropUtils.cropSquare(
            source = source,
            containerWidth = container,
            containerHeight = container,
            cropDiameter = cropDiameter,
            fitScale = fitScale,
            userScale = userScale,
            offset = Offset(0f, maxPan),
            outputSize = 48,
        )
        val pannedUp = AvatarCropUtils.cropSquare(
            source = source,
            containerWidth = container,
            containerHeight = container,
            cropDiameter = cropDiameter,
            fitScale = fitScale,
            userScale = userScale,
            offset = Offset(0f, -maxPan),
            outputSize = 48,
        )

        assertEquals(Color.YELLOW, pannedDown.getPixel(24, 24))
        assertEquals(Color.MAGENTA, pannedUp.getPixel(24, 24))
    }

    @Test
    fun minUserScale_wideImage_zoomsEnoughToCoverCrop() {
        val minScale = AvatarCropUtils.minUserScale(
            cropDiameter = 720f,
            bitmapWidth = 2000,
            bitmapHeight = 500,
            fitScale = 0.5f,
        )
        assertTrue(minScale >= 2.8f)
    }
}
