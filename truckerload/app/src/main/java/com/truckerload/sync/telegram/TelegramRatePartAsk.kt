package com.truckerload.sync.telegram

import android.content.Context
import android.content.SharedPreferences
import com.truckerload.R
import com.truckerload.domain.ingest.ReceiptKind
import com.truckerload.domain.ingest.ReceiptPreview
import com.truckerload.domain.model.Load
import com.truckerload.domain.parser.RatePartCheck
import java.util.Locale

/**
 * One question when linehaul + fuel + accessorials do not match the labeled total.
 * Yes saves the labeled total. A typed amount saves that amount instead.
 */
internal class TelegramRatePartAsk(
    private val context: Context,
    private val apiClient: TelegramApiClient,
) {
    suspend fun ask(
        chatId: String,
        text: String,
        fileName: String?,
        messageDateSeconds: Long?,
        sourceFilePath: String?,
        prefs: SharedPreferences,
        check: RatePartCheck,
        load: Load?,
    ) {
        val labeled = check.labeledCents / 100.0
        val preview = ReceiptPreview(
            kind = ReceiptKind.LOAD,
            amount = labeled,
            gallons = null,
            pricePerGallon = null,
            date = load?.date,
            location = null,
            vendor = null,
            driverName = null,
            tripId = load?.tripId,
            miles = load?.totalMiles,
            pointA = load?.pointA,
            pointB = load?.pointB,
            extractedText = text,
            highlightToken = String.format(Locale.US, "%.2f", labeled),
            sourceFileName = fileName,
            sourceFilePath = sourceFilePath,
            messageDateSeconds = messageDateSeconds,
        )
        TelegramReceiptConfirmStore(prefs, context).save(chatId, preview)
        apiClient.sendHtml(
            chatId = chatId,
            html = context.getString(
                R.string.sync_rate_parts_ask,
                String.format(Locale.US, "%,.2f", labeled),
                String.format(Locale.US, "%,.2f", check.partsCents / 100.0),
            ),
            replyMarkup = TelegramReceiptKeyboard.amountPicker(
                amounts = emptyList(),
                yes = context.getString(R.string.sync_paycheck_confirm_yes),
                typeOwn = context.getString(R.string.sync_amount_type_own),
                cancel = context.getString(R.string.common_cancel),
            ),
        )
    }
}
