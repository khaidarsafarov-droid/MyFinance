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
import com.truckerload.data.preferences.TelegramTokenStore

/**
 * Drains [SyncOutboxEntity] when the device is online.
 *
 * Delivery strategy (hybrid):
 * 1. If a Telegram bot token is configured, log a batch summary (server/bot
 *    ingest can replace this with a real push API later).
 * 2. On success (or no-token local drain), mark rows SYNCED.
 * 3. On failure, bump attempts and leave PENDING for retry.
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
        if (pending.isEmpty()) return Result.success()

        val token = TelegramTokenStore(applicationContext, userId).getToken()
        val delivered = if (token.isNotBlank()) {
            val summary = buildString {
                append("Truck Load sync batch: ")
                append(pending.size)
                append(" ops (")
                append(pending.groupBy { it.entityType }.entries.joinToString { "${it.key}=${it.value.size}" })
                append(')')
            }
            Log.i(TAG, summary)
            true
        } else {
            Log.i(TAG, "No bot token — marking ${pending.size} outbox rows synced (local-only drain)")
            true
        }

        if (!delivered) {
            pending.forEach { item ->
                dao.updateStatus(
                    id = item.id,
                    status = SyncOutboxEntity.STATUS_PENDING,
                    attempts = item.attempts + 1,
                    lastError = "network_or_bot_unavailable",
                )
            }
            return Result.retry()
        }

        pending.forEach { item ->
            dao.updateStatus(
                id = item.id,
                status = SyncOutboxEntity.STATUS_SYNCED,
                attempts = item.attempts + 1,
                lastError = null,
            )
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
