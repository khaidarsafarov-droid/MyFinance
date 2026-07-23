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
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.SyncOutboxEntity
import com.truckerload.data.preferences.AuthStore

/**
 * Drains [SyncOutboxEntity] when the device is online, then publishes the
 * account snapshot via [com.truckerload.data.sync.CloudSyncEngine].
 */
class OutboundSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = AuthStore(applicationContext).currentUserIdOrNull()
        if (userId.isNullOrBlank()) return Result.success()
        val db = AppDatabase.getInstance(applicationContext, userId)
        val dao = db.syncOutboxDao()
        val pending = dao.listByStatus(SyncOutboxEntity.STATUS_PENDING, limit = 40)

        pending.forEach { item ->
            dao.updateStatus(
                id = item.id,
                status = SyncOutboxEntity.STATUS_SYNCED,
                attempts = item.attempts + 1,
                lastError = null,
            )
        }
        if (pending.isNotEmpty()) {
            Log.i(TAG, "Marked ${pending.size} outbox rows synced; pushing account snapshot")
        }
        runCatching {
            com.truckerload.data.sync.CloudSyncEngine.pushLocalSnapshot(applicationContext)
        }.onFailure {
            Log.w(TAG, "Account snapshot push failed", it)
            return Result.retry()
        }
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
