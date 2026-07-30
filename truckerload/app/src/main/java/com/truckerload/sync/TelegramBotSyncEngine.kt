package com.truckerload.sync

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.truckerload.R
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.AccountIds
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.sync.telegram.TelegramApiClient
import com.truckerload.sync.telegram.TelegramMessageParser
import com.truckerload.sync.telegram.TelegramStateMachine
import com.truckerload.sync.telegram.TelegramSyncScheduler
import com.truckerload.sync.telegram.TelegramUpdateDispatcher
import com.truckerload.utils.LogRedactor
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Coordinator for Telegram long-poll + parse + DB + reply.
 *
 * Polling/backoff: [TelegramSyncScheduler] + [TelegramStateMachine]
 * API calls: [TelegramApiClient]
 * Text parse: [TelegramMessageParser]
 * Update routing: [TelegramUpdateDispatcher]
 */
class TelegramBotSyncEngine(private val context: Context) {

    private val messageArchive = TelegramMessageArchive(context)
    private val scheduler = TelegramSyncScheduler()
    private val stateMachine = TelegramStateMachine()
    private val messageParser = TelegramMessageParser(context)
    private val dispatcher = TelegramUpdateDispatcher(context, messageParser)

    suspend fun runOnce(token: String): SyncRunResult {
        if (token.isBlank()) {
            return SyncRunResult(skipped = true, processedUpdates = 0, nextDelaySeconds = 60)
        }
        stateMachine.beginPoll()
        val result = TelegramPollCoordinator.withPollLock {
            runOnceLocked(token)
        }
        if (result == null) {
            stateMachine.idle()
            return SyncRunResult(skipped = true, processedUpdates = 0, nextDelaySeconds = 15)
        }
        return result
    }

    private suspend fun runOnceLocked(token: String): SyncRunResult {
        val userId = AuthStore(context).currentUserIdOrNull()
        if (userId.isNullOrBlank()) {
            Log.w(TAG, "No active user session — skip Telegram sync")
            stateMachine.idle()
            return SyncRunResult(skipped = true, processedUpdates = 0, nextDelaySeconds = 60)
        }
        stateMachine.beginSync()
        val prefs = telegramSyncPrefs(context, userId)
        val settingsDataStore = SettingsDataStore(context)
        var nextRequestOffset = scheduler.loadNextRequestOffset(prefs, settingsDataStore)
        Log.d(TAG, "📥 Last update offset (next request): $nextRequestOffset user=$userId")

        val apiClient = TelegramApiClient(token)
        val telegramApi = apiClient.asTelegramApi()

        val db = AppDatabase.getInstance(context, userId)
        val loadRepository = LoadRepository(db)
        val paycheckRepository = PaycheckRepository(db)
        val dieselRepository = DieselRepository(db)
        val chatRestore = TelegramChatRestore(db.telegramInboxDao(), messageArchive)

        val result = apiClient.getUpdates(
            offset = nextRequestOffset.takeIf { it > 0L },
            timeoutSeconds = 25,
        ).getOrElse { e ->
            Log.e(TAG, "getUpdates failed: ${LogRedactor.redact(e.message)}", e)
            val delay = scheduler.delayAfterGetUpdatesFailure(e.message)
            if (TelegramAuthErrors.shouldStopService(e.message)) {
                TelegramBotForegroundService.stop(context)
            }
            stateMachine.fail(e.message)
            stateMachine.idle()
            return SyncRunResult(
                skipped = false,
                processedUpdates = 0,
                nextDelaySeconds = delay,
                error = LogRedactor.redact(e.message),
            )
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
                    telegramApi = telegramApi,
                    loadRepository = loadRepository,
                    paycheckRepository = paycheckRepository,
                    dieselRepository = dieselRepository,
                    chatRestore = chatRestore,
                    prefs = prefs,
                )
                nextRequestOffset = update.updateId + 1
                scheduler.persistNextRequestOffset(prefs, settingsDataStore, nextRequestOffset)
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "handleUpdate failed for updateId=${update.updateId}; offset NOT advanced: ${LogRedactor.redact(e.message)}",
                    e,
                )
                stoppedOnFailure = true
                stateMachine.fail(e.message)
                break
            }
        }

        if (!stoppedOnFailure && result.nextOffset > nextRequestOffset) {
            nextRequestOffset = result.nextOffset
            scheduler.persistNextRequestOffset(prefs, settingsDataStore, nextRequestOffset)
        }

        val nextDelay = scheduler.nextDelayAfterPoll(processed, result.updates.size)
        Log.d(TAG, "📥 runOnce done processed=$processed nextOffset=$nextRequestOffset")
        stateMachine.idle()
        return SyncRunResult(skipped = false, processedUpdates = processed, nextDelaySeconds = nextDelay)
    }

    data class SyncRunResult(
        val skipped: Boolean,
        val processedUpdates: Int,
        val nextDelaySeconds: Long,
        val error: String? = null,
    )

    companion object {
        private const val TAG = "TelegramBotSync"
        private val exportCaptionDate = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US)

        fun telegramSyncPrefs(context: Context, userId: String): SharedPreferences {
            val name = "telegram_sync_${AccountIds.sanitizeFilePart(userId)}"
            val scoped = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            val meta = context.getSharedPreferences("truckerload_account_meta", Context.MODE_PRIVATE)
            val migrated = meta.getBoolean("legacy_telegram_offset_migrated", false)
            if (!migrated && !scoped.contains(TelegramSyncWorker.KEY_LAST_OFFSET)) {
                val legacy = context.getSharedPreferences(TelegramSyncWorker.PREFS_NAME, Context.MODE_PRIVATE)
                val offset = legacy.getLong(TelegramSyncWorker.KEY_LAST_OFFSET, 0L)
                if (offset > 0L) {
                    scoped.edit(commit = true) {
                        putLong(TelegramSyncWorker.KEY_LAST_OFFSET, offset)
                    }
                }
                meta.edit().putBoolean("legacy_telegram_offset_migrated", true).apply()
            }
            return scoped
        }

        suspend fun sendFileToTelegram(
            context: Context,
            token: String,
            chatId: Long,
            file: File,
        ): Result<Unit> {
            val caption = context.getString(
                R.string.telegram_export_caption,
                exportCaptionDate.format(Date()),
            )
            return TelegramApiClient(token).sendDocument(chatId.toString(), file, caption)
        }
    }
}
