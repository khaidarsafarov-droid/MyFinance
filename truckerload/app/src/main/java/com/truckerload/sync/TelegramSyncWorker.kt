package com.truckerload.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.TelegramTokenStore
import com.truckerload.data.remote.TelegramBotBranding
import com.truckerload.utils.LogRedactor
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/**
 * Background Telegram poll via WorkManager.
 * One long-poll per run, then the next run is queued so texts and files still
 * arrive after the app leaves the screen. Skips the network call when the
 * in-process poller already holds the connection.
 */
@HiltWorker
class TelegramSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val authStore: AuthStore,
) : CoroutineWorker(context, params) {

    companion object {
        const val UNIQUE_ENSURE_SERVICE_WORK = "telegram_sync_ensure_service"
        const val UNIQUE_FOLLOW_UP_WORK = "telegram_sync_followup"
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

        /** Queue the next poll. A short delay while the in-process poller is already running. */
        fun enqueueFollowUp(context: Context) {
            val app = context.applicationContext
            val delaySec = if (TelegramBotPoller.isRunning()) 45L else 3L
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<TelegramSyncWorker>()
                .setConstraints(constraints)
                .setInitialDelay(delaySec, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(app).enqueueUniqueWork(
                UNIQUE_FOLLOW_UP_WORK,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request,
            )
        }
    }

    override suspend fun doWork(): Result {
        if (!com.truckerload.data.premium.PremiumAccess.get(applicationContext).isUnlocked()) {
            Log.d("TelegramSync", "Premium locked — skip background poll")
            return Result.success()
        }
        val userId = authStore.currentUserIdOrNull()
        if (userId.isNullOrBlank()) {
            Log.w("TelegramSync", "No active user — skip background poll")
            return Result.success()
        }
        val token = TelegramTokenStore(applicationContext, userId).getToken()
        if (token.isBlank()) {
            Log.w("TelegramSync", "No bot token — skip background poll")
            return Result.success()
        }
        runCatching { TelegramBotBranding.apply(applicationContext, token) }
        if (TelegramBotPoller.isRunning()) {
            Log.d("TelegramSync", "In-process poller is active — schedule the next check")
            enqueueFollowUp(applicationContext)
            return Result.success()
        }
        try {
            val engine = TelegramBotSyncEngine(applicationContext)
            val result = engine.runOnce(token, expectedUserId = userId)
            if (result.pollLockContention) {
                Log.d("TelegramSync", "Poll lock contention — retry background poll")
                return Result.retry()
            }
            Log.d("TelegramSync", "Background poll completed")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("TelegramSync", "Background poll failed", LogRedactor.redactThrowable(e))
        }
        enqueueFollowUp(applicationContext)
        return Result.success()
    }
}
