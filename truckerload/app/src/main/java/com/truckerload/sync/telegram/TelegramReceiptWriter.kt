package com.truckerload.sync.telegram

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.truckerload.R
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.domain.ingest.ReceiptKind
import com.truckerload.domain.ingest.ReceiptPreview
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
    ): String {
        val ingest = TelegramJournalIngest(paycheckRepository, dieselRepository)
        return when (kind) {
            ReceiptKind.PAYCHECK -> savePaycheck(preview, ingest)
            ReceiptKind.DIESEL, ReceiptKind.DEF -> saveFuel(kind, preview, ingest, prefs)
            ReceiptKind.LOAD, ReceiptKind.UNKNOWN ->
                context.getString(R.string.sync_receipt_need_choice)
        }
    }

    private suspend fun savePaycheck(
        preview: ReceiptPreview,
        ingest: TelegramJournalIngest,
    ): String {
        val amount = preview.amount ?: return context.getString(R.string.sync_paycheck_not_found)
        return when (
            val outcome = ingest.insertPaycheck(
                netAmount = amount,
                grossAmount = null,
                driverName = preview.driverName,
                weekStartDateHint = preview.date,
                messageDateSeconds = preview.messageDateSeconds,
                rawText = preview.extractedText,
                sourceFileName = preview.sourceFileName,
            )
        ) {
            TelegramJournalIngest.PaycheckOutcome.InvalidAmount ->
                context.getString(R.string.sync_paycheck_not_found)
            is TelegramJournalIngest.PaycheckOutcome.AlreadyExists ->
                context.getString(R.string.sync_paycheck_exists, outcome.weekNumber)
            is TelegramJournalIngest.PaycheckOutcome.Inserted ->
                context.getString(
                    R.string.sync_last_paycheck,
                    String.format(Locale.US, "%,.2f", outcome.netAmount),
                    outcome.weekNumber,
                )
        }
    }

    private suspend fun saveFuel(
        kind: ReceiptKind,
        preview: ReceiptPreview,
        ingest: TelegramJournalIngest,
        prefs: SharedPreferences,
    ): String {
        val amount = preview.amount ?: return context.getString(R.string.sync_diesel_not_found)
        val salt = if (kind == ReceiptKind.DEF) "DEF" else null
        return when (
            val outcome = ingest.insertDiesel(
                totalAmount = amount,
                gallons = preview.gallons,
                pricePerGallon = preview.pricePerGallon,
                location = fuelLocation(kind, preview),
                dateHint = preview.date,
                messageDateSeconds = preview.messageDateSeconds,
                rawText = preview.extractedText,
                sourceFileName = preview.sourceFileName,
                fingerprintSalt = salt,
            )
        ) {
            TelegramJournalIngest.DieselOutcome.InvalidAmount ->
                context.getString(R.string.sync_diesel_not_found)
            TelegramJournalIngest.DieselOutcome.Duplicate ->
                context.getString(R.string.sync_duplicate_diesel)
            is TelegramJournalIngest.DieselOutcome.Inserted -> {
                prefs.edit {
                    putString(
                        "last_diesel_text_sha",
                        TelegramTextFingerprint.dieselFingerprint(preview.extractedText, salt),
                    )
                }
                val template = if (kind == ReceiptKind.DEF) {
                    R.string.sync_last_def
                } else {
                    R.string.sync_last_diesel
                }
                context.getString(
                    template,
                    String.format(Locale.US, "%,.2f", outcome.totalAmount),
                    outcome.weekNumber,
                )
            }
        }
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
