package com.truckerload.utils

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.truckerload.R
import com.truckerload.domain.report.PeriodReport
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class PeriodReportFormat {
    PDF,
    TEXT,
}

data class PeriodReportReady(
    val file: File?,
    val mime: String,
    val subject: String,
    val body: String,
)

/** Writes a PDF into app exports and opens the system share sheet. Text is the message body. */
object PeriodReportShare {

    private const val EXPORTS_SUBDIR = "exports"
    private val stamp: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm", Locale.US)
    const val MIME_PDF = "application/pdf"
    const val MIME_TEXT = "text/plain"

    fun writePdf(context: Context, report: PeriodReport, periodKey: String): File {
        val regular = readFont(context, "LiberationSans-Regular.ttf")
        val bold = readFont(context, "LiberationSans-Bold.ttf")
        val bytes = PeriodReportPdf.render(report, regular, bold)
        val dir = exportsDir(context).apply { mkdirs() }
        val name = "${BrandConstants.FILE_PREFIX}_Period_${slug(periodKey)}_${LocalDateTime.now().format(stamp)}.pdf"
        return File(dir, name).apply { writeBytes(bytes) }
    }

    fun share(context: Context, ready: PeriodReportReady) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = ready.mime
            putExtra(Intent.EXTRA_SUBJECT, ready.subject)
            putExtra(Intent.EXTRA_TEXT, ready.body)
        }
        val file = ready.file
        if (file != null && file.exists()) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            send.putExtra(Intent.EXTRA_STREAM, uri)
            send.clipData = ClipData.newUri(context.contentResolver, file.name, uri)
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, context.getString(R.string.period_report_chooser)).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = send.clipData
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun readFont(context: Context, name: String): ByteArray =
        context.assets.open("fonts/$name").use { it.readBytes() }

    private fun exportsDir(context: Context): File {
        val root = context.getExternalFilesDir(null) ?: context.cacheDir
        return File(root, EXPORTS_SUBDIR)
    }

    private fun slug(raw: String): String =
        raw.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "period" }
}
