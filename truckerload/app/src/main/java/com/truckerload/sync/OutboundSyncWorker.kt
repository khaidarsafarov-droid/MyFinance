package com.truckerload.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.hilt.work.HiltWorker
import com.truckerload.data.local.entities.SyncOutboxEntity
import com.truckerload.data.preferences.AuthStore
import com.truckerload.di.UserComponentManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Drains [SyncOutboxEntity] when the device is online, then publishes the
 * account snapshot via [com.truckerload.data.sync.CloudSyncEngine].
 */
@HiltWorker
class OutboundSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val authStore: AuthStore,
    private val userComponentManager: UserComponentManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = authStore.currentUserIdOrNull() ?: return Result.success()
        val db = userComponentManager.startSession(userId).database
        val dao = db.syncOutboxDao()
        val pending = dao.listByStatus(SyncOutboxEntity.STATUS_PENDING, limit = 40)

        val uploadAcknowledged = runCatching {
            com.truckerload.data.sync.CloudSyncEngine.pushLocalSnapshot(applicationContext)
        }.getOrElse {
            Log.w(TAG, "Account snapshot push failed: ${it.javaClass.simpleName}")
            false
        }
        pending.forEach { item ->
            val update = OutboundSyncPolicy.afterSnapshotUpload(item.attempts, uploadAcknowledged)
            dao.updateStatus(
                id = item.id,
                status = update.status,
                attempts = update.attempts,
                lastError = update.lastError,
            )
        }
        if (!uploadAcknowledged) return Result.retry()
        if (pending.isNotEmpty()) Log.i(TAG, "Snapshot acknowledged; synced ${pending.size} outbox rows")
        val weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        dao.deleteOlderThan(SyncOutboxEntity.STATUS_SYNCED, weekAgo)
        return Result.success()
    }

    companion object {
        private const val TAG = "OutboundSyncWorker"
        const val UNIQUE_WORK = "outbound_sync_drain"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<OutboundSyncWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_WORK,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }
    }
}
