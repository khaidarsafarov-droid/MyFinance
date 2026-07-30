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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

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
    fun cropSquare_nonZeroOffset_keepsUserFraming() {
        // Left half red, right half blue — pan so crop is on the blue side.
        val source = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888).apply {
            for (x in 0 until width) {
                for (y in 0 until height) {
                    setPixel(x, y, if (x < 500) Color.RED else Color.BLUE)
                }
            }
        }
        val container = 1000f
        val cropDiameter = 400f
        val fitScale = 1f
        val userScale = 1f
        // Move image left so the crop circle covers the right (blue) half.
        val offset = Offset(x = -300f, y = 0f)

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

        val center = cropped.getPixel(50, 50)
        assertEquals("Crop with leftward pan should sample the blue region", Color.BLUE, center)

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
        // Centered crop straddles the red/blue boundary — must differ from panned crop.
        assertNotEquals(cropped.getPixel(0, 50), centered.getPixel(0, 50))
    }

    @Test
    fun clampOffset_preservesNonZeroPanWithinBounds() {
        val clamped = AvatarCropUtils.clampOffset(
            offset = Offset(120f, -80f),
            userScale = 2f,
            fitScale = 1f,
            bitmapWidth = 1000,
            bitmapHeight = 1000,
            containerWidth = 1000f,
            containerHeight = 1000f,
            cropDiameter = 400f,
        )
        assertEquals(120f, clamped.x, 0.01f)
        assertEquals(-80f, clamped.y, 0.01f)
    }

    @Test
    fun applyExifOrientation_rotate90_swapsDimensionsAndPreservesPixels() {
        val source = Bitmap.createBitmap(40, 20, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.BLACK)
            setPixel(39, 0, Color.RED) // top-right corner
        }
        val jpegBytes = jpegWithOrientation(source, ExifInterface.ORIENTATION_ROTATE_90)
        val decoded = BitmapFactoryCompat.decode(jpegBytes)!!
        val oriented = AvatarCropUtils.applyExifOrientation(jpegBytes, decoded)

        assertEquals(20, oriented.width)
        assertEquals(40, oriented.height)
        // After 90° CW, original top-right (39,0) lands near bottom-right.
        assertEquals(Color.RED, oriented.getPixel(oriented.width - 1, oriented.height - 1))
    }

    @Test
    fun decodeSampledBitmap_appliesExifOrientation() {
        val source = Bitmap.createBitmap(60, 30, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.GREEN)
            setPixel(59, 0, Color.YELLOW)
        }
        val jpegBytes = jpegWithOrientation(source, ExifInterface.ORIENTATION_ROTATE_90)
        val decoded = AvatarCropUtils.decodeSampledBitmap(ByteArrayInputStream(jpegBytes))!!

        assertEquals(30, decoded.width)
        assertEquals(60, decoded.height)
        assertEquals(Color.YELLOW, decoded.getPixel(decoded.width - 1, decoded.height - 1))
    }

    private fun jpegWithOrientation(bitmap: Bitmap, orientation: Int): ByteArray {
        val raw = ByteArrayOutputStream().use { out ->
            assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out))
            out.toByteArray()
        }
        val temp = File.createTempFile("avatar_exif_", ".jpg")
        try {
            temp.writeBytes(raw)
            ExifInterface(temp.absolutePath).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
                saveAttributes()
            }
            return temp.readBytes()
        } finally {
            temp.delete()
        }
    }

    /** Thin wrapper so tests don't need android.graphics.BitmapFactory import conflicts. */
    private object BitmapFactoryCompat {
        fun decode(bytes: ByteArray): Bitmap? =
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}
