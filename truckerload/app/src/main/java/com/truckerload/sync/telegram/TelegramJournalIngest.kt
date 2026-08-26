package com.truckerload.sync.telegram

import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.Paycheck
import com.truckerload.utils.formatDateFromUnixSeconds
import com.truckerload.utils.getWeekNumberAndYearFromDate
import com.truckerload.utils.getWeekRange

/**
 * Shared paycheck/diesel Room writes for device Telegram bot and server inbox.
 * UI strings stay in callers; this layer returns structured outcomes only.
 */
class TelegramJournalIngest(
    private val paycheckRepository: PaycheckRepository,
    private val dieselRepository: DieselRepository,
) {
    sealed class PaycheckOutcome {
        data class Inserted(val weekNumber: Int, val year: Int, val netAmount: Double) : PaycheckOutcome()
        data class AlreadyExists(val weekNumber: Int, val year: Int) : PaycheckOutcome()
        data object InvalidAmount : PaycheckOutcome()
    }

    sealed class DieselOutcome {
        data class Inserted(val weekNumber: Int, val year: Int, val totalAmount: Double) : DieselOutcome()
        data object Duplicate : DieselOutcome()
        data object InvalidAmount : DieselOutcome()
    }

    suspend fun insertPaycheck(
        netAmount: Double,
        grossAmount: Double?,
        driverName: String?,
        weekStartDateHint: String?,
        messageDateSeconds: Long?,
        rawText: String,
        sourceFileName: String? = null,
        sourceFilePath: String? = null,
        addedAt: Long = System.currentTimeMillis(),
    ): PaycheckOutcome {
        if (netAmount <= 0) return PaycheckOutcome.InvalidAmount
        val (weekNumber, year) = resolveWeek(weekStartDateHint, messageDateSeconds)
        if (paycheckRepository.getPaycheckForWeek(weekNumber, year) != null) {
            return PaycheckOutcome.AlreadyExists(weekNumber, year)
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
                driverName = driverName,
                grossAmount = grossAmount,
                netAmount = netAmount,
                rawExtractedText = rawText,
                sourceFileName = sourceFileName,
                addedAt = addedAt,
                sourceFilePath = sourceFilePath,
            ),
        )
        return PaycheckOutcome.Inserted(weekNumber, year, netAmount)
    }

    suspend fun insertDiesel(
        totalAmount: Double,
        gallons: Double?,
        pricePerGallon: Double?,
        location: String?,
        dateHint: String?,
        messageDateSeconds: Long?,
        rawText: String,
        sourceFileName: String? = null,
        fingerprintSalt: String? = null,
        addedAt: Long = System.currentTimeMillis(),
    ): DieselOutcome {
        if (totalAmount <= 0) return DieselOutcome.InvalidAmount
        val fingerprint = TelegramTextFingerprint.dieselFingerprint(rawText, fingerprintSalt)
        val duplicate = dieselRepository.getAllDieselOnce().any { existing ->
            TelegramTextFingerprint.dieselFingerprint(existing.rawExtractedText, fingerprintSalt) == fingerprint
        }
        if (duplicate) return DieselOutcome.Duplicate

        val (weekNumber, year) = resolveWeek(dateHint, messageDateSeconds)
        val (weekStart, weekEnd, weekLabel) = getWeekRange(weekNumber, year)
        dieselRepository.insertDiesel(
            Diesel(
                id = 0,
                weekNumber = weekNumber,
                year = year,
                weekLabel = weekLabel,
                weekStartDate = weekStart,
                weekEndDate = weekEnd,
                totalAmount = totalAmount,
                gallons = gallons,
                pricePerGallon = pricePerGallon,
                location = location,
                rawExtractedText = rawText,
                sourceFileName = sourceFileName,
                addedAt = addedAt,
            ),
        )
        return DieselOutcome.Inserted(weekNumber, year, totalAmount)
    }

    private fun resolveWeek(dateHint: String?, messageDateSeconds: Long?): Pair<Int, Int> {
        val dateForWeek = when {
            !dateHint.isNullOrBlank() -> dateHint
            messageDateSeconds != null -> formatDateFromUnixSeconds(messageDateSeconds)
            else -> null
        }
        return getWeekNumberAndYearFromDate(dateForWeek)
    }
}
