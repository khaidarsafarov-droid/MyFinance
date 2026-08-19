package com.truckerload.sync.telegram

import android.content.Context
import androidx.core.net.toUri
import com.truckerload.domain.ingest.DocumentBytesDecoder
import com.truckerload.utils.LoadDocumentTextExtractor
import com.truckerload.utils.PdfTextLayerExtractor
import java.io.File

/**
 * Any Telegram file → plain text: office/text codecs, PDF text layer, then OCR.
 */
class TelegramFileTextExtractor(private val context: Context) {

    suspend fun extract(
        bytes: ByteArray,
        fileName: String?,
        mimeType: String?,
    ): Result<String> = runCatching {
        val name = fileName.orEmpty()
        val mime = mimeType.orEmpty()
        DocumentBytesDecoder.decode(bytes, name, mime)?.trim()?.takeIf { it.isNotBlank() }
            ?: extractPdfOrImage(bytes, name, mime)
    }

    private suspend fun extractPdfOrImage(
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
    ): String {
        val isPdf = DocumentBytesDecoder.isPdf(bytes, fileName, mimeType)
        if (isPdf) {
            val layer = PdfTextLayerExtractor.extract(bytes)
            if (layer.length >= MIN_PDF_TEXT) return layer
        }
        val ext = when {
            isPdf -> "pdf"
            mimeType.contains("png") || fileName.endsWith(".png", true) -> "png"
            mimeType.contains("webp") -> "webp"
            else -> "jpg"
        }
        val file = File(context.cacheDir, "tg_file_${System.nanoTime()}.$ext")
        return try {
            file.writeBytes(bytes)
            val mime = when {
                isPdf -> "application/pdf"
                else -> mimeType.ifBlank { "image/jpeg" }
            }
            LoadDocumentTextExtractor(context).extract(file.toUri(), mime).getOrThrow()
        } finally {
            file.delete()
        }
    }

    companion object {
        private const val MIN_PDF_TEXT = 40
    }
}
