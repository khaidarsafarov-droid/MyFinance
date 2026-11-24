package com.truckerload.sync

import android.content.Context
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.repository.JournalWriteEffects
import com.truckerload.utils.BackupService
import com.truckerload.widget.WidgetDataUpdater

/**
 * The app's answer to journal writes. Runs only while a journal is open
 * ([AppDatabase.hostContext]), as the repositories did before.
 */
class AppJournalWriteEffects(context: Context) : JournalWriteEffects {

    private val app: Context = context.applicationContext ?: context

    override fun onDataChanged() {
        if (openJournal()) WidgetDataUpdater.updateWidgetData(app)
    }

    override fun onBackupWorthyWrite() {
        if (openJournal()) BackupService.scheduleCreateAutoBackup(app)
    }

    override fun onMileageInputsChanged() {
        if (openJournal()) MaintenanceReminderCoordinator.enqueue(app)
    }

    private fun openJournal(): Boolean = AppDatabase.hostContext() != null
}
