package com.truckerload.presentation.screens.social

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.exifinterface.media.ExifInterface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream

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
    fun cropSquare_withOffset_keepsPannedRegion() {
        val source = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
        // Left half green, right half blue.
        for (x in 0 until 400) {
            for (y in 0 until 400) {
                source.setPixel(x, y, if (x < 200) Color.GREEN else Color.BLUE)
            }
        }

        val container = 400f
        val cropDiameter = 200f
        val fitScale = AvatarCropUtils.fitScale(source.width, source.height, container, container)
        val userScale = AvatarCropUtils.minUserScale(cropDiameter, source.width, source.height, fitScale)
        // Negative X pans the image left → crop circle shows the right (blue) half.
        val pannedLeft = AvatarCropUtils.clampOffset(
            offset = Offset(-10_000f, 0f),
            userScale = userScale,
            fitScale = fitScale,
            bitmapWidth = source.width,
            bitmapHeight = source.height,
            containerWidth = container,
            containerHeight = container,
            cropDiameter = cropDiameter,
        )
        // Positive X pans the image right → crop circle shows the left (green) half.
        val pannedRight = AvatarCropUtils.clampOffset(
            offset = Offset(10_000f, 0f),
            userScale = userScale,
            fitScale = fitScale,
            bitmapWidth = source.width,
            bitmapHeight = source.height,
            containerWidth = container,
            containerHeight = container,
            cropDiameter = cropDiameter,
        )

        val intoBlue = AvatarCropUtils.cropSquare(
            source = source,
            containerWidth = container,
            containerHeight = container,
            cropDiameter = cropDiameter,
            fitScale = fitScale,
            userScale = userScale,
            offset = pannedLeft,
            outputSize = 64,
        )
        val intoGreen = AvatarCropUtils.cropSquare(
            source = source,
            containerWidth = container,
            containerHeight = container,
            cropDiameter = cropDiameter,
            fitScale = fitScale,
            userScale = userScale,
            offset = pannedRight,
            outputSize = 64,
        )

        assertEquals(Color.BLUE, intoBlue.getPixel(32, 32))
        assertEquals(Color.GREEN, intoGreen.getPixel(32, 32))
        assertTrue(pannedLeft.x < 0f)
        assertTrue(pannedRight.x > 0f)
    }

    @Test
    fun preserveTransformAfterLayoutChange_doesNotResetOffsetToZero() {
        val previousOffset = Offset(40f, -25f)
        val previousScale = 1.8f
        val (scale, offset) = AvatarCropUtils.preserveTransformAfterLayoutChange(
            previousUserScale = previousScale,
            previousOffset = previousOffset,
            fitScale = 1f,
            minScale = 1.2f,
            bitmapWidth = 800,
            bitmapHeight = 800,
            containerWidth = 1000f,
            containerHeight = 1000f,
            cropDiameter = 720f,
        )
        assertTrue(scale >= 1.2f)
        assertNotEquals(Offset.Zero, offset)
        assertEquals(previousOffset.x, offset.x, 0.01f)
        assertEquals(previousOffset.y, offset.y, 0.01f)
    }

    @Test
    fun applyExifOrientation_rotates90Clockwise() {
        val source = Bitmap.createBitmap(20, 10, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.RED)
            setPixel(0, 0, Color.GREEN)
        }
        val jpeg = ByteArrayOutputStream().use { out ->
            source.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.toByteArray()
        }
        // Bake EXIF orientation into a rewritten JPEG.
        val orientedBytes = ByteArrayOutputStream().use { out ->
            // Re-encode then stamp orientation via ExifInterface on a temp round-trip:
            // Robolectric ExifInterface can write attributes on a file; use Matrix path directly
            // by verifying applyExifOrientation reacts to a synthetic stream with TAG set via file.
            out.write(jpeg)
            out.toByteArray()
        }

        // When EXIF says rotate 90, green corner should move.
        // Build bytes with orientation by writing to a temp file ExifInterface can mutate.
        val tmp = java.io.File.createTempFile("avatar_exif", ".jpg")
        try {
            tmp.writeBytes(orientedBytes)
            ExifInterface(tmp.absolutePath).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
                saveAttributes()
            }
            val withExif = tmp.readBytes()
            val decoded = Bitmap.createBitmap(20, 10, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.RED)
                setPixel(0, 0, Color.GREEN)
            }
            val rotated = AvatarCropUtils.applyExifOrientation(withExif, decoded)
            assertEquals(10, rotated.width)
            assertEquals(20, rotated.height)
        } finally {
            tmp.delete()
        }
    }

    @Test
    fun applyExifOrientation_noopWhenNormal() {
        val source = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.MAGENTA)
        }
        val jpeg = ByteArrayOutputStream().use { out ->
            source.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.toByteArray()
        }
        val result = AvatarCropUtils.applyExifOrientation(jpeg, source)
        assertEquals(16, result.width)
        assertEquals(16, result.height)
        assertEquals(Color.MAGENTA, result.getPixel(0, 0))
    }
}
