package com.truckerload.sync.telegram

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.truckerload.R
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.remote.TelegramUpdate
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.Paycheck
import com.truckerload.domain.parser.MessageClassifier
import com.truckerload.domain.parser.MessageParseService
import com.truckerload.domain.parser.ProcessingResult
import com.truckerload.sync.ManualRestoreModeStore
import com.truckerload.sync.TelegramLoadHandler
import com.truckerload.utils.FeedbackManager
import com.truckerload.utils.formatDateFromUnixSeconds
import com.truckerload.utils.getWeekNumberAndYearFromDate
import com.truckerload.utils.getWeekRange
import java.util.Locale

/**
 * Parse inbound Telegram text into loads / paycheck / diesel and produce reply strings.
 */
class TelegramMessageParser(
    private val context: Context,
    private val messageParseService: MessageParseService = MessageParseService(),
    private val settingsDataStore: SettingsDataStore = SettingsDataStore(context),
) {
    fun isCommand(text: String, command: String): Boolean {
        val trimmed = text.trim()
        return trimmed.equals(command, ignoreCase = true) ||
            trimmed.startsWith("$command@", ignoreCase = true) ||
            trimmed.startsWith("$command ", ignoreCase = true)
    }

    fun isExportTextDocument(update: TelegramUpdate): Boolean {
        val name = update.documentFileName?.lowercase(Locale.US).orEmpty()
        val mime = update.documentMimeType?.lowercase(Locale.US).orEmpty()
        return name.endsWith(".txt") ||
            mime == "text/plain" ||
            name.contains("trucklog_export") ||
            name.contains("truckerload_export")
    }

    fun loadHandler(loadRepository: LoadRepository): TelegramLoadHandler =
        TelegramLoadHandler(
            context = context,
            loadRepository = loadRepository,
            messageParseService = messageParseService,
            settingsDataStore = settingsDataStore,
        )

    suspend fun processMessage(
        text: String,
        messageDateSeconds: Long?,
        loadRepository: LoadRepository,
        paycheckRepository: PaycheckRepository,
        dieselRepository: DieselRepository,
        prefs: SharedPreferences,
    ): String {
        messageParseService.parseLoadsFromMessage(text)
            .onSuccess { incomingLoads ->
                if (incomingLoads.isNotEmpty()) {
                    return loadHandler(loadRepository).handleLoads(
                        loads = incomingLoads,
                        rawMessage = text,
                        messageDateSeconds = messageDateSeconds,
                    )
                }
            }

        messageParseService.parsePaycheckFromText(text)
            .onSuccess { r ->
                val (weekNumber, year) = if (messageDateSeconds != null && r.weekStartDate.isNullOrBlank()) {
                    getWeekNumberAndYearFromDate(formatDateFromUnixSeconds(messageDateSeconds))
                } else {
                    getWeekNumberAndYearFromDate(r.weekStartDate)
                }
                if (r.netAmount <= 0) return context.getString(R.string.sync_paycheck_not_found)
                if (paycheckRepository.getPaycheckForWeek(weekNumber, year) != null) {
                    return context.getString(R.string.sync_paycheck_exists, weekNumber)
                }
                val (weekStart, weekEnd, weekLabel) = getWeekRange(weekNumber, year)
                paycheckRepository.insertPaycheck(
                    Paycheck(
                        id = 0,
                        weekNumber = weekNumber,
                        year = year,
                        weekLabel = weekLabel,
                        weekStartDate = weekStart,
                        weekEndDate = weekEnd,
                        driverName = r.driverName,
                        grossAmount = r.grossAmount,
                        netAmount = r.netAmount,
                        rawExtractedText = text,
                        sourceFileName = null,
                        addedAt = System.currentTimeMillis(),
                    ),
                )
                return context.getString(
                    R.string.sync_last_paycheck,
                    String.format(Locale.US, "%,.2f", r.netAmount),
                    weekNumber,
                )
            }

        messageParseService.parseDieselFromText(text)
            .onSuccess { r ->
                val dateForWeek = when {
                    messageDateSeconds != null && r.date.isNullOrBlank() ->
                        formatDateFromUnixSeconds(messageDateSeconds)
                    else -> r.date
                }
                val (weekNumber, year) = getWeekNumberAndYearFromDate(dateForWeek)
                if (r.totalAmount <= 0) return context.getString(R.string.sync_diesel_not_found)
                val textHash = text.hashCode()
                val lastDieselHash = prefs.getInt("last_diesel_text_hash", 0)
                if (lastDieselHash != 0 && lastDieselHash == textHash) {
                    return context.getString(R.string.sync_duplicate_diesel)
                }
                val (weekStart, weekEnd, weekLabel) = getWeekRange(weekNumber, year)
                dieselRepository.insertDiesel(
                    Diesel(
                        id = 0,
                        weekNumber = weekNumber,
                        year = year,
                        weekLabel = weekLabel,
                        weekStartDate = weekStart,
                        weekEndDate = weekEnd,
                        totalAmount = r.totalAmount,
                        gallons = r.gallons,
                        pricePerGallon = r.pricePerGallon,
                        location = r.location,
                        rawExtractedText = text,
                        sourceFileName = null,
                        addedAt = System.currentTimeMillis(),
                    ),
                )
                prefs.edit { putInt("last_diesel_text_hash", textHash) }
                return context.getString(
                    R.string.sync_last_diesel,
                    String.format(Locale.US, "%,.2f", r.totalAmount),
                    weekNumber,
                )
            }

        return context.getString(R.string.sync_parse_failed)
    }

    suspend fun processManualRestoreMessage(
        text: String,
        messageDateSeconds: Long?,
        chatId: String,
        loadRepository: LoadRepository,
        prefs: SharedPreferences,
    ): String {
        if (!MessageClassifier.isLoadLike(text)) {
            return context.getString(R.string.sync_restore_manual_not_load)
        }

        val parsed = messageParseService.parseLoadsFromMessage(text).getOrNull().orEmpty()
        if (parsed.isEmpty()) {
            return context.getString(R.string.sync_restore_manual_not_load)
        }

        val handler = loadHandler(loadRepository)
        val results = handler.processLoadsStructured(
            loads = parsed,
            rawMessage = text,
            messageDateSeconds = messageDateSeconds,
            playFeedback = false,
        )

        val replies = mutableListOf<String>()
        var addedCount = 0
        val restoreStore = ManualRestoreModeStore(prefs)
        parsed.zip(results).forEach { (load, result) ->
            when (result) {
                is ProcessingResult.Added -> {
                    addedCount++
                    restoreStore.incrementCount(chatId)
                    replies.add(
                        context.getString(
                            R.string.sync_restore_manual_added,
                            load.tripId,
                            load.totalMiles,
                            String.format(Locale.US, "%,.2f", load.totalRate),
                        ),
                    )
                }
                is ProcessingResult.Updated,
                is ProcessingResult.Replaced -> {
                    addedCount++
                    restoreStore.incrementCount(chatId)
                    replies.add(handler.formatProcessingResult(result, load.tripId))
                }
                is ProcessingResult.Skipped -> {
                    replies.add(handler.formatProcessingResult(result, load.tripId))
                }
            }
        }

        if (addedCount > 0) {
            FeedbackManager.onLoadAdded()
        }

        return if (replies.isEmpty()) {
            context.getString(R.string.sync_no_new_data)
        } else {
            replies.joinToString("\n\n")
        }
    }
}
