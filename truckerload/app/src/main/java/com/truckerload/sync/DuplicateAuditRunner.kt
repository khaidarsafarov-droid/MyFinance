package com.truckerload.sync

import android.content.Context
import com.truckerload.R
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.domain.parser.DuplicateAuditReport
import com.truckerload.domain.parser.DuplicateAuditUseCase
import com.truckerload.widget.WidgetDataUpdater
import com.truckerload.widget.WidgetUpdateWorker

object DuplicateAuditRunner {

    suspend fun run(
        loadRepository: LoadRepository,
        paycheckRepository: PaycheckRepository,
        dieselRepository: DieselRepository,
    ): DuplicateAuditReport {
        return DuplicateAuditUseCase(
            loadRepository = loadRepository,
            paycheckRepository = paycheckRepository,
            dieselRepository = dieselRepository,
        ).auditAndRemove()
    }

    fun formatReport(context: Context, report: DuplicateAuditReport): String = buildString {
        val totalDeleted = report.deletedLoads + report.deletedPaychecks + report.deletedDiesel
        appendLine(context.getString(R.string.sync_dedup_done_title))
        appendLine(context.getString(R.string.sync_dedup_scanned, report.scannedLoads))
        appendLine(context.getString(R.string.sync_dedup_deleted_loads, report.deletedLoads))
        appendLine(context.getString(R.string.sync_dedup_deleted_paychecks, report.deletedPaychecks))
        appendLine(context.getString(R.string.sync_dedup_deleted_diesel, report.deletedDiesel))
        appendLine(context.getString(R.string.sync_dedup_duration, report.durationMs))
        if (totalDeleted == 0) {
            appendLine()
            append(context.getString(R.string.sync_dedup_none))
        } else if (report.deletedLoadTripIds.isNotEmpty()) {
            appendLine()
            appendLine(context.getString(R.string.sync_dedup_removed_loads))
            report.deletedLoadTripIds.take(10).forEach { tripId ->
                appendLine("• $tripId")
            }
            if (report.deletedLoadTripIds.size > 10) {
                appendLine(context.getString(R.string.sync_import_more, report.deletedLoadTripIds.size - 10))
            }
        }
    }

    fun refreshWidgets(context: Context) {
        WidgetDataUpdater.updateWidgetData(context.applicationContext)
        WidgetUpdateWorker.refreshNow(context.applicationContext)
    }
}
