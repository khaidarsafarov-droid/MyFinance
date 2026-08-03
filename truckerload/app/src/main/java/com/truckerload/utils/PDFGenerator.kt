package com.truckerload.utils

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class PDFGenerator(private val context: Context) {

    private val scansDir: File
        get() {
            val dir = File(context.getExternalFilesDir(null), "scans")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

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

    fun createPdfFromPageUris(pageUris: List<Uri>, fileName: String): File {
        val dest = File(scansDir, fileName)
        val writer = PdfWriter(dest)
        val pdfDoc = PdfDocument(writer)
        val document = Document(pdfDoc, PageSize.A4)
        pageUris.forEach { uri ->
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                val image = Image(ImageDataFactory.create(bytes))
                image.setAutoScale(true)
                document.add(image)
            }
        }
        document.close()
        return dest
    }

    fun mergePdfFiles(
        sources: List<File>,
        fileName: String = buildScanFileName(),
    ): File {
        val existing = sources.filter { it.exists() }
        require(existing.isNotEmpty()) { "No PDF files to merge" }
        if (existing.size == 1) return existing.first()
        val dest = File(scansDir, fileName)
        val writer = PdfWriter(dest)
        val merged = PdfDocument(writer)
        try {
            existing.forEach { source ->
                val reader = PdfReader(source)
                val srcDoc = PdfDocument(reader)
                srcDoc.copyPagesTo(1, srcDoc.numberOfPages, merged)
                srcDoc.close()
            }
        } finally {
            merged.close()
        }
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
