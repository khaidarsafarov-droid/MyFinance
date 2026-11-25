package com.truckerload.utils

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import android.graphics.BitmapFactory
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class PDFGenerator(private val context: Context) {

    init {
        PdfBox.init(context)
    }

    private val scansDir: File
        get() = MediaStorage.scansDir(context)

    fun buildScanFileName(
        timestamp: Long = System.currentTimeMillis(),
        tripId: String? = null,
        loadDate: String? = null,
    ): String {
        if (!tripId.isNullOrBlank()) {
            return AttachmentNaming.buildFileName(tripId, loadDate.orEmpty(), timestamp, "pdf")
        }
        return "scan_${scanTimestampFormat.format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))}.pdf"
    }

    fun saveScanFromResult(
        result: GmsDocumentScanningResult,
        timestamp: Long = System.currentTimeMillis(),
        tripId: String? = null,
        loadDate: String? = null,
    ): SavedScanFile {
        val fileName = buildScanFileName(timestamp, tripId, loadDate)
        val pdfUri = result.pdf?.uri
        if (pdfUri != null) {
            val file = copyUriToScansDir(pdfUri, fileName)
            return SavedScanFile(
                file = file,
                pageCount = result.pdf?.pageCount ?: result.pages?.size ?: 1,
            )
        }
        val pages = result.pages.orEmpty()
        require(pages.isNotEmpty()) { "No scan pages returned" }
        val file = createPdfFromPageUris(
            pageUris = pages.mapNotNull { it.imageUri },
            fileName = fileName,
        )
        return SavedScanFile(file = file, pageCount = pages.size)
    }

    fun copyUriToScansDir(uri: Uri, fileName: String): File {
        val dest = File(scansDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(dest).use { output -> input.copyTo(output) }
        } ?: error("Cannot read scan URI")
        return dest
    }

    /** One scanned image per A4 page, scaled to fit inside the margins. */
    fun createPdfFromPageUris(pageUris: List<Uri>, fileName: String): File {
        val dest = File(scansDir, fileName)
        PDDocument().use { doc ->
            pageUris.forEach { uri ->
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val image = pageImage(doc, stream.readBytes()) ?: return@use
                    addImagePage(doc, image)
                }
            }
            if (doc.numberOfPages == 0) doc.addPage(PDPage(PDRectangle.A4))
            doc.save(dest)
        }
        return dest
    }

    private fun pageImage(doc: PDDocument, bytes: ByteArray): PDImageXObject? {
        val isJpeg = bytes.size > 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()
        if (isJpeg) {
            runCatching { return JPEGFactory.createFromByteArray(doc, bytes) }
        }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        return try {
            LosslessFactory.createFromImage(doc, bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    private fun addImagePage(doc: PDDocument, image: PDImageXObject) {
        val page = PDPage(PDRectangle.A4)
        doc.addPage(page)
        val box = page.mediaBox
        val maxW = box.width - PAGE_MARGIN * 2
        val maxH = box.height - PAGE_MARGIN * 2
        val scale = minOf(maxW / image.width, maxH / image.height)
        val w = image.width * scale
        val h = image.height * scale
        val x = PAGE_MARGIN + (maxW - w) / 2f
        val y = box.height - PAGE_MARGIN - h
        PDPageContentStream(doc, page).use { it.drawImage(image, x, y, w, h) }
    }

    fun mergePdfFiles(
        sources: List<File>,
        fileName: String = buildScanFileName(),
    ): File {
        val existing = sources.filter { it.exists() }
        require(existing.isNotEmpty()) { "No PDF files to merge" }
        if (existing.size == 1) return existing.first()
        val dest = File(scansDir, fileName)
        val merger = PDFMergerUtility()
        existing.forEach { merger.addSource(it) }
        merger.destinationFileName = dest.absolutePath
        merger.mergeDocuments(MemoryUsageSetting.setupMixed(MERGE_MEMORY_BYTES))
        return dest
    }

    fun saveToPublicDownloads(source: File): String? {
        val storageHelper = StorageHelper(context)
        return storageHelper.saveToPublicDownloads(
            fileName = source.name,
            relativeSubDir = BrandConstants.DOWNLOADS_FOLDER,
            mimeType = "application/pdf",
        ) { out ->
            source.inputStream().use { it.copyTo(out) }
        }?.displayPath
    }

    companion object {
        /** Same 0.5 inch margin the old layout used around scanned pages. */
        private const val PAGE_MARGIN = 36f
        private const val MERGE_MEMORY_BYTES = 32L * 1024 * 1024

        // FIX: thread-safe scan filename — shared SimpleDateFormat is unsafe under concurrent saves
        private val scanTimestampFormat: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.US)

        fun formatFileSize(bytes: Long): String {
            if (bytes < 1024) return "$bytes B"
            val kb = bytes / 1024.0
            if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
            val mb = kb / 1024.0
            return String.format(Locale.US, "%.1f MB", mb)
        }
    }
}

data class SavedScanFile(
    val file: File,
    val pageCount: Int,
)
