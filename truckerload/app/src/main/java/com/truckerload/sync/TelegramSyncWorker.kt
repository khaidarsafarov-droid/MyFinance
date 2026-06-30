package com.truckerload.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import com.truckerload.data.preferences.TelegramTokenStore
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.WorkManager
import android.util.Log
import java.util.concurrent.TimeUnit

/**
 * Ensures [TelegramBotForegroundService] is running. Polling only happens in the service (single client).
 */
class TelegramSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val PREFS_NAME = "telegram_sync"
        const val KEY_LAST_OFFSET = "last_update_offset"
        const val KEY_MANUAL_RESTORE_PREFIX = "manual_restore_mode_"
        const val KEY_MANUAL_RESTORE_COUNT_PREFIX = "manual_restore_count_"
        const val KEY_MANUAL_RESTORE_LAST_ACTIVITY_PREFIX = "manual_restore_last_activity_"
        const val KEY_IMPORT_MODE_PREFIX = "import_mode_"
        const val KEY_IMPORT_LAST_ACTIVITY_PREFIX = "import_last_activity_"
        const val KEY_IMPORT_FILES_PREFIX = "import_files_"
        const val MANUAL_RESTORE_TIMEOUT_MS = 5 * 60 * 1000L
        const val MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024
    }

    override suspend fun doWork(): Result {
        val token = TelegramTokenStore(applicationContext).getToken()
        if (token.isBlank()) {
            Log.w("TelegramSync", "No bot token — set TELEGRAM_BOT_TOKEN in local.properties or Settings")
            scheduleNext(120)
            return Result.success()
        }
        if (!TelegramPollCoordinator.isForegroundPolling()) {
            TelegramBotForegroundService.start(applicationContext)
        }
        scheduleNext(300)
        return Result.success()
    }

    private fun scheduleNext(delaySeconds: Long) {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val nextWork = OneTimeWorkRequestBuilder<TelegramSyncWorker>()
            .setConstraints(constraints)
            .setInitialDelay(delaySeconds.coerceAtLeast(60), TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(applicationContext).enqueue(nextWork)
    }
}
