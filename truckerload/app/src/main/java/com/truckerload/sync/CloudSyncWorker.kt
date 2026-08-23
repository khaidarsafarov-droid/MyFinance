package com.truckerload.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.sync.CloudSyncEngine as LegacyCloudSyncEngine
import com.truckerload.data.sync.cloud.CloudSyncEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Periodic / one-shot account cloud sync (push outbox drain + pull/hydrate).
 * Routes through the Hilt [CloudSyncEngine] so [com.truckerload.data.sync.cloud.SyncStatusTracker] updates.
 */
@HiltWorker
class CloudSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val cloudSyncEngine: CloudSyncEngine,
    private val authStore: AuthStore,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            OutboundSyncWorker.enqueue(applicationContext)
            val result = cloudSyncEngine.onSessionReady()
            Log.i(TAG, "Cloud sync: $result")
            if (result.mode == LegacyCloudSyncEngine.SyncResult.Mode.DEVICE_SLOT_DENIED) {
                authStore.logout()
                return Result.success()
            }
            if (result.retryableFailure) Result.retry() else Result.success()
        }.getOrElse {
            Log.w(TAG, "Cloud sync failed", it)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "CloudSyncWorker"
        const val UNIQUE_ONESHOT = "cloud_sync_oneshot"
        const val UNIQUE_PERIODIC = "cloud_sync_periodic"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<CloudSyncWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_ONESHOT,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }

        fun enqueuePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<CloudSyncWorker>(6, TimeUnit.HOURS)
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
