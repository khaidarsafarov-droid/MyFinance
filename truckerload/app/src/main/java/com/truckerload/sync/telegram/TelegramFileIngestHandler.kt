package com.truckerload.sync.telegram

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.truckerload.R
import com.truckerload.data.remote.TelegramApi
import com.truckerload.data.remote.TelegramUpdate
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.domain.ingest.InboundDocumentResolver
import com.truckerload.domain.ingest.ReceiptKind
import com.truckerload.domain.ingest.ReceiptPreview
import com.truckerload.domain.ingest.ReceiptPreviewFormatter
import com.truckerload.domain.model.Load
import com.truckerload.domain.parser.LoadField
import com.truckerload.domain.parser.ManualLoadFactory
import com.truckerload.domain.parser.MessageParseService
import com.truckerload.utils.LogRedactor
import com.truckerload.utils.PaycheckSourceFiles
import java.util.Locale

class TelegramFileIngestHandler(
    private val context: Context,
    private val apiClient: TelegramApiClient,
    private val messageParser: TelegramMessageParser,
    private val extractor: TelegramFileTextExtractor = TelegramFileTextExtractor(context),
    private val writer: TelegramReceiptWriter = TelegramReceiptWriter(context),
    private val parseService: MessageParseService = MessageParseService(),
) {
    suspend fun handleMedia(
        update: TelegramUpdate,
        loadRepository: LoadRepository,
        prefs: SharedPreferences,
    ) {
        val fileId = update.documentFileId ?: update.photoFileId
        if (fileId == null) return
        val declaredSize = update.documentFileSize
        if (declaredSize != null && declaredSize > TelegramApi.MAX_DOWNLOAD_BYTES) {
            apiClient.sendWithMenu(
                update.chatId,
                context.getString(
                    R.string.sync_import_file_too_large,
                    (TelegramApi.MAX_DOWNLOAD_BYTES / (1024 * 1024)).toInt(),
                ),
            )
            return
        }
        apiClient.sendMessage(update.chatId, context.getString(R.string.sync_processing))
        val bytes = apiClient.downloadFile(fileId).getOrElse { e ->
            Log.e(TAG, "download failed: ${LogRedactor.redact(e.message)}", e)
            apiClient.sendWithMenu(
                update.chatId,
                context.getString(R.string.sync_receipt_extract_failed, e.message ?: "?"),
            )
            return
        }
        val fileName = update.documentFileName?.takeIf { it.isNotBlank() }
            ?: if (update.photoFileId != null) "telegram_photo.jpg" else "telegram_file"
        val storedPath = PaycheckSourceFiles.copyFromBytes(context, bytes, fileName)
        val fileText = extractor.extract(bytes, fileName, update.documentMimeType)
            .getOrElse { e ->
                Log.e(TAG, "extract failed: ${LogRedactor.redact(e.message)}", e)
                ""
            }
        val caption = update.text.trim()
        val combined = listOf(caption, fileText).filter { it.isNotBlank() }.joinToString("\n\n")
        if (combined.isBlank()) {
            PaycheckSourceFiles.delete(context, storedPath)
            apiClient.sendWithMenu(update.chatId, context.getString(R.string.sync_receipt_empty_ocr))
            return
        }
        applyText(
            chatId = update.chatId,
            text = combined,
            fileName = fileName,
            messageDateSeconds = update.messageDateSeconds,
            loadRepository = loadRepository,
            prefs = prefs,
            sourceFilePath = storedPath,
        )
    }

    suspend fun applyText(
        chatId: String,
        text: String,
        fileName: String?,
        messageDateSeconds: Long?,
        loadRepository: LoadRepository,
        prefs: SharedPreferences,
        sourceFilePath: String? = null,
    ) {
        val referenceMillis = messageDateSeconds?.times(1000) ?: System.currentTimeMillis()
        val decision = InboundDocumentResolver.resolve(
            text = text,
            fileName = fileName,
            messageDateSeconds = messageDateSeconds,
            referenceMillis = referenceMillis,
            parseService = parseService,
        )
        if (decision.autoSaveLoads) {
            PaycheckSourceFiles.delete(context, sourceFilePath)
            saveLoads(chatId, decision.loads, text, messageDateSeconds, loadRepository)
            return
        }
        decision.incompleteLoad?.let { gaps ->
            PaycheckSourceFiles.delete(context, sourceFilePath)
            apiClient.sendWithMenu(
                chatId,
                context.getString(
                    R.string.sync_load_missing_fields,
                    gaps.allMissing.joinToString(", ") { context.getString(loadFieldLabel(it)) },
                ),
            )
            return
        }
        askConfirmation(
            chatId,
            decision.preview.copy(sourceFilePath = sourceFilePath),
            prefs,
        )
    }

    private fun loadFieldLabel(field: LoadField): Int = when (field) {
        LoadField.RATE -> R.string.add_load_field_rate
        LoadField.PICKUP -> R.string.add_load_field_pickup
        LoadField.DELIVERY -> R.string.add_load_field_delivery
        LoadField.MILES -> R.string.add_load_field_miles
        LoadField.DATE -> R.string.add_load_field_date
        LoadField.TRIP_ID -> R.string.add_load_field_trip_id
    }

    suspend fun handleCallback(
        data: String,
        chatId: String,
        loadRepository: LoadRepository,
        paycheckRepository: PaycheckRepository,
        dieselRepository: DieselRepository,
        prefs: SharedPreferences,
        callbackQueryId: String?,
    ) {
        val store = TelegramReceiptConfirmStore(prefs, context)
        if (data == TelegramReceiptKeyboard.CANCEL) {
            store.clear(chatId, discardFile = true)
            callbackQueryId?.let { apiClient.answerCallbackQuery(it, "OK") }
            apiClient.sendWithMenu(chatId, context.getString(R.string.sync_receipt_cancelled))
            return
        }
        val preview = store.load(chatId)
        if (preview == null) {
            callbackQueryId?.let { apiClient.answerCallbackQuery(it, "OK") }
            apiClient.sendWithMenu(chatId, context.getString(R.string.sync_receipt_expired))
            return
        }
        val kind = when (data) {
            TelegramReceiptKeyboard.LOAD -> ReceiptKind.LOAD
            TelegramReceiptKeyboard.DIESEL -> ReceiptKind.DIESEL
            TelegramReceiptKeyboard.DEF -> ReceiptKind.DEF
            TelegramReceiptKeyboard.PAYCHECK,
            TelegramReceiptKeyboard.CONFIRM,
            -> ReceiptKind.PAYCHECK
            else -> preview.kind
        }
        callbackQueryId?.let { apiClient.answerCallbackQuery(it, "OK") }
        if (kind == ReceiptKind.PAYCHECK && data != TelegramReceiptKeyboard.CONFIRM) {
            val asPay = preview.copy(kind = ReceiptKind.PAYCHECK)
            store.save(chatId, asPay)
            askPaycheckConfirm(chatId, asPay, prefs)
            return
        }
        if (kind == ReceiptKind.LOAD) {
            val referenceMillis = preview.messageDateSeconds?.times(1000) ?: System.currentTimeMillis()
            val loads = parseService.parseLoadsFromInboundText(
                preview.extractedText,
                referenceMillis,
                preview.sourceFileName,
            ).getOrNull().orEmpty().ifEmpty {
                listOfNotNull(loadFromPreview(preview, referenceMillis))
            }
            store.clear(chatId)
            if (loads.isEmpty()) {
                PaycheckSourceFiles.delete(context, preview.sourceFilePath)
                apiClient.sendWithMenu(chatId, context.getString(R.string.sync_receipt_load_parse_failed))
                return
            }
            saveLoads(
                chatId = chatId,
                loads = loads,
                rawMessage = preview.extractedText,
                messageDateSeconds = preview.messageDateSeconds,
                loadRepository = loadRepository,
            )
            PaycheckSourceFiles.delete(context, preview.sourceFilePath)
            return
        }
        val reply = writer.save(kind, preview, paycheckRepository, dieselRepository, prefs)
        store.clear(chatId)
        apiClient.sendWithMenu(chatId, reply)
    }

    private suspend fun saveLoads(
        chatId: String,
        loads: List<Load>,
        rawMessage: String,
        messageDateSeconds: Long?,
        loadRepository: LoadRepository,
    ) {
        val reply = messageParser.telegramLoadHandler(loadRepository).handleLoads(
            loads = loads,
            rawMessage = rawMessage,
            messageDateSeconds = messageDateSeconds,
        )
        apiClient.sendWithMenu(chatId, reply)
    }

    private fun loadFromPreview(
        preview: ReceiptPreview,
        referenceMillis: Long,
    ): Load? {
        val rate = preview.amount?.takeIf { it > 0 } ?: return null
        return ManualLoadFactory.build(
            tripId = preview.tripId.orEmpty(),
            date = preview.date.orEmpty(),
            rate = rate,
            miles = preview.miles ?: 0.0,
            pointA = preview.pointA.orEmpty(),
            pointB = preview.pointB.orEmpty(),
            rawMessage = preview.extractedText,
            nowMillis = referenceMillis,
        )
    }

    private suspend fun askConfirmation(
        chatId: String,
        preview: ReceiptPreview,
        prefs: SharedPreferences,
    ) {
        TelegramReceiptConfirmStore(prefs, context).save(chatId, preview)
        if (preview.kind == ReceiptKind.PAYCHECK) {
            askPaycheckConfirm(chatId, preview, prefs)
            return
        }
        val guessed = when (preview.kind) {
            ReceiptKind.PAYCHECK -> context.getString(R.string.sync_receipt_guess_paycheck)
            ReceiptKind.DIESEL -> context.getString(R.string.sync_receipt_guess_diesel)
            ReceiptKind.DEF -> context.getString(R.string.sync_receipt_guess_def)
            ReceiptKind.LOAD -> context.getString(R.string.sync_receipt_guess_load)
            ReceiptKind.UNKNOWN -> context.getString(R.string.sync_receipt_guess_unknown)
        }
        val html = ReceiptPreviewFormatter.toHtml(
            preview = preview,
            guessedLabel = guessed,
            askLabel = context.getString(R.string.sync_receipt_ask_type),
        )
        apiClient.sendHtml(
            chatId = chatId,
            html = html,
            replyMarkup = TelegramReceiptKeyboard.inline(
                load = context.getString(R.string.sync_receipt_btn_load),
                diesel = context.getString(R.string.sync_receipt_btn_diesel),
                def = context.getString(R.string.sync_receipt_btn_def),
                paycheck = context.getString(R.string.sync_receipt_btn_paycheck),
                cancel = context.getString(R.string.common_cancel),
            ),
        )
    }

    private suspend fun askPaycheckConfirm(
        chatId: String,
        preview: ReceiptPreview,
        prefs: SharedPreferences,
    ) {
        val amount = preview.amount
        if (amount == null || amount <= 0.0) {
            TelegramReceiptConfirmStore(prefs, context).clear(chatId, discardFile = true)
            apiClient.sendWithMenu(chatId, context.getString(R.string.sync_paycheck_amount_missing))
            return
        }
        val formatted = String.format(Locale.US, "%,.2f", amount)
        val html = ReceiptPreviewFormatter.toHtml(
            preview = preview,
            guessedLabel = context.getString(R.string.sync_receipt_guess_paycheck),
            askLabel = context.getString(R.string.sync_paycheck_confirm_ask, formatted),
        )
        apiClient.sendHtml(
            chatId = chatId,
            html = html,
            replyMarkup = TelegramReceiptKeyboard.confirm(
                yes = context.getString(R.string.sync_paycheck_confirm_yes),
                no = context.getString(R.string.sync_paycheck_confirm_no),
            ),
        )
    }

    private companion object {
        const val TAG = "TgFileIngest"
    }
}
