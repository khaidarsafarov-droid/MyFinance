package com.truckerload.sync

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.AccountIds
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.sync.AccountCloudBackendFactory
import java.util.concurrent.TimeUnit

class ServerTelegramInboxWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!TelegramSyncMode.isServer()) return Result.success()
        val auth = AuthStore(applicationContext)
        val userId = auth.currentUserIdOrNull() ?: return Result.success()
        if (auth.accessTokenOrNull().isNullOrBlank()) return Result.retry()
        val client = runCatching { AccountCloudBackendFactory.remoteClientOrNull(applicationContext) }
            .getOrElse {
                Log.w(TAG, "Server Telegram backend configuration is invalid")
                return Result.failure()
            }
            ?: return Result.success()
        val cursor = cursor(userId)
        val inbox = runCatching { client.getTelegramInbox(cursor) }.getOrElse {
            Log.w(TAG, "Server Telegram inbox download failed: ${it.javaClass.simpleName}")
            return Result.retry()
        }
        if (inbox.items.isEmpty()) return Result.success()

        val processor = runCatching {
            ServerTelegramMessageProcessor(
                context = applicationContext,
                db = AppDatabase.getInstance(applicationContext, userId),
            )
        }.getOrElse {
            Log.w(TAG, "Server Telegram inbox database unavailable: ${it.javaClass.simpleName}")
            return Result.retry()
        }
        for (item in inbox.items.sortedBy { it.updateId }) {
            val processed = runCatching {
                processor.process(item.text, item.receivedAt)
            }.getOrElse {
                Log.w(TAG, "Server Telegram inbox database processing failed: ${it.javaClass.simpleName}")
                return Result.retry()
            }
            if (processed !in ACKNOWLEDGEABLE_RESULTS) return Result.retry()
            val acknowledged = runCatching {
                client.acknowledgeTelegramInbox(item.updateId)
            }.isSuccess
            if (!acknowledged) {
                Log.w(TAG, "Server Telegram inbox acknowledgement failed")
                return Result.retry()
            }
            markCursor(userId, item.updateId)
        }
        return Result.success()
    }

    private fun cursor(userId: String): Long =
        applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(cursorKey(userId), 0L)

    private fun markCursor(userId: String, updateId: Long) {
        applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putLong(cursorKey(userId), updateId)
        }
    }

    private fun cursorKey(userId: String): String =
        KEY_CURSOR_PREFIX + AccountIds.sanitizeFilePart(userId)

    companion object {
        private const val TAG = "ServerTelegramInbox"
        private const val PREFS_NAME = "server_telegram_inbox"
        private const val KEY_CURSOR_PREFIX = "last_acknowledged_"
        const val UNIQUE_ONESHOT = "server_telegram_inbox_oneshot"
        const val UNIQUE_PERIODIC = "server_telegram_inbox_periodic"
        private val ACKNOWLEDGEABLE_RESULTS = setOf(
            ServerInboxProcessingResult.PROCESSED,
            ServerInboxProcessingResult.IGNORED,
        )

        fun enqueue(context: Context) {
            if (!TelegramSyncMode.isServer()) return
            val request = OneTimeWorkRequestBuilder<ServerTelegramInboxWorker>()
                .setConstraints(networkConstraints())
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_ONESHOT,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }

        fun enqueuePeriodic(context: Context) {
            if (!TelegramSyncMode.isServer()) return
            val request = PeriodicWorkRequestBuilder<ServerTelegramInboxWorker>(
                15,
                TimeUnit.MINUTES,
            )
                .setConstraints(networkConstraints())
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        private fun networkConstraints(): Constraints =
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
    }
}
