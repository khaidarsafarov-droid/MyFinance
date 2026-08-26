package com.truckerload.data.backup

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.hilt.work.HiltWorker
import com.truckerload.data.preferences.AuthStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Background Drive backup when the device has network (guide Part 3 — WorkManager trigger).
 */
@HiltWorker
class DriveSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val authStore: AuthStore,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = authStore.currentUserIdOrNull()
        if (userId.isNullOrBlank()) {
            Log.i(TAG, "No session — skip Drive sync")
            return Result.success()
        }
        return try {
            val ok = GoogleDriveBackupService.pushAutoBackupIfEnabled(applicationContext)
            Log.i(TAG, "Drive auto backup attempted")
            if (ok) Result.success() else Result.retry()
        } catch (e: Exception) {
            Log.w(TAG, "Drive sync worker failed", e)
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_PERIODIC = "drive_sync_periodic"
        private const val TAG = "DriveSyncWorker"

        fun enqueuePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<DriveSyncWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
