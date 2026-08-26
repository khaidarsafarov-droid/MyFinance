package com.truckerload.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.truckerload.domain.expense.MiscExpenseFields
import com.truckerload.domain.model.MiscExpense
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** CSV of miscellaneous expenses for sharing with an accountant or chat. */
object MiscExpenseExporter {

    private const val EXPORTS_SUBDIR = "exports"
    private val dayStamp: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.US)

    const val CSV_HEADER = "date,description,amount_usd,receipt_attached"

    fun buildCsv(expenses: List<MiscExpense>): String {
        val sorted = expenses.sortedWith(compareBy<MiscExpense> { it.date }.thenBy { it.id })
        val total = sorted.sumOf { it.amount }
        return buildString {
            appendLine("# ${BrandConstants.DISPLAY_NAME} miscellaneous expenses")
            appendLine("# count,${sorted.size}")
            appendLine("# total_usd,${String.format(Locale.US, "%.2f", total)}")
            appendLine(CSV_HEADER)
            for (row in sorted) {
                val hasReceipt = if (!row.receiptPhotoPath.isNullOrBlank()) "yes" else "no"
                appendLine(
                    listOf(
                        row.date,
                        MiscExpenseFields.csvQuote(row.description),
                        String.format(Locale.US, "%.2f", row.amount),
                        hasReceipt,
                    ).joinToString(","),
                )
            }
        }
    }

    suspend fun writeCsvFile(context: Context, expenses: List<MiscExpense>): File =
        withContext(Dispatchers.IO) {
            val content = buildCsv(expenses)
            val dir = File(context.getExternalFilesDir(null), EXPORTS_SUBDIR).apply { mkdirs() }
            val fileName =
                "${BrandConstants.FILE_PREFIX}_MiscExpenses_${LocalDate.now().format(dayStamp)}.csv"
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
                context.getString(com.truckerload.R.string.misc_expense_send_title),
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }
}
