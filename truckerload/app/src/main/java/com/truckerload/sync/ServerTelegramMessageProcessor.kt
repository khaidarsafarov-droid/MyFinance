package com.truckerload.sync

import android.content.Context
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.Paycheck
import com.truckerload.domain.parser.MessageParseService
import com.truckerload.utils.formatDateFromUnixSeconds
import com.truckerload.utils.getWeekNumberAndYearFromDate
import com.truckerload.utils.getWeekRange

enum class ServerInboxProcessingResult {
    PROCESSED,
    IGNORED,
}

class ServerTelegramMessageProcessor(
    private val context: Context,
    db: AppDatabase,
    private val parser: MessageParseService = MessageParseService(),
) {
    private val loadRepository = LoadRepository(db)
    private val paycheckRepository = PaycheckRepository(db)
    private val dieselRepository = DieselRepository(db)
    private val loadHandler = TelegramLoadHandler(
        context = context,
        loadRepository = loadRepository,
        settingsDataStore = SettingsDataStore(context),
    )

    suspend fun process(text: String, receivedAtMillis: Long): ServerInboxProcessingResult {
        if (shouldIgnore(text)) return ServerInboxProcessingResult.IGNORED
        val messageDateSeconds = receivedAtMillis.takeIf { it > 0 }?.div(1000)

        val loads = parser.parseLoadsFromMessage(text).getOrNull().orEmpty()
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
            if (parsed.netAmount <= 0) return ServerInboxProcessingResult.IGNORED
            val (weekNumber, year) =
                if (messageDateSeconds != null && parsed.weekStartDate.isNullOrBlank()) {
                    getWeekNumberAndYearFromDate(formatDateFromUnixSeconds(messageDateSeconds))
                } else {
                    getWeekNumberAndYearFromDate(parsed.weekStartDate)
                }
            if (paycheckRepository.getPaycheckForWeek(weekNumber, year) == null) {
                val (weekStart, weekEnd, weekLabel) = getWeekRange(weekNumber, year)
                paycheckRepository.insertPaycheck(
                    Paycheck(
                        id = 0,
                        weekNumber = weekNumber,
                        year = year,
                        weekLabel = weekLabel,
                        weekStartDate = weekStart,
                        weekEndDate = weekEnd,
                        driverName = parsed.driverName,
                        grossAmount = parsed.grossAmount,
                        netAmount = parsed.netAmount,
                        rawExtractedText = text,
                        sourceFileName = null,
                        addedAt = System.currentTimeMillis(),
                    ),
                )
            }
            return ServerInboxProcessingResult.PROCESSED
        }

        parser.parseDieselFromText(text).getOrNull()?.let { parsed ->
            if (parsed.totalAmount <= 0) return ServerInboxProcessingResult.IGNORED
            val textHash = text.hashCode()
            val duplicate = dieselRepository.getAllDieselOnce().any {
                it.rawExtractedText.hashCode() == textHash
            }
            if (!duplicate) {
                val dateForWeek =
                    if (messageDateSeconds != null && parsed.date.isNullOrBlank()) {
                        formatDateFromUnixSeconds(messageDateSeconds)
                    } else {
                        parsed.date
                    }
                val (weekNumber, year) = getWeekNumberAndYearFromDate(dateForWeek)
                val (weekStart, weekEnd, weekLabel) = getWeekRange(weekNumber, year)
                dieselRepository.insertDiesel(
                    Diesel(
                        id = 0,
                        weekNumber = weekNumber,
                        year = year,
                        weekLabel = weekLabel,
                        weekStartDate = weekStart,
                        weekEndDate = weekEnd,
                        totalAmount = parsed.totalAmount,
                        gallons = parsed.gallons,
                        pricePerGallon = parsed.pricePerGallon,
                        location = parsed.location,
                        rawExtractedText = text,
                        sourceFileName = null,
                        addedAt = System.currentTimeMillis(),
                    ),
                )
            }
            return ServerInboxProcessingResult.PROCESSED
        }

        return ServerInboxProcessingResult.IGNORED
    }

    companion object {
        fun shouldIgnore(text: String): Boolean {
            val trimmed = text.trim()
            return trimmed.isBlank() || trimmed.startsWith("/")
        }
    }
}
