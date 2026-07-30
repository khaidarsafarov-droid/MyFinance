package com.truckerload.presentation.screens.social

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
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

    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

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
        return applyExifOrientation(decoded, bytes)
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
        if (containerWidth <= 0f || containerHeight <= 0f) return 1f
        return min(containerWidth / bitmapWidth, containerHeight / bitmapHeight)
    }

    fun minUserScale(cropDiameter: Float, bitmapWidth: Int, bitmapHeight: Int, fitScale: Float): Float {
        val displayWidth = bitmapWidth * fitScale
        val displayHeight = bitmapHeight * fitScale
        if (displayWidth <= 0f || displayHeight <= 0f || cropDiameter <= 0f) return 1f
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
     * Crops the square that matches the on-screen circular frame after [fitScale],
     * [userScale], and [offset] are applied around the container center.
     *
     * Uses a Canvas/Matrix draw so the saved pixels match the preview framing
     * even when the crop window sits on a bitmap edge (no non-square stretch).
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
        require(outputSize > 0) { "outputSize must be positive" }
        if (cropDiameter <= 0f || fitScale <= 0f || userScale <= 0f) {
            return source.scale(outputSize, outputSize)
        }

        val totalScale = fitScale * userScale
        val halfCrop = cropDiameter / 2f
        val outScale = outputSize / cropDiameter

        // screen = containerCenter + offset + (bitmap - bitmapCenter) * totalScale
        // Invert that mapping onto the output canvas whose (0,0) is the crop top-left.
        val matrix = Matrix().apply {
            setTranslate(-source.width / 2f, -source.height / 2f)
            postScale(totalScale, totalScale)
            postTranslate(halfCrop + offset.x, halfCrop + offset.y)
            postScale(outScale, outScale)
        }

        val output = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
        Canvas(output).drawBitmap(source, matrix, bitmapPaint)
        return output
    }

    /**
     * Bitmap-space axis-aligned crop window for the circular frame. Exposed for tests.
     */
    fun cropWindowInBitmap(
        bitmapWidth: Int,
        bitmapHeight: Int,
        containerWidth: Float,
        containerHeight: Float,
        cropDiameter: Float,
        fitScale: Float,
        userScale: Float,
        offset: Offset,
    ): IntArray {
        val centerX = containerWidth / 2f
        val centerY = containerHeight / 2f
        val halfCrop = cropDiameter / 2f
        val totalScale = fitScale * userScale

        fun screenToBitmap(x: Float, y: Float): Pair<Float, Float> {
            val imageCenterX = centerX + offset.x
            val imageCenterY = centerY + offset.y
            val bitmapX = bitmapWidth / 2f + (x - imageCenterX) / totalScale
            val bitmapY = bitmapHeight / 2f + (y - imageCenterY) / totalScale
            return bitmapX to bitmapY
        }

        val (rawLeft, rawTop) = screenToBitmap(centerX - halfCrop, centerY - halfCrop)
        val (rawRight, rawBottom) = screenToBitmap(centerX + halfCrop, centerY + halfCrop)

        val leftF = min(rawLeft, rawRight)
        val topF = min(rawTop, rawBottom)
        val rightF = max(rawLeft, rawRight)
        val bottomF = max(rawTop, rawBottom)
        // Keep a square window; clamp as a unit so framing does not skew at edges.
        val side = min(rightF - leftF, bottomF - topF)
        val left = leftF.roundToInt().coerceIn(0, (bitmapWidth - 1).coerceAtLeast(0))
        val top = topF.roundToInt().coerceIn(0, (bitmapHeight - 1).coerceAtLeast(0))
        val maxSide = min(bitmapWidth - left, bitmapHeight - top).coerceAtLeast(1)
        val size = side.roundToInt().coerceIn(1, maxSide)
        return intArrayOf(left, top, left + size, top + size)
    }

    private fun applyExifOrientation(source: Bitmap, jpegBytes: ByteArray): Bitmap {
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
            else -> return source
        }

        return runCatching {
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true).also {
                if (it !== source) source.recycle()
            }
        }.getOrDefault(source)
    }
}
