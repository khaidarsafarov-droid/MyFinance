package com.truckerload.sync.telegram

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.truckerload.R
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.domain.ingest.ReceiptKind
import com.truckerload.domain.ingest.ReceiptPreview
import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.Paycheck
import com.truckerload.utils.formatDateFromUnixSeconds
import com.truckerload.utils.getWeekNumberAndYearFromDate
import com.truckerload.utils.getWeekRange
import java.util.Locale

class TelegramReceiptWriter(
    private val context: Context,
) {
    suspend fun save(
        kind: ReceiptKind,
        preview: ReceiptPreview,
        paycheckRepository: PaycheckRepository,
        dieselRepository: DieselRepository,
        prefs: SharedPreferences,
    ): String = when (kind) {
        ReceiptKind.PAYCHECK -> savePaycheck(preview, paycheckRepository)
        ReceiptKind.DIESEL, ReceiptKind.DEF -> saveFuel(kind, preview, dieselRepository, prefs)
        ReceiptKind.LOAD, ReceiptKind.UNKNOWN ->
            context.getString(R.string.sync_receipt_need_choice)
    }

    private suspend fun savePaycheck(
        preview: ReceiptPreview,
        paycheckRepository: PaycheckRepository,
    ): String {
        val amount = preview.amount ?: return context.getString(R.string.sync_paycheck_not_found)
        if (amount <= 0) return context.getString(R.string.sync_paycheck_not_found)
        val (weekNumber, year) = weekOf(preview)
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
                driverName = preview.driverName,
                grossAmount = null,
                netAmount = amount,
                rawExtractedText = preview.extractedText,
                sourceFileName = preview.sourceFileName,
                addedAt = System.currentTimeMillis(),
            ),
        )
        return context.getString(
            R.string.sync_last_paycheck,
            String.format(Locale.US, "%,.2f", amount),
            weekNumber,
        )
    }

    private suspend fun saveFuel(
        kind: ReceiptKind,
        preview: ReceiptPreview,
        dieselRepository: DieselRepository,
        prefs: SharedPreferences,
    ): String {
        val amount = preview.amount ?: return context.getString(R.string.sync_diesel_not_found)
        if (amount <= 0) return context.getString(R.string.sync_diesel_not_found)
        val fingerprint = (preview.extractedText + kind.name).hashCode()
        val last = prefs.getInt("last_diesel_text_hash", 0)
        if (last != 0 && last == fingerprint) {
            return context.getString(R.string.sync_duplicate_diesel)
        }
        val (weekNumber, year) = weekOf(preview)
        val (weekStart, weekEnd, weekLabel) = getWeekRange(weekNumber, year)
        val location = fuelLocation(kind, preview)
        dieselRepository.insertDiesel(
            Diesel(
                id = 0,
                weekNumber = weekNumber,
                year = year,
                weekLabel = weekLabel,
                weekStartDate = weekStart,
                weekEndDate = weekEnd,
                totalAmount = amount,
                gallons = preview.gallons,
                pricePerGallon = preview.pricePerGallon,
                location = location,
                rawExtractedText = preview.extractedText,
                sourceFileName = preview.sourceFileName,
                addedAt = System.currentTimeMillis(),
            ),
        )
        prefs.edit { putInt("last_diesel_text_hash", fingerprint) }
        val template = if (kind == ReceiptKind.DEF) {
            R.string.sync_last_def
        } else {
            R.string.sync_last_diesel
        }
        return context.getString(
            template,
            String.format(Locale.US, "%,.2f", amount),
            weekNumber,
        )
    }

    private fun weekOf(preview: ReceiptPreview): Pair<Int, Int> {
        val dateForWeek = when {
            !preview.date.isNullOrBlank() -> preview.date
            preview.messageDateSeconds != null -> formatDateFromUnixSeconds(preview.messageDateSeconds)
            else -> null
        }
        return getWeekNumberAndYearFromDate(dateForWeek)
    }

    private fun fuelLocation(kind: ReceiptKind, preview: ReceiptPreview): String? {
        val parts = buildList {
            if (kind == ReceiptKind.DEF) add("DEF")
            preview.location?.let { add(it) }
            preview.vendor?.let { vendor ->
                if (!preview.location.orEmpty().contains(vendor, ignoreCase = true)) add(vendor)
            }
        }
        return parts.distinct().joinToString(" · ").ifBlank { null }
    }
}
