package com.truckerload.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.truckerload.domain.model.Load
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
        val appContext = context.applicationContext
        val uri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            file
        )
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/plain")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        appContext.startActivity(
            Intent.createChooser(viewIntent, appContext.getString(com.truckerload.R.string.open_folder))
        )
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
            val miles = formatMiles(load.totalMiles)
            val income = formatMoney(load.totalRate)
            appendLine("${index + 1}. $date | $route | $miles | $income")
        }
    }

    private fun formatStatistics(loads: List<Load>): String {
        val count = loads.size
        val totalIncome = loads.sumOf { it.totalRate }
        val totalMiles = loads.sumOf { it.totalMiles }
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
