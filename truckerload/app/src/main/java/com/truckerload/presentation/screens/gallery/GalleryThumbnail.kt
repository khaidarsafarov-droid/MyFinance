package com.truckerload.presentation.screens.gallery

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.truckerload.presentation.utils.decodeSampledBitmap
import java.io.File
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Downsampled photo and first-page scan thumbnails.
 * The cache keeps scroll-back from decoding the same JPEG or PDF again.
 */
private val galleryThumbCache = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount.coerceAtLeast(1)
}

internal fun decodeGalleryThumbnail(path: String, maxEdgePx: Int): Bitmap? {
    if (path.isBlank() || maxEdgePx <= 0) return null
    val key = "$maxEdgePx|$path"
    galleryThumbCache.get(key)?.let { cached ->
        if (!cached.isRecycled) return cached
    }
    val file = File(path)
    if (!file.isFile) return null
    val bitmap = runCatching {
        if (file.extension.equals("pdf", ignoreCase = true)) {
            decodePdfThumbnail(file, maxEdgePx)
        } else {
            decodeSampledBitmap(path, maxEdgePx)
        }
    }.getOrNull() ?: return null
    galleryThumbCache.put(key, bitmap)
    return bitmap
}

private fun decodePdfThumbnail(file: File, maxEdgePx: Int): Bitmap? {
    val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    return try {
        val renderer = PdfRenderer(descriptor)
        try {
            if (renderer.pageCount <= 0) return null
            val page = renderer.openPage(0)
            try {
                val longEdge = max(page.width, page.height).coerceAtLeast(1)
                val scale = maxEdgePx.toFloat() / longEdge.toFloat()
                val width = (page.width * scale).toInt().coerceAtLeast(1)
                val height = (page.height * scale).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            } finally {
                page.close()
            }
        } finally {
            renderer.close()
        }
    } finally {
        descriptor.close()
    }
}

@Composable
fun rememberGalleryThumbnail(path: String, maxEdgePx: Int): ImageBitmap? {
    val state = produceState<ImageBitmap?>(initialValue = null, path, maxEdgePx) {
        value = withContext(Dispatchers.IO) {
            decodeGalleryThumbnail(path, maxEdgePx)?.asImageBitmap()
        }
    }
    return state.value
}
