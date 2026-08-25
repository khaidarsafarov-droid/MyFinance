package com.truckerload.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.truckerload.domain.ingest.DocumentBytesDecoder
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/**
 * Any picked file → plain text for [com.truckerload.domain.parser.MessageParseService].
 * Text / JSON / CSV / HTML / DOCX / XLSX decode directly, digital PDFs use their text
 * layer, and only scans and photos fall through to OCR.
 */
class LoadDocumentTextExtractor(context: Context) {

    private val appContext = context.applicationContext

    suspend fun extract(uri: Uri, mimeType: String? = null): Result<String> = runCatching {
        val mime = (mimeType ?: appContext.contentResolver.getType(uri)).orEmpty()
        val name = displayName(uri)
        val bytes = readBytes(uri)
        val isPdf = DocumentBytesDecoder.isPdf(bytes, name, mime)
        val isImage = !isPdf && DocumentBytesDecoder.isImage(bytes, name, mime)

        if (!isPdf && !isImage) {
            DocumentBytesDecoder.decode(bytes, name, mime)
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { return@runCatching it }
        }
        if (isPdf) {
            val layer = runCatching { PdfTextLayerExtractor.extract(bytes) }.getOrDefault("")
            if (layer.length >= MIN_PDF_TEXT) return@runCatching layer
        }
        val text = if (isPdf) extractPdf(uri) else extractImage(uri)
        text.trim().ifBlank { error("empty_ocr") }
    }

    /** Display name drives extension-based decoding for content:// URIs. */
    fun displayName(uri: Uri): String {
        val fromProvider = runCatching {
            appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst() && cursor.columnCount > 0) cursor.getString(0) else null
                }
        }.getOrNull()
        return fromProvider ?: uri.lastPathSegment?.substringAfterLast('/').orEmpty()
    }

    private fun readBytes(uri: Uri): ByteArray {
        val stream = appContext.contentResolver.openInputStream(uri) ?: return ByteArray(0)
        return stream.use { input ->
            val buffer = ByteArray(READ_CHUNK)
            val out = java.io.ByteArrayOutputStream()
            var total = 0
            while (total < MAX_READ_BYTES) {
                val read = input.read(buffer)
                if (read <= 0) break
                out.write(buffer, 0, read)
                total += read
            }
            out.toByteArray()
        }
    }

    private suspend fun extractImage(uri: Uri): String {
        val ocr = OCRService(appContext)
        return try {
            ocr.recognizeFromUri(appContext, uri).text
        } finally {
            ocr.close()
        }
    }

    private suspend fun extractPdf(uri: Uri): String {
        val pfd = openDescriptor(uri) ?: error("pdf_open_failed")
        val ocr = OCRService(appContext)
        val pageFiles = mutableListOf<File>()
        // FIX: PdfRenderer owns the PFD and closes it; only close PFD if construction fails
        val renderer = try {
            PdfRenderer(pfd)
        } catch (t: Throwable) {
            runCatching { pfd.close() }
            ocr.close()
            throw t
        }
        return try {
            renderer.use { pdf ->
                val pageCount = pdf.pageCount.coerceAtMost(MAX_PDF_PAGES)
                val parts = ArrayList<String>(pageCount)
                for (index in 0 until pageCount) {
                    pdf.openPage(index).use { page ->
                        val (width, height, matrix) = scaledPdfRender(page.width, page.height)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val file = File(appContext.cacheDir, "load_pdf_page_$index.jpg")
                        FileOutputStream(file).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                        }
                        bitmap.recycle()
                        pageFiles += file
                        val pageText = ocr.recognizeFromUri(appContext, file.toUri()).text
                        if (pageText.isNotBlank()) parts += pageText
                    }
                }
                parts.joinToString("\n\n")
            }
        } finally {
            ocr.close()
            pageFiles.forEach { it.delete() }
        }
    }

    /** Downscales huge PDF pages so ARGB bitmaps stay under [MAX_PDF_EDGE_PX]. */
    private fun scaledPdfRender(pageWidth: Int, pageHeight: Int): Triple<Int, Int, Matrix?> {
        val srcW = pageWidth.coerceAtLeast(1)
        val srcH = pageHeight.coerceAtLeast(1)
        val longest = max(srcW, srcH)
        if (longest <= MAX_PDF_EDGE_PX) return Triple(srcW, srcH, null)
        val scale = MAX_PDF_EDGE_PX.toFloat() / longest.toFloat()
        val width = (srcW * scale).toInt().coerceAtLeast(1)
        val height = (srcH * scale).toInt().coerceAtLeast(1)
        return Triple(width, height, Matrix().apply { setScale(scale, scale) })
    }

    private fun openDescriptor(uri: Uri): ParcelFileDescriptor? {
        appContext.contentResolver.openFileDescriptor(uri, "r")?.let { return it }
        val tmp = File(appContext.cacheDir, "load_import.pdf")
        appContext.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tmp).use { output -> input.copyTo(output) }
        } ?: return null
        return ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    companion object {
        private const val MAX_PDF_PAGES = 8
        // FIX: full-page ARGB bitmaps on large PDFs can OOM on tablets
        private const val MAX_PDF_EDGE_PX = 2048
        private const val MIN_PDF_TEXT = 40
        private const val READ_CHUNK = 64 * 1024
        private const val MAX_READ_BYTES = 32 * 1024 * 1024
    }
}
