package com.truckerload.sync

import android.content.Context
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.domain.parser.MessageParseService
import com.truckerload.sync.telegram.TelegramJournalIngest
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
        settingsDataStore = SettingsDataStore(context),
    )

    suspend fun process(text: String, receivedAtMillis: Long): ServerInboxProcessingResult {
        if (shouldIgnore(text)) return ServerInboxProcessingResult.IGNORED
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

    companion object {
        fun shouldIgnore(text: String): Boolean {
            val trimmed = text.trim()
            return trimmed.isBlank() || trimmed.startsWith("/")
        }

        /** Kept for compatibility; prefer [TelegramTextFingerprint.sha256Hex]. */
        fun stableTextHash(text: String): String = TelegramTextFingerprint.sha256Hex(text)
    }
}
