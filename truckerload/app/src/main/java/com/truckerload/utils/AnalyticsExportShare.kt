package com.truckerload.utils

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.truckerload.R
import com.truckerload.data.repository.AnalyticsDashboard
import com.truckerload.domain.model.analytics.AnalyticsPeriod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Writes My numbers reports into app exports/ and opens the system share sheet. */
object AnalyticsExportShare {

    private const val EXPORTS_SUBDIR = "exports"
    private val stamp: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm", Locale.US)

    const val MIME_TEXT = "text/plain"
    const val MIME_CSV = "text/csv"

    suspend fun writeReport(
        context: Context,
        dashboard: AnalyticsDashboard,
        labels: AnalyticsExportLabels,
        format: AnalyticsShareFormat,
        period: AnalyticsPeriod,
    ): File = withContext(Dispatchers.IO) {
        val body = when (format) {
            AnalyticsShareFormat.TEXT -> AnalyticsReportBuilder.buildReadableText(dashboard, labels)
            AnalyticsShareFormat.CSV -> AnalyticsReportBuilder.buildCsvContent(dashboard, labels)
        }
        val ext = if (format == AnalyticsShareFormat.TEXT) "txt" else "csv"
        val dir = File(context.getExternalFilesDir(null), EXPORTS_SUBDIR).apply { mkdirs() }
        val periodSlug = period.name.lowercase(Locale.US)
        val name = "${BrandConstants.FILE_PREFIX}_MyNumbers_${periodSlug}_${LocalDateTime.now().format(stamp)}.$ext"
        File(dir, name).apply { writeText(body, Charsets.UTF_8) }
    }

    fun shareReport(
        context: Context,
        file: File,
        format: AnalyticsShareFormat,
        caption: String,
    ) {
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val mime = if (format == AnalyticsShareFormat.TEXT) MIME_TEXT else MIME_CSV
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
            putExtra(Intent.EXTRA_TEXT, caption)
            clipData = ClipData.newUri(context.contentResolver, file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(
                intent,
                context.getString(R.string.analytics_share_chooser),
            ).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }
}
