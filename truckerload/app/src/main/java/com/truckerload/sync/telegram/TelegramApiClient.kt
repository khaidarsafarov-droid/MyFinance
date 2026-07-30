package com.truckerload.sync.telegram

import android.util.Log
import com.truckerload.data.remote.TelegramApi
import com.truckerload.data.remote.TelegramBotFeatures
import com.truckerload.data.remote.TelegramGetUpdatesResult
import com.truckerload.sync.TelegramSyncPolicy
import com.truckerload.utils.LogRedactor
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.io.File

/**
 * Thin client over [TelegramApi] with retry / rate-limit awareness.
 * Raw HTTP stays in [TelegramApi]; this layer owns call policy for the sync engine.
 */
class TelegramApiClient(
    private val api: TelegramApi,
    private val scheduler: TelegramSyncScheduler = TelegramSyncScheduler(),
) {
    constructor(token: String) : this(TelegramApi(token))

    suspend fun getUpdates(
        offset: Long?,
        timeoutSeconds: Int = 25,
        maxAttempts: Int = 3,
    ): Result<TelegramGetUpdatesResult> {
        var lastError: Throwable? = null
        repeat(maxAttempts) { attempt ->
            val result = api.getUpdates(offset = offset, timeoutSeconds = timeoutSeconds)
            if (result.isSuccess) return result
            lastError = result.exceptionOrNull()
            val message = lastError?.message
            if (!TelegramSyncPolicy.isRetryable(message) || attempt == maxAttempts - 1) {
                return result
            }
            val backoff = scheduler.backoffMs(attempt)
            Log.w(TAG, "getUpdates retry attempt=${attempt + 1} in ${backoff}ms: ${LogRedactor.redact(message)}")
            delay(backoff)
        }
        return Result.failure(lastError ?: IllegalStateException("getUpdates failed"))
    }

    suspend fun sendMessage(
        chatId: String,
        text: String,
        replyMarkup: JSONObject? = null,
    ): Result<Unit> = api.sendMessage(chatId, text, replyMarkup)

    suspend fun sendWithMenu(chatId: String, text: String): Result<Unit> =
        api.sendMessage(chatId, text, TelegramBotFeatures.mainMenuKeyboard())
            .onFailure { e ->
                Log.e(TAG, "menu reply failed: ${LogRedactor.redact(e.message)}")
            }

    suspend fun answerCallbackQuery(callbackQueryId: String, text: String): Result<Unit> =
        api.answerCallbackQuery(callbackQueryId, text)

    suspend fun downloadFile(fileId: String): Result<ByteArray> = api.downloadFile(fileId)

    suspend fun sendDocument(chatId: String, file: File, caption: String): Result<Unit> =
        api.sendDocument(chatId, file, caption)

    /** Underlying API for handlers that still take [TelegramApi]. */
    fun asTelegramApi(): TelegramApi = api

    companion object {
        private const val TAG = "TelegramApiClient"
    }
}
