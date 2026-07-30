package com.truckerload.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.hilt.work.HiltWorker
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.TelegramTokenStore
import android.util.Log
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Ensures [TelegramBotForegroundService] is running. Polling only happens in the service (single client).
 */
@HiltWorker
class TelegramSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val authStore: AuthStore,
) : CoroutineWorker(context, params) {

    companion object {
        const val UNIQUE_ENSURE_SERVICE_WORK = "telegram_sync_ensure_service"
        const val UNIQUE_WATCHDOG_WORK = "telegram_bot_watchdog"
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

        fun enqueueEnsureService(context: Context, replace: Boolean = false) {
            if (TelegramSyncMode.isServer()) return
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<TelegramSyncWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_ENSURE_SERVICE_WORK,
                if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
                request,
            )
        }
    }

    override suspend fun doWork(): Result {
        if (TelegramSyncMode.isServer()) return Result.success()
        val userId = authStore.currentUserIdOrNull()
        if (userId.isNullOrBlank()) {
            Log.w("TelegramSync", "No active user — skip ensuring Telegram service")
            return Result.success()
        }
        val token = TelegramTokenStore(applicationContext, userId).getToken()
        if (token.isBlank()) {
            Log.w("TelegramSync", "No bot token — set TELEGRAM_BOT_TOKEN in local.properties or Settings")
            return Result.success()
        }
        if (!TelegramPollCoordinator.isForegroundPolling()) {
            TelegramBotForegroundService.start(applicationContext)
        }
        return Result.success()
    }
}
