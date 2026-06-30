package com.truckerload.sync.import

import android.content.Context
import com.truckerload.R
import com.truckerload.domain.import.model.ImportReport
import com.truckerload.widget.WidgetDataUpdater
import com.truckerload.widget.WidgetUpdateWorker

internal object ImportHandlerSupport {

    fun formatSuccess(
        context: Context,
        reportFormatter: ImportReportFormatter,
        sessionManager: ImportSessionManager,
        chatId: String,
        report: ImportReport,
        countAsFile: Boolean = false,
    ): String {
        if (countAsFile) {
            sessionManager.incrementFilesProcessed(chatId)
        }
        val sessionFiles = sessionManager.getFilesProcessed(chatId)
        val body = reportFormatter.format(report)
        val footer = context.getString(R.string.sync_import_send_more, sessionFiles)
        return "$body\n\n$footer"
    }

    fun refreshWidgets(context: Context) {
        WidgetDataUpdater.updateWidgetData(context.applicationContext)
        WidgetUpdateWorker.refreshNow(context.applicationContext)
    }
}
