package com.truckerload.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Backup for when exact alarms are not allowed, or an exact alarm was dropped.
 * WorkManager can run late in Doze; the exact alarm is the primary path.
 */
@HiltWorker
class MaintenanceReminderSafetyWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        MaintenanceReminderCoordinator.sync(applicationContext)
        return Result.success()
    }
}
