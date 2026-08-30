package com.truckerload.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.truckerload.R
import com.truckerload.domain.model.Load
import com.truckerload.domain.parser.ParseUtils
import com.truckerload.presentation.utils.MoneyFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

object LoadExporter {

    private const val EXPORTS_SUBDIR = "exports"
    // FIX: thread-safe export filename — shared SimpleDateFormat corrupts under parallel exports
    private val fileTimestamp: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.US)

    suspend fun exportAllLoads(context: Context, loads: List<Load>): File = withContext(Dispatchers.IO) {
        require(loads.isNotEmpty()) { "no loads" }
        val sorted = loads.sortedByDescending { sortKey(it) }
        val content = buildString {
            append(formatHeader(sorted.size))
            appendLine()
            append(formatLoads(sorted))
            appendLine()
            append(formatStatistics(sorted))
        }
        val dir = exportsDir(context).apply { mkdirs() }
        val fileName = "${BrandConstants.FILE_PREFIX}_Export_${LocalDateTime.now().format(fileTimestamp)}.txt"
        File(dir, fileName).apply { writeText(content, Charsets.UTF_8) }
    }

    fun exportsDir(context: Context): File =
        File(context.getExternalFilesDir(null), EXPORTS_SUBDIR)

    fun openExportsFolder(context: Context, file: File) {
        val host = context.findActivity() ?: context.applicationContext
        if (!file.exists()) {
            Toast.makeText(host, host.getString(R.string.export_open_failed), Toast.LENGTH_LONG).show()
            return
        }
        val app = host.applicationContext
        val uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
        val mime = mimeForExport(file)
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            clipData = ClipData.newUri(host.contentResolver, file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            host.startActivity(chooserForExport(host, view))
        } catch (_: ActivityNotFoundException) {
            shareExportOrToast(host, uri, mime, file.name)
        } catch (_: RuntimeException) {
            shareExportOrToast(host, uri, mime, file.name)
        }
    }

    internal fun mimeForExport(file: File): String = when (file.extension.lowercase(Locale.US)) {
        "csv" -> "text/csv"
        "txt" -> "text/plain"
        "json", "tlb" -> "application/json"
        else -> "application/octet-stream"
    }

    internal fun chooserForExport(host: Context, target: Intent): Intent {
        return Intent.createChooser(target, host.getString(R.string.open_folder)).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = target.clipData
            if (host !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    private fun shareExportOrToast(
        host: Context,
        uri: android.net.Uri,
        mime: String,
        title: String,
    ) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(host.contentResolver, title, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            host.startActivity(chooserForExport(host, send))
        } catch (_: Exception) {
            Toast.makeText(host, host.getString(R.string.export_open_failed), Toast.LENGTH_LONG).show()
        }
    }

    private fun sortKey(load: Load): String = load.date.ifBlank { "0000-00-00" }

    private fun formatHeader(count: Int): String = buildString {
        appendLine("========================================")
        appendLine("📝🚛 ${BrandConstants.DISPLAY_NAME} — ALL LOADS")
        appendLine("========================================")
        appendLine("Export date: ${formatExportTimestamp()}")
        appendLine("Total loads: $count")
    }

    private fun formatLoads(loads: List<Load>): String = buildString {
        appendLine()
        appendLine("========================================")
        appendLine("📦 LOADS (${loads.size})")
        appendLine("========================================")
        appendLine()
        loads.forEachIndexed { index, load ->
            val date = formatDisplayDate(load.date)
            val route = "${load.pointA} → ${load.pointB}"
            val miles = formatMiles(ParseUtils.sanitizeLoadedMiles(load.totalMiles, load.totalRate))
            val income = formatMoney(load.totalRate)
            appendLine("${index + 1}. $date | $route | $miles | $income")
        }
    }

    private fun formatStatistics(loads: List<Load>): String {
        val count = loads.size
        val totalIncome = loads.sumOf { it.totalRate }
        val totalMiles = loads.sumOf { ParseUtils.sanitizeLoadedMiles(it.totalMiles, it.totalRate) }
        val avgIncome = if (count > 0) totalIncome / count else 0.0
        val avgRpm = if (totalMiles > 0) totalIncome / totalMiles else 0.0
        return buildString {
            appendLine()
            appendLine("========================================")
            appendLine("📊 TOTALS:")
            appendLine("========================================")
            appendLine("Total loads: $count")
            appendLine("Total income: ${formatMoney(totalIncome)}")
            appendLine("Total miles: ${formatMilesValue(totalMiles)}")
            appendLine("Avg income per load: ${formatMoney(avgIncome)}")
            appendLine("Avg rate per mile: ${formatRpm(avgRpm)}")
            appendLine("========================================")
        }
    }

    private fun formatExportTimestamp(): String {
        val cal = Calendar.getInstance()
        return "%02d.%02d.%04d %02d:%02d".format(
            Locale.US,
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.YEAR),
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE)
        )
    }

    private fun formatDisplayDate(dateStr: String): String {
        val parts = dateStr.split("-")
        if (parts.size != 3) return dateStr
        return "${parts[2]}.${parts[1]}.${parts[0]}"
    }

    private fun formatMiles(miles: Double): String =
        MoneyFormat.formatMiles(miles)

    private fun formatMilesValue(miles: Double): String =
        MoneyFormat.formatNumber(miles)

    private fun formatMoney(amount: Double): String =
        MoneyFormat.formatCurrency(amount)

    private fun formatRpm(rpm: Double): String =
        MoneyFormat.formatRpmShort(rpm)
}
