package com.truckerload.data.social

import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

object SocialMediaOptimizer {
    private const val MAX_IMAGE_DIMENSION = 1280
    private const val JPEG_QUALITY = 82

    fun compressImage(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val largest = max(width, height)
        if (largest <= MAX_IMAGE_DIMENSION) return source
        val scale = MAX_IMAGE_DIMENSION.toFloat() / largest
        val targetW = max(1, (width * scale).toInt())
        val targetH = max(1, (height * scale).toInt())
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }

    fun jpegQuality(): Int = JPEG_QUALITY

    fun compressImage(source: Bitmap, dest: File): File {
        val compressed = compressImage(source)
        FileOutputStream(dest).use { out ->
            compressed.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        return dest
    }

    fun clampVolume(volume: Float): Float = min(1f, max(0f, volume))
}
