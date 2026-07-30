package com.truckerload.presentation.screens.social

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
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
    fun cropSquare_withPanOffset_preservesFramedRegion() {
        // Left half red, right half blue — panning left should crop more of the blue side.
        val source = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
        for (x in 0 until 400) {
            for (y in 0 until 400) {
                source.setPixel(x, y, if (x < 200) Color.RED else Color.BLUE)
            }
        }

        val container = 400f
        val cropDiameter = 200f
        val fitScale = 1f
        val userScale = 1f
        // Move image left so the crop circle covers more of the right (blue) half.
        val offset = Offset(-80f, 0f)

        val cropped = AvatarCropUtils.cropSquare(
            source = source,
            containerWidth = container,
            containerHeight = container,
            cropDiameter = cropDiameter,
            fitScale = fitScale,
            userScale = userScale,
            offset = offset,
            outputSize = 100,
        )

        assertEquals(100, cropped.width)
        assertEquals(100, cropped.height)

        val center = cropped.getPixel(50, 50)
        val leftEdge = cropped.getPixel(5, 50)
        val rightEdge = cropped.getPixel(95, 50)

        // With a leftward pan, the framed center and right should be blue-dominant.
        assertEquals("center should be blue after leftward pan", Color.BLUE, center)
        assertEquals("right edge should be blue", Color.BLUE, rightEdge)
        // Left edge of crop may still catch some red near the seam depending on framing,
        // but must not be an all-red centered crop (which would put red at center).
        assertTrue(
            "left edge should not be pure blue-only failure; got $leftEdge",
            leftEdge == Color.RED || leftEdge == Color.BLUE,
        )
    }

    @Test
    fun cropSquare_zoomedIn_preservesOffsetRegion() {
        val source = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.BLACK)
        }
        // Bright marker in the upper-left quadrant.
        for (x in 40 until 80) {
            for (y in 40 until 80) {
                source.setPixel(x, y, Color.GREEN)
            }
        }

        val container = 400f
        val cropDiameter = 100f
        val fitScale = 1f
        val userScale = 2f
        // Pan so the green marker sits at the crop center:
        // image center is at (200,200); marker center at (60,60).
        // On screen at scale 2: marker is at imageCenter + (60-200)*2 + offset = containerCenter
        // => offset = - (60-200)*2 = 280 when we want marker at container center... 
        // screenToBitmap: bitmap = center + (screen - (containerCenter + offset)) / totalScale
        // We want crop center (200,200) to map to bitmap (60,60):
        // 60 = 200 + (200 - (200 + offset.x)) / 2  => 60 = 200 - offset.x/2 => offset.x = 280
        val offset = Offset(280f, 280f)

        val cropped = AvatarCropUtils.cropSquare(
            source = source,
            containerWidth = container,
            containerHeight = container,
            cropDiameter = cropDiameter,
            fitScale = fitScale,
            userScale = userScale,
            offset = offset,
            outputSize = 50,
        )

        val center = cropped.getPixel(25, 25)
        assertEquals("zoomed+panned crop should hit the green marker", Color.GREEN, center)
    }

    @Test
    fun clampOffset_limitsPanSoCropStaysInsideImage() {
        val clamped = AvatarCropUtils.clampOffset(
            offset = Offset(10_000f, -10_000f),
            userScale = 1f,
            fitScale = 1f,
            bitmapWidth = 400,
            bitmapHeight = 400,
            containerWidth = 400f,
            containerHeight = 400f,
            cropDiameter = 200f,
        )
        // display 400, halfCrop 100 => max |offset| = 100
        assertEquals(100f, clamped.x, 0.01f)
        assertEquals(-100f, clamped.y, 0.01f)
    }

    @Test
    fun minUserScale_ensuresImageCoversCropCircle() {
        val minScale = AvatarCropUtils.minUserScale(
            cropDiameter = 300f,
            bitmapWidth = 200,
            bitmapHeight = 100,
            fitScale = 1f,
        )
        // Need scale so shorter edge (100) covers 300 => 3x
        assertEquals(3f, minScale, 0.01f)
    }
}
