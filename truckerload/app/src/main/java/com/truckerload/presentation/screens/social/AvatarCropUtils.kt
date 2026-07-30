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
     * The crop window is forced square and clamped as a unit so edge clamping
     * cannot stretch/skew the framed subject.
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
        val window = cropWindowInBitmap(
            bitmapWidth = source.width,
            bitmapHeight = source.height,
            containerWidth = containerWidth,
            containerHeight = containerHeight,
            cropDiameter = cropDiameter,
            fitScale = fitScale,
            userScale = userScale,
            offset = offset,
        )
        val left = window[0]
        val top = window[1]
        val width = window[2] - left
        val height = window[3] - top
        val cropped = Bitmap.createBitmap(source, left, top, width, height)
        return if (width == outputSize && height == outputSize) {
            cropped
        } else {
            cropped.scale(outputSize, outputSize).also {
                if (it !== cropped) cropped.recycle()
            }
        }
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
        if (bitmapWidth <= 0 || bitmapHeight <= 0 || cropDiameter <= 0f || fitScale <= 0f || userScale <= 0f) {
            return intArrayOf(0, 0, 1, 1)
        }
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

        var leftF = min(rawLeft, rawRight)
        var topF = min(rawTop, rawBottom)
        var rightF = max(rawLeft, rawRight)
        var bottomF = max(rawTop, rawBottom)
        // Keep a square window; shift as a unit so framing does not skew at edges.
        val side = min(rightF - leftF, bottomF - topF).coerceAtLeast(1f)
        val midX = (leftF + rightF) / 2f
        val midY = (topF + bottomF) / 2f
        leftF = midX - side / 2f
        topF = midY - side / 2f
        rightF = leftF + side
        bottomF = topF + side

        if (leftF < 0f) {
            rightF -= leftF
            leftF = 0f
        }
        if (topF < 0f) {
            bottomF -= topF
            topF = 0f
        }
        if (rightF > bitmapWidth) {
            val overflow = rightF - bitmapWidth
            leftF -= overflow
            rightF = bitmapWidth.toFloat()
        }
        if (bottomF > bitmapHeight) {
            val overflow = bottomF - bitmapHeight
            topF -= overflow
            bottomF = bitmapHeight.toFloat()
        }
        leftF = leftF.coerceAtLeast(0f)
        topF = topF.coerceAtLeast(0f)

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
