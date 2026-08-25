package com.truckerload.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** CSV report of on-duty per-diem days for the driver's accountant. */
object TaxPerDiemExporter {

    private const val EXPORTS_SUBDIR = "exports"
    private val dayStamp: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.US)

    const val CSV_HEADER = "date,per_diem_rate_usd,amount_usd"

    fun buildCsv(
        year: Int,
        dates: Set<String>,
        dailyRate: Double,
        dieselDeductions: Double,
        grossIncome: Double,
    ): String {
        val sorted = dates.sorted()
        val days = sorted.size
        val perDiemTotal = days * dailyRate
        return buildString {
            appendLine("# ${BrandConstants.DISPLAY_NAME} per-diem report $year")
            appendLine("# gross_income_usd,$grossIncome")
            appendLine("# diesel_deductions_usd,$dieselDeductions")
            appendLine("# per_diem_days,$days")
            appendLine("# per_diem_rate_usd,$dailyRate")
            appendLine("# per_diem_total_usd,$perDiemTotal")
            appendLine(CSV_HEADER)
            for (date in sorted) {
                appendLine(
                    listOf(
                        date,
                        String.format(Locale.US, "%.2f", dailyRate),
                        String.format(Locale.US, "%.2f", dailyRate),
                    ).joinToString(","),
                )
            }
        }
    }

    suspend fun writeCsvFile(
        context: Context,
        year: Int,
        dates: Set<String>,
        dailyRate: Double,
        dieselDeductions: Double,
        grossIncome: Double,
    ): File = withContext(Dispatchers.IO) {
        val content = buildCsv(year, dates, dailyRate, dieselDeductions, grossIncome)
        val dir = File(context.getExternalFilesDir(null), EXPORTS_SUBDIR).apply { mkdirs() }
        val fileName = "${BrandConstants.FILE_PREFIX}_PerDiem_${year}_${LocalDate.now().format(dayStamp)}.csv"
        File(dir, fileName).apply { writeText(content, Charsets.UTF_8) }
    }

    fun shareCsv(context: Context, file: File) {
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(
                intent,
                context.getString(com.truckerload.R.string.tax_export_share_title),
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }
}
