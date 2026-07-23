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
import com.truckerload.data.preferences.AuthStore
import java.util.concurrent.TimeUnit

/**
 * Background Drive backup when the device has network (guide Part 3 — WorkManager trigger).
 */
class DriveSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = AuthStore(applicationContext).currentUserIdOrNull()
        if (userId.isNullOrBlank()) {
            Log.i(TAG, "No session — skip Drive sync")
            return Result.success()
        }
        return try {
            GoogleDriveBackupService.pushAutoBackupIfEnabled(applicationContext)
            Log.i(TAG, "Drive auto backup attempted")
            Result.success()
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
