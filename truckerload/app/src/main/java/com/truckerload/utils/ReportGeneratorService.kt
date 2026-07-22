package com.truckerload.utils

import android.content.Context
import androidx.core.content.FileProvider
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.graphics.toColorInt
import android.net.Uri
import com.truckerload.domain.model.Load
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates professional PDF reports from Truck Log data.
 * Style: accounting document, white background, black text, gray borders.
 *
 * PDF report headers/labels are English-only (Metric, Gross Revenue, Detailed Log, etc.).
 * UI language does not affect PDF column titles.
 */
class ReportGeneratorService(private val context: Context) {

    companion object {
        /** Sanitize period labels / trip-like tokens for safe PDF filenames. */
        fun sanitizeFileLabel(raw: String): String =
            raw.replace(Regex("[^\\p{L}0-9\\s-]"), "_")
                .replace(Regex("_+"), "_")
                .trim('_')
                .ifBlank { "report" }
    }

    private val pageWidth = 595
    private val pageHeight = 842
    private val margin = 50
    private val contentWidth = pageWidth - 2 * margin

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 18f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 12f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 10f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }

    private val linePaint = Paint().apply {
        color = Color.GRAY
        strokeWidth = 0.5f
        style = Paint.Style.STROKE
    }

    private val zebraPaint = Paint().apply {
        color = "#F5F5F5".toColorInt()
        style = Paint.Style.FILL
    }

    data class ReportParams(
        val periodLabel: String,
        val startDate: String,
        val endDate: String,
        val driverName: String?,
        val grossRevenue: Double,
        val totalMiles: Double,
        val avgRpm: Double,
        val totalLoads: Int,
        val loads: List<Load>
    )

    /**
     * Generates PDF and saves it to public Downloads/TruckLog/Reports.
     * Returns SaveResult (share URI + display path) or null.
     */
    suspend fun generatePdfAndSaveToStorage(params: ReportParams): StorageHelper.SaveResult? = withContext(Dispatchers.IO) {
        try {
            val safeLabel = sanitizeFileLabel(params.periodLabel)
            val fileName = "${BrandConstants.FILE_PREFIX}_Report_${safeLabel}_${System.currentTimeMillis()}.pdf"
            val storageHelper = StorageHelper(context)
            storageHelper.saveToPublicDownloads(fileName, "${BrandConstants.DOWNLOADS_FOLDER}/Reports", "application/pdf") { out ->
                generatePdfToStream(params, out)
            }
                ?: run {
                    val file = storageHelper.saveToAppStorage(fileName, "Reports") { generatePdfToStream(params, it) }
                    StorageHelper.SaveResult(storageHelper.getShareableUri(file), "${BrandConstants.DOWNLOADS_FOLDER}/Reports/$fileName")
                }
        } catch (e: Exception) {
            android.util.Log.e("ReportGenerator", "generatePdfAndSaveToStorage failed", e)
            null
        }
    }

    suspend fun generatePdf(params: ReportParams): Uri? = withContext(Dispatchers.IO) {
        try {
            val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                ?: File(context.getExternalFilesDir(null), "exports").also { it.mkdirs() }
            val safeLabel = sanitizeFileLabel(params.periodLabel)
            val fileName = "${BrandConstants.FILE_PREFIX}_Report_${safeLabel}_${System.currentTimeMillis()}.pdf"
            val file = File(dir, fileName)
            generatePdfToStream(params, FileOutputStream(file))
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            android.util.Log.e("ReportGenerator", "generatePdf failed", e)
            null
        }
    }

    private fun generatePdfToStream(params: ReportParams, out: java.io.OutputStream) {
        val doc = PdfDocument()
        try {
            var y = margin.toFloat()
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
                var page = doc.startPage(pageInfo)
                var canvas = page.canvas

                fun newPage() {
                    doc.finishPage(page)
                    val newPageNum = doc.pages.size + 1
                    page = doc.startPage(
                        PdfDocument.PageInfo.Builder(pageWidth, pageHeight, newPageNum).create()
                    )
                    canvas = page.canvas
                    y = margin.toFloat()
                }

                fun drawText(text: String, paint: Paint, x: Float = margin.toFloat()) {
                    canvas.drawText(text, x, y, paint)
                    y += paint.textSize + 4
                }

                // Header
                drawText(BrandConstants.DISPLAY_NAME, titlePaint)
                drawText(params.driverName?.takeIf { it.isNotBlank() } ?: "Driver Report", headerPaint)
                drawText(formatDateRangeForReport(params.startDate, params.endDate), headerPaint)
                y += 16

                // Summary Table
                drawTableHeader(canvas, y, listOf("Metric", "Value"))
                y += 26
                val summaryRows = listOf(
                    "Gross Revenue" to "$${formatMoney(params.grossRevenue)}",
                    "Total Miles" to "${formatMiles(params.totalMiles)} mi",
                    "Avg RPM" to "$${String.format("%.2f", params.avgRpm)}/mi",
                    "Total Loads" to "${params.totalLoads}"
                )
                summaryRows.forEachIndexed { i, (label, value) ->
                    if (i % 2 == 1) canvas.drawRect(margin.toFloat(), y - 16, (margin + contentWidth).toFloat(), y + 6, zebraPaint)
                    canvas.drawLine(margin.toFloat(), y, (margin + contentWidth).toFloat(), y, linePaint)
                    canvas.drawText(label, margin + 8f, y + 14, cellPaint)
                    canvas.drawText(value, (margin + contentWidth - 120).toFloat(), y + 14, cellPaint)
                    y += 22
                }
                canvas.drawLine(margin.toFloat(), y, (margin + contentWidth).toFloat(), y, linePaint)
                y += 24

                // Detailed Log header
                drawText("Detailed Log", headerPaint)
                drawTableHeader(canvas, y, listOf("Date", "Route", "Miles", "Amount", "RPM"))
                y += 22

                params.loads.sortedBy { it.date }.forEachIndexed { idx, load ->
                    if (y > pageHeight - 80) {
                        newPage()
                        drawTableHeader(canvas, y, listOf("Date", "Route", "Miles", "Amount", "RPM"))
                        y += 22
                    }
                    val rpm = if (load.totalMiles > 0) load.totalRate / load.totalMiles else 0.0
                    val route = "${load.pointA} → ${load.pointB}"
                    val routeShort = if (route.length > 35) route.take(32) + "…" else route
                    if (idx % 2 == 1) canvas.drawRect(margin.toFloat(), y - 16, (margin + contentWidth).toFloat(), y + 6, zebraPaint)
                    canvas.drawLine(margin.toFloat(), y, (margin + contentWidth).toFloat(), y, linePaint)
                    canvas.drawText(formatDate(load.date), margin + 8f, y + 14, cellPaint)
                    canvas.drawText(routeShort, margin + 75f, y + 14, cellPaint)
                    canvas.drawText(formatMiles(load.totalMiles), margin + 260f, y + 14, cellPaint)
                    canvas.drawText("$${formatMoney(load.totalRate)}", margin + 320f, y + 14, cellPaint)
                    canvas.drawText("$${String.format("%.2f", rpm)}", (margin + contentWidth - 55).toFloat(), y + 14, cellPaint)
                    y += 22
                }
                canvas.drawLine(margin.toFloat(), y, (margin + contentWidth).toFloat(), y, linePaint)
                y += 32

                // Footer
                val genDate = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
                drawText("Generated: $genDate", cellPaint.apply { textSize = 9f })
                drawText("— End of Report —", cellPaint.apply { textSize = 9f })

            doc.finishPage(page)
            doc.writeTo(out)
        } finally {
            doc.close()
        }
    }

    private fun drawTableHeader(canvas: Canvas, topY: Float, columns: List<String>) {
        val headerHeight = 24f
        val bottomY = topY + headerHeight
        canvas.drawRect(margin.toFloat(), topY, (margin + contentWidth).toFloat(), bottomY, Paint().apply {
            color = "#E0E0E0".toColorInt()
            style = Paint.Style.FILL
        })
        canvas.drawLine(margin.toFloat(), topY, (margin + contentWidth).toFloat(), topY, linePaint)
        canvas.drawLine(margin.toFloat(), bottomY, (margin + contentWidth).toFloat(), bottomY, linePaint)
        val colWidth = contentWidth / columns.size
        columns.forEachIndexed { i, text ->
            val px = margin + 8 + i * colWidth
            canvas.drawText(text, px.toFloat(), topY + 16, headerPaint)
        }
    }

    private fun formatMoney(v: Double): String =
        if (v >= 1000 || v <= -1000) "%,.0f".format(Locale.US, v)
        else "%,.2f".format(Locale.US, v)

    private fun formatMiles(v: Double): String = "%,.0f".format(Locale.US, v)

    private fun formatDate(s: String): String {
        if (s.length < 10) return s
        val parts = s.substring(0, 10).split("-")
        if (parts.size != 3) return s
        return "${parts[2]}.${parts[1]}.${parts[0]}"
    }
}
