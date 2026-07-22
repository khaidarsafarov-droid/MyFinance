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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PDFGenerator(private val context: Context) {

    private val scansDir: File
        get() {
            val dir = File(context.getExternalFilesDir(null), "scans")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    fun buildScanFileName(timestamp: Long = System.currentTimeMillis()): String =
        "scan_${scanTimestampFormat.format(Date(timestamp))}.pdf"

    fun saveScanFromResult(result: GmsDocumentScanningResult, timestamp: Long = System.currentTimeMillis()): SavedScanFile {
        val pdfUri = result.pdf?.uri
        if (pdfUri != null) {
            val file = copyUriToScansDir(pdfUri, buildScanFileName(timestamp))
            return SavedScanFile(
                file = file,
                pageCount = result.pdf?.pageCount ?: result.pages?.size ?: 1,
            )
        }
        val pages = result.pages.orEmpty()
        require(pages.isNotEmpty()) { "No scan pages returned" }
        val file = createPdfFromPageUris(
            pageUris = pages.mapNotNull { it.imageUri },
            fileName = buildScanFileName(timestamp),
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

    fun mergePdfFiles(sources: List<File>, fileName: String = buildScanFileName()): File {
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
        private val scanTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

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
