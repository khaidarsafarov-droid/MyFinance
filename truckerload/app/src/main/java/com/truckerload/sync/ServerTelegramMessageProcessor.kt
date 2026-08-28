package com.truckerload.sync

import android.content.Context
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.remote.TelegramBotFeatures
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.domain.parser.MessageParseService
import com.truckerload.sync.import.ImportCommandHandler
import com.truckerload.sync.import.ImportSessionManager
import com.truckerload.sync.telegram.TelegramJournalIngest
import com.truckerload.sync.telegram.TelegramStateMachine
import com.truckerload.sync.telegram.TelegramTextFingerprint

enum class ServerInboxProcessingResult {
    PROCESSED,
    IGNORED,
}

/**
 * Server-push inbox path. Paycheck/diesel inserts go through [TelegramJournalIngest]
 * (same rules as the on-device Telegram bot: week dedupe + SHA-256 diesel fingerprint).
 */
class ServerTelegramMessageProcessor(
    private val context: Context,
    db: AppDatabase,
    private val parser: MessageParseService = MessageParseService(),
) {
    private val loadRepository = LoadRepository(db)
    private val paycheckRepository = PaycheckRepository(db)
    private val dieselRepository = DieselRepository(db)
    private val journalIngest = TelegramJournalIngest(paycheckRepository, dieselRepository)
    private val loadHandler = TelegramLoadHandler(
        context = context,
        loadRepository = loadRepository,
        settingsDataStore = com.truckerload.data.preferences.SettingsDataStore(context),
    )

    suspend fun process(
        text: String,
        receivedAtMillis: Long,
        chatId: Long,
    ): ServerInboxProcessingResult {
        if (shouldIgnore(text)) return ServerInboxProcessingResult.IGNORED
        handleSlashCommand(text, chatId)?.let { return it }
        val messageDateSeconds = receivedAtMillis.takeIf { it > 0 }?.div(1000)

        val loads = parser.parseLoadsFromMessage(
            text,
            messageDateSeconds?.times(1000) ?: System.currentTimeMillis(),
        ).getOrNull().orEmpty()
        if (loads.isNotEmpty()) {
            loadHandler.handleLoads(
                loads = loads,
                rawMessage = text,
                messageDateSeconds = messageDateSeconds,
                playFeedback = false,
            )
            return ServerInboxProcessingResult.PROCESSED
        }

        parser.parsePaycheckFromText(text).getOrNull()?.let { parsed ->
            val outcome = journalIngest.insertPaycheck(
                netAmount = parsed.netAmount,
                grossAmount = parsed.grossAmount,
                driverName = parsed.driverName,
                weekStartDateHint = parsed.weekStartDate,
                messageDateSeconds = messageDateSeconds,
                rawText = text,
            )
            return when (outcome) {
                TelegramJournalIngest.PaycheckOutcome.InvalidAmount ->
                    ServerInboxProcessingResult.IGNORED
                is TelegramJournalIngest.PaycheckOutcome.AlreadyExists,
                is TelegramJournalIngest.PaycheckOutcome.Inserted,
                -> ServerInboxProcessingResult.PROCESSED
            }
        }

        parser.parseDieselFromText(text).getOrNull()?.let { parsed ->
            val outcome = journalIngest.insertDiesel(
                totalAmount = parsed.totalAmount,
                gallons = parsed.gallons,
                pricePerGallon = parsed.pricePerGallon,
                location = parsed.location,
                dateHint = parsed.date,
                messageDateSeconds = messageDateSeconds,
                rawText = text,
            )
            return when (outcome) {
                TelegramJournalIngest.DieselOutcome.InvalidAmount ->
                    ServerInboxProcessingResult.IGNORED
                TelegramJournalIngest.DieselOutcome.Duplicate,
                is TelegramJournalIngest.DieselOutcome.Inserted,
                -> ServerInboxProcessingResult.PROCESSED
            }
        }

        return ServerInboxProcessingResult.IGNORED
    }

    /**
     * Activates import/restore/cancel sessions for server-mode inbox (no live bot replies).
     */
    private fun handleSlashCommand(text: String, chatId: Long): ServerInboxProcessingResult? {
        var raw = text.trim()
        if (TelegramBotFeatures.isMenuButtonText(raw)) {
            raw = TelegramBotFeatures.menuButtonToCommand(raw) ?: raw
        }
        raw = TelegramBotFeatures.aliasCommand(raw)
        if (!raw.startsWith("/")) return null

        val userId = AuthStore(context).currentUserIdOrNull() ?: return ServerInboxProcessingResult.IGNORED
        val prefs = TelegramBotSyncEngine.telegramSyncPrefs(context, userId)
        val stateMachine = TelegramStateMachine(prefs)
        val chatKey = chatId.toString()

        return when {
            isCommand(raw, "/import") -> {
                if (stateMachine.isManualRestoreActive(chatKey)) {
                    stateMachine.clearManualRestore(chatKey)
                }
                ImportCommandHandler(context, ImportSessionManager(prefs))
                    .startImport(chatKey) { id -> stateMachine.clearManualRestore(id) }
                ServerInboxProcessingResult.PROCESSED
            }
            TelegramBotFeatures.isRestoreRequest(raw) || isCommand(raw, "/restore") -> {
                ImportSessionManager(prefs).endSession(chatKey)
                stateMachine.startManualRestore(chatKey)
                ServerInboxProcessingResult.PROCESSED
            }
            isCommand(raw, "/cancel") -> {
                ImportSessionManager(prefs).cancelSession(chatKey)
                if (stateMachine.isManualRestoreActive(chatKey)) {
                    stateMachine.clearManualRestore(chatKey)
                }
                ServerInboxProcessingResult.PROCESSED
            }
            isCommand(raw, "/help") ||
                isCommand(raw, "/status") ||
                isCommand(raw, "/stats") ||
                isCommand(raw, "/help_load") ||
                isCommand(raw, "/help_pay") ||
                isCommand(raw, "/start") ||
                isCommand(raw, "/dedup") ->
                ServerInboxProcessingResult.PROCESSED
            else -> null
        }
    }

    companion object {
        fun shouldIgnore(text: String): Boolean = text.isBlank()

        /** Kept for compatibility; prefer [TelegramTextFingerprint.sha256Hex]. */
        fun stableTextHash(text: String): String = TelegramTextFingerprint.sha256Hex(text)

        private fun isCommand(text: String, command: String): Boolean =
            text.equals(command, ignoreCase = true) ||
                text.startsWith("$command ", ignoreCase = true) ||
                text.startsWith("$command@", ignoreCase = true)
    }
}
