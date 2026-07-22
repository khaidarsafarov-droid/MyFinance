package com.truckerload.presentation.utils

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/**
 * Decodes a bitmap off the main thread for Compose thumbnails,
 * sampling down so large camera JPEGs do not blow memory.
 */
@Composable
fun rememberDecodedBitmap(path: String, maxEdgePx: Int = 512): ImageBitmap? {
    val state = produceState<ImageBitmap?>(initialValue = null, path, maxEdgePx) {
        value = withContext(Dispatchers.IO) {
            decodeSampledBitmap(path, maxEdgePx)?.asImageBitmap()
        }
    }
    return state.value
}

internal fun decodeSampledBitmap(path: String, maxEdgePx: Int): android.graphics.Bitmap? {
    val file = File(path)
    if (!file.exists()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    val w = bounds.outWidth
    val h = bounds.outHeight
    if (w <= 0 || h <= 0) return null
    var sample = 1
    var halfW = w / 2
    var halfH = h / 2
    while (halfW / sample >= maxEdgePx && halfH / sample >= maxEdgePx) {
        sample *= 2
    }
    // Ensure the longer edge is near maxEdgePx even when aspect is extreme.
    while (max(w, h) / sample > maxEdgePx * 2) {
        sample *= 2
    }
    val opts = BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) }
    return BitmapFactory.decodeFile(file.absolutePath, opts)
}
