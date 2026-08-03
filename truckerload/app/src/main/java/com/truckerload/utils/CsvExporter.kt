package com.truckerload.utils

import android.content.Context
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.formatLoadRoute
import com.truckerload.domain.model.withRouteMetrics
import com.truckerload.domain.parser.ParseUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Exports loads to UTF-8 CSV for Excel / Google Sheets. */
object CsvExporter {

    private const val EXPORTS_SUBDIR = "exports"
    // FIX: thread-safe date stamp — SimpleDateFormat is not safe across concurrent IO exports
    private val dayStamp: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.US)

    const val CSV_HEADER = "date,route,income,miles,rpm"

    /** Pure row formatter for unit tests and export. */
    fun formatLoadCsvRow(load: Load): String {
        val metrics = load.withRouteMetrics()
        val route = formatLoadRoute(metrics).replace("\"", "\"\"")
        val income = metrics.totalRate
        // FIX: sanitize typo miles so export RPM matches Room-mapped journal values
        val miles = ParseUtils.sanitizeLoadedMiles(metrics.totalMiles, income)
        val rpm = if (miles > 0) income / miles else 0.0
        return listOf(
            metrics.date,
            "\"$route\"",
            String.format(Locale.US, "%.2f", income),
            String.format(Locale.US, "%.0f", miles),
            String.format(Locale.US, "%.2f", rpm),
        ).joinToString(",")
    }

    suspend fun exportAllLoads(context: Context, loads: List<Load>): File = withContext(Dispatchers.IO) {
        require(loads.isNotEmpty()) { "no loads" }
        val sorted = loads.sortedByDescending { it.date.ifBlank { "0000-00-00" } }
        val rows = sorted.map(::formatLoadCsvRow)
        val content = buildString {
            appendLine(CSV_HEADER)
            rows.forEach { appendLine(it) }
        }
        val dir = File(context.getExternalFilesDir(null), EXPORTS_SUBDIR).apply { mkdirs() }
        val fileName = "${BrandConstants.FILE_PREFIX}_Export_${LocalDate.now().format(dayStamp)}.csv"
        File(dir, fileName).apply { writeText(content, Charsets.UTF_8) }
    }
}
