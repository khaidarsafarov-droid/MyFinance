package com.truckerload.sync.telegram

import android.content.Context
import android.util.Log
import com.truckerload.data.remote.TelegramApi
import com.truckerload.data.remote.TelegramBotFeatures
import com.truckerload.data.remote.TelegramGetUpdatesResult
import com.truckerload.sync.TelegramAuthErrors
import com.truckerload.sync.TelegramBotForegroundService
import com.truckerload.utils.LogRedactor
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject

/**
 * Thin wrapper around [TelegramApi] for the sync engine: create client, poll, reply.
 * Commands live in Telegram's Menu button; replies hide the old reply keyboard.
 * Raw HTTP lives in [TelegramApi] (`data/remote`).
 */
class TelegramApiClient(
    private val context: Context,
    private val telegramApi: TelegramApi,
) {
    fun api(): TelegramApi = telegramApi

    suspend fun getUpdates(
        offset: Long?,
        timeoutSeconds: Int = 25,
    ): Result<TelegramGetUpdatesResult> = telegramApi.getUpdates(offset, timeoutSeconds)

    suspend fun sendWithMenu(chatId: String, text: String) {
        telegramApi.sendMessage(chatId, text, TelegramBotFeatures.removeReplyKeyboard())
            .onFailure { e -> Log.e(TAG, "menu reply failed: ${LogRedactor.redact(e.message)}") }
    }

    suspend fun sendMessage(chatId: String, text: String) =
        telegramApi.sendMessage(chatId, text)

    suspend fun sendHtml(
        chatId: String,
        html: String,
        replyMarkup: JSONObject? = null,
    ) {
        telegramApi.sendMessage(chatId, html, replyMarkup, parseMode = "HTML")
            .onFailure { e ->
                Log.e(TAG, "html reply failed: ${LogRedactor.redact(e.message)}")
                sendWithMenu(chatId, html.replace(Regex("<[^>]+>"), ""))
            }
    }

    suspend fun answerCallbackQuery(callbackQueryId: String, text: String = "OK") =
        telegramApi.answerCallbackQuery(callbackQueryId, text)

    suspend fun downloadFile(fileId: String) = telegramApi.downloadFile(fileId)

    /**
     * Maps getUpdates failure into a [TelegramSyncRunResult], optionally stopping
     * the foreground service on auth errors.
     */
    fun mapGetUpdatesFailure(e: Throwable): TelegramSyncRunResult {
        Log.e(TAG, "getUpdates failed: ${LogRedactor.redact(e.message)}", e)
        val delay = if (e.message?.contains("409") == true) 45L else 30L
        if (TelegramAuthErrors.shouldStopService(e.message)) {
            TelegramBotForegroundService.stop(context)
        }
        return TelegramSyncRunResult(
            skipped = false,
            processedUpdates = 0,
            nextDelaySeconds = delay,
            error = LogRedactor.redact(e.message),
        )
    }

    companion object {
        private const val TAG = "TelegramBotSync"
        private val exportCaptionDate = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US)

        fun create(context: Context, token: String): TelegramApiClient =
            TelegramApiClient(context, TelegramApi(token))

        suspend fun sendFileToTelegram(
            context: Context,
            token: String,
            chatId: Long,
            file: File,
        ): Result<Unit> {
            val caption = context.getString(
                com.truckerload.R.string.telegram_export_caption,
                exportCaptionDate.format(Date()),
            )
            return TelegramApi(token).sendDocument(chatId.toString(), file, caption)
        }
    }
}
