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
import kotlin.math.roundToInt

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
     * Crops the on-screen circular window into a square bitmap using the same
     * transform as [AvatarCropScreen] (fitScale × userScale + pan offset).
     * Keeps a true square extract so framing from pan/zoom is not stretched.
     */
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
        val totalScale = (fitScale * userScale).coerceAtLeast(1e-6f)
        val centerX = containerWidth / 2f
        val centerY = containerHeight / 2f
        val halfCrop = cropDiameter.coerceAtLeast(1f) / 2f

        val imageCenterX = centerX + offset.x
        val imageCenterY = centerY + offset.y

        fun screenToBitmapX(x: Float): Float =
            source.width / 2f + (x - imageCenterX) / totalScale

        fun screenToBitmapY(y: Float): Float =
            source.height / 2f + (y - imageCenterY) / totalScale

        val rawLeft = screenToBitmapX(centerX - halfCrop)
        val rawTop = screenToBitmapY(centerY - halfCrop)
        val rawRight = screenToBitmapX(centerX + halfCrop)
        val rawBottom = screenToBitmapY(centerY + halfCrop)

        val cropCenterX = (rawLeft + rawRight) / 2f
        val cropCenterY = (rawTop + rawBottom) / 2f
        val idealSize = ((rawRight - rawLeft) + (rawBottom - rawTop)) / 2f

        var size = idealSize.roundToInt().coerceAtLeast(1)
        size = min(size, min(source.width, source.height))

        var left = (cropCenterX - size / 2f).roundToInt()
        var top = (cropCenterY - size / 2f).roundToInt()
        left = left.coerceIn(0, source.width - size)
        top = top.coerceIn(0, source.height - size)

        val cropped = Bitmap.createBitmap(source, left, top, size, size)
        return if (size == outputSize) {
            cropped
        } else {
            cropped.scale(outputSize, outputSize).also {
                if (it !== cropped) cropped.recycle()
            }
        }
    }

    internal fun applyExifOrientation(jpegBytes: ByteArray, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            ExifInterface(ByteArrayInputStream(jpegBytes))
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
            if (it !== bitmap) bitmap.recycle()
        }
    }
}
