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
    fun cropSquare_withHorizontalOffset_preservesFramingTowardOffsetSide() {
        // Left half green, right half blue — zoom + pan right should keep only green in frame.
        val source = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888).apply {
            for (x in 0 until width) {
                val color = if (x < width / 2) Color.GREEN else Color.BLUE
                for (y in 0 until height) {
                    setPixel(x, y, color)
                }
            }
        }
        val container = 1080f
        val cropDiameter = 720f
        val fitScale = AvatarCropUtils.fitScale(source.width, source.height, container, container)
        val minScale = AvatarCropUtils.minUserScale(cropDiameter, source.width, source.height, fitScale)
        val userScale = minScale * 2f
        val offset = AvatarCropUtils.clampOffset(
            offset = Offset(500f, 0f),
            userScale = userScale,
            fitScale = fitScale,
            bitmapWidth = source.width,
            bitmapHeight = source.height,
            containerWidth = container,
            containerHeight = container,
            cropDiameter = cropDiameter,
        )

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
        val panned = AvatarCropUtils.cropSquare(
            source = source,
            containerWidth = container,
            containerHeight = container,
            cropDiameter = cropDiameter,
            fitScale = fitScale,
            userScale = userScale,
            offset = offset,
            outputSize = 64,
        )

        assertNotEquals(Offset.Zero, offset)
        assertEquals(Color.GREEN, centered.getPixel(0, 32))
        assertEquals(Color.BLUE, centered.getPixel(63, 32))
        assertEquals(Color.GREEN, panned.getPixel(0, 32))
        assertEquals(Color.GREEN, panned.getPixel(32, 32))
        assertEquals(Color.GREEN, panned.getPixel(63, 32))
    }

    @Test
    fun cropSquare_withVerticalOffset_preservesFramingTowardOffsetSide() {
        val source = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888).apply {
            for (y in 0 until height) {
                val color = if (y < height / 2) Color.YELLOW else Color.MAGENTA
                for (x in 0 until width) {
                    setPixel(x, y, color)
                }
            }
        }
        val container = 1080f
        val cropDiameter = 720f
        val fitScale = AvatarCropUtils.fitScale(source.width, source.height, container, container)
        val minScale = AvatarCropUtils.minUserScale(cropDiameter, source.width, source.height, fitScale)
        val userScale = minScale * 2f
        val offset = AvatarCropUtils.clampOffset(
            offset = Offset(0f, 500f),
            userScale = userScale,
            fitScale = fitScale,
            bitmapWidth = source.width,
            bitmapHeight = source.height,
            containerWidth = container,
            containerHeight = container,
            cropDiameter = cropDiameter,
        )

        val panned = AvatarCropUtils.cropSquare(
            source = source,
            containerWidth = container,
            containerHeight = container,
            cropDiameter = cropDiameter,
            fitScale = fitScale,
            userScale = userScale,
            offset = offset,
            outputSize = 64,
        )

        assertNotEquals(Offset.Zero, offset)
        assertEquals(Color.YELLOW, panned.getPixel(32, 0))
        assertEquals(Color.YELLOW, panned.getPixel(32, 32))
        assertEquals(Color.YELLOW, panned.getPixel(32, 63))
    }

    @Test
    fun cropSquare_zoomedAndPanned_matchesBitmapWindowCenterColor() {
        val source = Bitmap.createBitmap(1200, 800, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.BLACK)
            for (x in 850 until 1150) {
                for (y in 300 until 500) {
                    setPixel(x, y, Color.CYAN)
                }
            }
        }
        val containerW = 1080f
        val containerH = 1600f
        val cropDiameter = minOf(containerW, containerH) * 0.72f
        val fitScale = AvatarCropUtils.fitScale(source.width, source.height, containerW, containerH)
        val minScale = AvatarCropUtils.minUserScale(cropDiameter, source.width, source.height, fitScale)
        val userScale = minScale * 1.6f
        val totalScale = fitScale * userScale
        val desired = Offset(
            x = (source.width / 2f - 1000f) * totalScale,
            y = (source.height / 2f - 400f) * totalScale,
        )
        val offset = AvatarCropUtils.clampOffset(
            offset = desired,
            userScale = userScale,
            fitScale = fitScale,
            bitmapWidth = source.width,
            bitmapHeight = source.height,
            containerWidth = containerW,
            containerHeight = containerH,
            cropDiameter = cropDiameter,
        )

        val window = AvatarCropUtils.cropWindowInBitmap(
            bitmapWidth = source.width,
            bitmapHeight = source.height,
            containerWidth = containerW,
            containerHeight = containerH,
            cropDiameter = cropDiameter,
            fitScale = fitScale,
            userScale = userScale,
            offset = offset,
        )
        val midX = (window[0] + window[2]) / 2
        val midY = (window[1] + window[3]) / 2
        assertTrue("expected cyan marker in crop window, mid=($midX,$midY) offset=$offset", midX in 850 until 1150)
        assertTrue("expected cyan marker Y in crop window, mid=($midX,$midY)", midY in 300 until 500)

        val cropped = AvatarCropUtils.cropSquare(
            source = source,
            containerWidth = containerW,
            containerHeight = containerH,
            cropDiameter = cropDiameter,
            fitScale = fitScale,
            userScale = userScale,
            offset = offset,
            outputSize = 128,
        )
        assertEquals(128, cropped.width)
        assertEquals(128, cropped.height)
        val centerColor = cropped.getPixel(64, 64)
        assertEquals(
            "center pixel should be cyan, was #${Integer.toHexString(centerColor)} window=$window offset=$offset",
            Color.CYAN,
            centerColor,
        )
    }

    @Test
    fun clampOffset_keepsCropInsideBitmapBounds() {
        val bw = 2000
        val bh = 1000
        val container = 1080f
        val cropDiameter = 720f
        val fitScale = AvatarCropUtils.fitScale(bw, bh, container, container)
        val userScale = AvatarCropUtils.minUserScale(cropDiameter, bw, bh, fitScale)
        val clamped = AvatarCropUtils.clampOffset(
            offset = Offset(10_000f, -10_000f),
            userScale = userScale,
            fitScale = fitScale,
            bitmapWidth = bw,
            bitmapHeight = bh,
            containerWidth = container,
            containerHeight = container,
            cropDiameter = cropDiameter,
        )
        val window = AvatarCropUtils.cropWindowInBitmap(
            bitmapWidth = bw,
            bitmapHeight = bh,
            containerWidth = container,
            containerHeight = container,
            cropDiameter = cropDiameter,
            fitScale = fitScale,
            userScale = userScale,
            offset = clamped,
        )
        assertTrue(window[0] >= 0)
        assertTrue(window[1] >= 0)
        assertTrue(window[2] <= bw)
        assertTrue(window[3] <= bh)
        assertEquals(window[2] - window[0], window[3] - window[1])
    }
}
