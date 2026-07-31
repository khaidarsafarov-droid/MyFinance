package com.truckerload.sync

import android.content.Context
import android.util.Log
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
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

    suspend fun runOnce(token: String): SyncRunResult {
        if (token.isBlank()) {
            return SyncRunResult(skipped = true, processedUpdates = 0, nextDelaySeconds = 60)
        }
        val result = TelegramPollCoordinator.withPollLock {
            runOnceLocked(token)
        }
        return result ?: SyncRunResult(skipped = true, processedUpdates = 0, nextDelaySeconds = 15)
    }

    private suspend fun runOnceLocked(token: String): SyncRunResult {
        val userId = AuthStore(context).currentUserIdOrNull()
        if (userId.isNullOrBlank()) {
            Log.w(TAG, "No active user session — skip Telegram sync")
            return SyncRunResult(skipped = true, processedUpdates = 0, nextDelaySeconds = 60)
        }
        val prefs = syncScheduler.prefsForUser(userId)
        val settingsDataStore = SettingsDataStore(context)
        var nextRequestOffset = syncScheduler.loadNextRequestOffset(prefs, settingsDataStore)
        Log.d(TAG, "📥 Last update offset (next request): $nextRequestOffset user=$userId")

        val apiClient = TelegramApiClient.create(context, token)
        val dispatcher = TelegramUpdateDispatcher(context, apiClient, messageParser)
        val stateMachine = TelegramStateMachine(prefs)

        val db = AppDatabase.getInstance(context, userId)
        val loadRepository = LoadRepository(db)
        val paycheckRepository = PaycheckRepository(db)
        val dieselRepository = DieselRepository(db)
        val chatRestore = TelegramChatRestore(db.telegramInboxDao(), TelegramMessageArchive(context))

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
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "handleUpdate failed for updateId=${update.updateId}; offset NOT advanced: ${LogRedactor.redact(e.message)}",
                    e,
                )
                // Stop this poll cycle so the failed update is retried next run.
                // Do NOT jump to result.nextOffset — that would skip the failed update.
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

        fun telegramSyncPrefs(context: Context, userId: String) =
            TelegramSyncScheduler.telegramSyncPrefs(context, userId)

        suspend fun sendFileToTelegram(
            context: Context,
            token: String,
            chatId: Long,
            file: File,
        ): Result<Unit> = TelegramApiClient.sendFileToTelegram(context, token, chatId, file)
    }
}
