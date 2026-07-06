package com.truckerload.sync.import

import android.content.Context
import com.truckerload.R
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.domain.import.model.ImportReport
import com.truckerload.sync.DuplicateAuditRunner
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

    suspend fun runPostImportDedup(
        context: Context,
        loadRepository: LoadRepository,
        paycheckRepository: PaycheckRepository,
        dieselRepository: DieselRepository,
    ): String {
        val report = DuplicateAuditRunner.run(
            loadRepository = loadRepository,
            paycheckRepository = paycheckRepository,
            dieselRepository = dieselRepository,
        )
        val deleted = report.deletedLoads + report.deletedPaychecks + report.deletedDiesel
        if (deleted == 0) return ""
        refreshWidgets(context)
        return DuplicateAuditRunner.formatReport(context, report)
    }
}
