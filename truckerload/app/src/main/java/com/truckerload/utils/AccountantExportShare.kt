package com.truckerload.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.truckerload.R
import com.truckerload.domain.tax.AccountantExportSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Writes and shares accountant workbooks via the system share sheet (Telegram, WhatsApp, email, …). */
object AccountantExportShare {

    private const val EXPORTS_SUBDIR = "exports"
    private val dayStamp: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.US)

    /** Excel opens SpreadsheetML with this MIME / .xls extension. */
    const val MIME_EXCEL = "application/vnd.ms-excel"

    suspend fun writeWorkbook(
        context: Context,
        input: AccountantWorkbookBuilder.Input,
        sections: Set<AccountantExportSection>,
    ): File = withContext(Dispatchers.IO) {
        val xml = AccountantWorkbookBuilder.buildXml(input, sections)
        val label = AccountantWorkbookBuilder.fileLabel(sections)
        val dir = File(context.getExternalFilesDir(null), EXPORTS_SUBDIR).apply { mkdirs() }
        val name = "${BrandConstants.FILE_PREFIX}_Tax_${input.year}_${label}_${
            LocalDate.now().format(dayStamp)
        }.xls"
        File(dir, name).apply { writeText(xml, Charsets.UTF_8) }
    }

    fun shareWorkbook(context: Context, file: File) {
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = MIME_EXCEL
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
            putExtra(
                Intent.EXTRA_TEXT,
                context.getString(R.string.tax_send_share_body, file.name),
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(
                intent,
                context.getString(R.string.tax_send_chooser_title),
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }
}
