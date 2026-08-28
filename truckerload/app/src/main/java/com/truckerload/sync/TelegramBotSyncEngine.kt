package com.truckerload.sync

import android.content.Context
import android.util.Log
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.di.userComponentManager
import com.truckerload.sync.telegram.TelegramApiClient
import com.truckerload.sync.telegram.TelegramMessageParser
import com.truckerload.sync.telegram.TelegramStateMachine
import com.truckerload.sync.telegram.TelegramSyncRunResult
import com.truckerload.sync.telegram.TelegramSyncScheduler
import com.truckerload.sync.telegram.TelegramUpdateDispatcher
import com.truckerload.utils.LogRedactor
import java.io.File

/**
 * Shared Telegram long-poll + parse + DB + reply coordinator for Worker and ForegroundService.
 *
 * Facade over [TelegramApiClient], [TelegramMessageParser], [TelegramSyncScheduler],
 * [TelegramStateMachine], and [TelegramUpdateDispatcher].
 */
class TelegramBotSyncEngine(private val context: Context) {

    private val messageParser = TelegramMessageParser(context)
    private val syncScheduler = TelegramSyncScheduler(context)

    suspend fun runOnce(token: String, expectedUserId: String? = null): SyncRunResult {
        if (token.isBlank()) {
            return SyncRunResult(skipped = true, processedUpdates = 0, nextDelaySeconds = 60)
        }
        val result = TelegramPollCoordinator.withPollLock {
            runOnceLocked(token, expectedUserId)
        }
        return result ?: SyncRunResult(skipped = true, processedUpdates = 0, nextDelaySeconds = 15)
    }

    private suspend fun runOnceLocked(token: String, expectedUserId: String?): SyncRunResult {
        val userId = AuthStore(context).currentUserIdOrNull()
        if (userId.isNullOrBlank()) {
            Log.w(TAG, "No active user session — skip Telegram sync")
            return SyncRunResult(skipped = true, processedUpdates = 0, nextDelaySeconds = 60)
        }
        // FIX: refuse to write when poller account ≠ current session
        if (expectedUserId != null && expectedUserId != userId) {
            Log.w(TAG, "Session user mismatch expected=$expectedUserId active=$userId — skip")
            return SyncRunResult(skipped = true, processedUpdates = 0, nextDelaySeconds = 5)
        }
        val prefs = syncScheduler.prefsForUser(userId)
        val settingsDataStore = SettingsDataStore(context)
        var nextRequestOffset = syncScheduler.loadNextRequestOffset(prefs, settingsDataStore)
        Log.d(TAG, "📥 Last update offset (next request): $nextRequestOffset user=$userId")

        val apiClient = TelegramApiClient.create(context, token)
        val dispatcher = TelegramUpdateDispatcher(context, apiClient, messageParser)
        val stateMachine = TelegramStateMachine(prefs)

        // FIX: use session-scoped graph — same DB as ViewModels, survives account binding correctly
        val userComponent = context.userComponentManager().startSession(userId)
        val db = userComponent.database
        val loadRepository = userComponent.loadRepository
        val paycheckRepository = userComponent.paycheckRepository
        val dieselRepository = userComponent.dieselRepository
        val chatRestore = TelegramChatRestore(db.telegramInboxDao(), TelegramMessageArchive(context, userId))

        val result = apiClient.getUpdates(
            offset = nextRequestOffset.takeIf { it > 0L },
            timeoutSeconds = 25,
        ).getOrElse { e ->
            return apiClient.mapGetUpdatesFailure(e).toPublic()
        }

        Log.d(TAG, "📥 Received ${result.updates.size} updates (rawMax=${result.rawMaxUpdateId})")

        var processed = 0
        var stoppedOnFailure = false
        for (update in result.updates) {
            if (update.updateId + 1 <= nextRequestOffset) {
                Log.d(TAG, "⏭️ Skipping already processed updateId=${update.updateId}")
                continue
            }
            processed++
            Log.d(TAG, "📥 Processing updateId=${update.updateId}")
            try {
                dispatcher.handleUpdate(
                    update = update,
                    loadRepository = loadRepository,
                    paycheckRepository = paycheckRepository,
                    dieselRepository = dieselRepository,
                    chatRestore = chatRestore,
                    prefs = prefs,
                    stateMachine = stateMachine,
                )
                nextRequestOffset = update.updateId + 1
                syncScheduler.persistNextRequestOffset(prefs, settingsDataStore, nextRequestOffset)
                clearPoisonFailures(prefs, update.updateId)
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "handleUpdate failed for updateId=${update.updateId}: ${LogRedactor.redact(e.message)}",
                    e,
                )
                // FIX: after N failures, dead-letter and advance so one poison update cannot stall forever
                val failures = incrementPoisonFailures(prefs, update.updateId)
                if (failures >= MAX_POISON_RETRIES) {
                    Log.e(TAG, "Dead-letter updateId=${update.updateId} after $failures failures")
                    nextRequestOffset = update.updateId + 1
                    syncScheduler.persistNextRequestOffset(prefs, settingsDataStore, nextRequestOffset)
                    clearPoisonFailures(prefs, update.updateId)
                    runCatching {
                        apiClient.sendMessage(
                            update.chatId,
                            context.getString(com.truckerload.R.string.sync_update_skipped_error),
                        )
                    }
                    continue
                }
                stoppedOnFailure = true
                break
            }
        }

        if (!stoppedOnFailure && result.nextOffset > nextRequestOffset) {
            nextRequestOffset = result.nextOffset
            syncScheduler.persistNextRequestOffset(prefs, settingsDataStore, nextRequestOffset)
        }

        val nextDelay = syncScheduler.nextDelaySeconds(processed, result.updates.isNotEmpty())
        Log.d(TAG, "📥 runOnce done processed=$processed nextOffset=$nextRequestOffset")
        return SyncRunResult(skipped = false, processedUpdates = processed, nextDelaySeconds = nextDelay)
    }

    data class SyncRunResult(
        val skipped: Boolean,
        val processedUpdates: Int,
        val nextDelaySeconds: Long,
        val error: String? = null,
    )

    private fun TelegramSyncRunResult.toPublic() = SyncRunResult(
        skipped = skipped,
        processedUpdates = processedUpdates,
        nextDelaySeconds = nextDelaySeconds,
        error = error,
    )

    companion object {
        private const val TAG = "TelegramBotSync"
        private const val MAX_POISON_RETRIES = 3
        private const val KEY_POISON_PREFIX = "poison_fail_"

        fun telegramSyncPrefs(context: Context, userId: String) =
            TelegramSyncScheduler.telegramSyncPrefs(context, userId)

        suspend fun sendFileToTelegram(
            context: Context,
            token: String,
            chatId: Long,
            file: File,
        ): Result<Unit> = TelegramApiClient.sendFileToTelegram(context, token, chatId, file)

        private fun incrementPoisonFailures(prefs: android.content.SharedPreferences, updateId: Long): Int {
            val key = KEY_POISON_PREFIX + updateId
            val next = prefs.getInt(key, 0) + 1
            prefs.edit().putInt(key, next).apply()
            return next
        }

        private fun clearPoisonFailures(prefs: android.content.SharedPreferences, updateId: Long) {
            prefs.edit().remove(KEY_POISON_PREFIX + updateId).apply()
        }
    }
}
