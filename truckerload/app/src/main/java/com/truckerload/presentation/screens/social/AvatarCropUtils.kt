package com.truckerload.presentation.screens.social

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.ui.geometry.Offset
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import kotlin.math.max
import kotlin.math.min

internal object AvatarCropUtils {
    private const val MAX_SOURCE_DIMENSION = 2048
    const val OUTPUT_SIZE = 512

    fun decodeSampledBitmap(stream: java.io.InputStream, maxDimension: Int = MAX_SOURCE_DIMENSION): Bitmap? {
        val bytes = stream.readBytes()
        if (bytes.isEmpty()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val largest = max(bounds.outWidth, bounds.outHeight)
        val sampleSize = generateSequence(1) { it * 2 }
            .takeWhile { largest / it > maxDimension }
            .lastOrNull()?.let { it * 2 } ?: 1
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return null
        return applyExifOrientation(bytes, decoded)
    }

    /**
     * Applies JPEG EXIF orientation so gallery/camera photos keep upright framing in the cropper.
     */
    fun applyExifOrientation(jpegBytes: ByteArray, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            ExifInterface(ByteArrayInputStream(jpegBytes))
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
            if (it !== bitmap) bitmap.recycle()
        }
    }

    fun prepareBitmapForCrop(source: Bitmap): Bitmap {
        val largest = max(source.width, source.height)
        if (largest <= MAX_SOURCE_DIMENSION) return source
        val scale = MAX_SOURCE_DIMENSION.toFloat() / largest
        return source.scale(
            max(1, (source.width * scale).toInt()),
            max(1, (source.height * scale).toInt()),
        )
    }

    fun fitScale(bitmapWidth: Int, bitmapHeight: Int, containerWidth: Float, containerHeight: Float): Float {
        if (bitmapWidth <= 0 || bitmapHeight <= 0) return 1f
        return min(containerWidth / bitmapWidth, containerHeight / bitmapHeight)
    }

    fun minUserScale(cropDiameter: Float, bitmapWidth: Int, bitmapHeight: Int, fitScale: Float): Float {
        val displayWidth = bitmapWidth * fitScale
        val displayHeight = bitmapHeight * fitScale
        if (displayWidth <= 0f || displayHeight <= 0f) return 1f
        return max(cropDiameter / displayWidth, cropDiameter / displayHeight).coerceAtLeast(1f)
    }

    fun clampOffset(
        offset: Offset,
        userScale: Float,
        fitScale: Float,
        bitmapWidth: Int,
        bitmapHeight: Int,
        containerWidth: Float,
        containerHeight: Float,
        cropDiameter: Float,
    ): Offset {
        val totalScale = fitScale * userScale
        val displayWidth = bitmapWidth * totalScale
        val displayHeight = bitmapHeight * totalScale
        val halfCrop = cropDiameter / 2f
        val maxX = displayWidth / 2f - halfCrop
        val minX = halfCrop - displayWidth / 2f
        val maxY = displayHeight / 2f - halfCrop
        val minY = halfCrop - displayHeight / 2f
        return Offset(
            x = if (minX <= maxX) offset.x.coerceIn(minX, maxX) else 0f,
            y = if (minY <= maxY) offset.y.coerceIn(minY, maxY) else 0f,
        )
    }

    /**
     * After the crop viewport size changes, keep the user's framing by raising scale if needed
     * and re-clamping the pan offset — never snap back to center.
     */
    fun preserveTransformAfterLayoutChange(
        previousUserScale: Float,
        previousOffset: Offset,
        fitScale: Float,
        minScale: Float,
        bitmapWidth: Int,
        bitmapHeight: Int,
        containerWidth: Float,
        containerHeight: Float,
        cropDiameter: Float,
    ): Pair<Float, Offset> {
        val userScale = previousUserScale.coerceIn(minScale, minScale * 4f)
        val offset = clampOffset(
            offset = previousOffset,
            userScale = userScale,
            fitScale = fitScale,
            bitmapWidth = bitmapWidth,
            bitmapHeight = bitmapHeight,
            containerWidth = containerWidth,
            containerHeight = containerHeight,
            cropDiameter = cropDiameter,
        )
        return userScale to offset
    }

    fun cropSquare(
        source: Bitmap,
        containerWidth: Float,
        containerHeight: Float,
        cropDiameter: Float,
        fitScale: Float,
        userScale: Float,
        offset: Offset,
        outputSize: Int = OUTPUT_SIZE,
    ): Bitmap {
        val centerX = containerWidth / 2f
        val centerY = containerHeight / 2f
        val halfCrop = cropDiameter / 2f
        val totalScale = fitScale * userScale

        fun screenToBitmap(x: Float, y: Float): Pair<Float, Float> {
            val imageCenterX = centerX + offset.x
            val imageCenterY = centerY + offset.y
            val bitmapX = source.width / 2f + (x - imageCenterX) / totalScale
            val bitmapY = source.height / 2f + (y - imageCenterY) / totalScale
            return bitmapX to bitmapY
        }

        val (rawLeft, rawTop) = screenToBitmap(centerX - halfCrop, centerY - halfCrop)
        val (rawRight, rawBottom) = screenToBitmap(centerX + halfCrop, centerY + halfCrop)

        val left = min(rawLeft, rawRight).toInt().coerceIn(0, source.width - 1)
        val top = min(rawTop, rawBottom).toInt().coerceIn(0, source.height - 1)
        val right = max(rawLeft, rawRight).toInt().coerceIn(left + 1, source.width)
        val bottom = max(rawTop, rawBottom).toInt().coerceIn(top + 1, source.height)

        val width = right - left
        val height = bottom - top
        val cropped = Bitmap.createBitmap(source, left, top, width, height)
        return if (width == outputSize && height == outputSize) {
            cropped
        } else {
            cropped.scale(outputSize, outputSize).also {
                if (it !== cropped) cropped.recycle()
            }
        }
    }
}
