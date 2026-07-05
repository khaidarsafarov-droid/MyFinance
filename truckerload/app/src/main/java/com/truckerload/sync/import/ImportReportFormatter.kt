package com.truckerload.sync.import

import android.content.Context
import com.truckerload.R
import com.truckerload.domain.import.model.ImportReport
import com.truckerload.domain.import.model.ParsedLoad
import com.truckerload.domain.import.model.SkipReason

class ImportReportFormatter(private val context: Context) {

    fun format(report: ImportReport): String = buildString {
        appendLine(context.getString(R.string.sync_import_done_title))
        appendLine()
        appendLine(context.getString(R.string.sync_import_result_header))
        appendLine(context.getString(R.string.sync_import_found, report.totalFound))
        appendLine(context.getString(R.string.sync_import_added, report.added))
        if (report.updated > 0) {
            appendLine(context.getString(R.string.sync_import_updated, report.updated))
        }
        if (report.replaced > 0) {
            appendLine(context.getString(R.string.sync_import_replaced, report.replaced))
        }
        appendLine(context.getString(R.string.sync_import_skipped, report.skipped))
        if (report.failed > 0) {
            appendLine(context.getString(R.string.sync_import_failed, report.failed))
        }
        if (report.fileName != null) {
            appendLine(context.getString(R.string.sync_import_file_name, report.fileName))
        }
        if (report.filesProcessed > 0) {
            appendLine(context.getString(R.string.sync_import_files_processed, report.filesProcessed))
        }
        appendLine(context.getString(R.string.sync_import_duration, report.durationMs))
        appendLine()

        if (report.addedLoads.isNotEmpty()) {
            appendLine(context.getString(R.string.sync_import_added_list))
            report.addedLoads.take(20).forEachIndexed { index, load ->
                appendLine(
                    context.getString(
                        R.string.sync_import_added_line,
                        index + 1,
                        load.tripId,
                        formatRoute(load),
                        load.totalRate,
                    )
                )
            }
            if (report.addedLoads.size > 20) {
                appendLine(context.getString(R.string.sync_import_more, report.addedLoads.size - 20))
            }
            appendLine()
        }

        if (report.skippedLoads.isNotEmpty()) {
            appendLine(context.getString(R.string.sync_import_skipped_list))
            report.skippedLoads.take(10).forEach { (id, reason) ->
                appendLine("• $id — ${reasonLabel(reason)}")
            }
            appendLine()
        }

        if (report.failedBlocks.isNotEmpty()) {
            appendLine(context.getString(R.string.sync_import_failed_list))
            report.failedBlocks.take(5).forEach { (id, error) ->
                appendLine("• $id: $error")
            }
        }
    }

    private fun formatRoute(load: ParsedLoad): String {
        val from = load.pointA.substringBefore(",").ifBlank { "?" }
        val to = load.pointB.substringBefore(",").ifBlank { "?" }
        return "$from → $to"
    }

    private fun reasonLabel(reason: SkipReason): String = when (reason) {
        SkipReason.DUPLICATE -> context.getString(R.string.sync_import_reason_duplicate)
        SkipReason.INVALID_DATA -> context.getString(R.string.sync_import_reason_invalid)
        SkipReason.ALREADY_BOOKED -> context.getString(R.string.sync_import_reason_booked)
        SkipReason.NO_CHANGES -> context.getString(R.string.sync_import_reason_no_changes)
        SkipReason.AUTO_UPDATE_DISABLED -> context.getString(R.string.sync_import_reason_auto_update_off)
        SkipReason.SUSPICIOUS_DUPLICATE -> context.getString(R.string.sync_import_reason_suspicious)
    }
}
