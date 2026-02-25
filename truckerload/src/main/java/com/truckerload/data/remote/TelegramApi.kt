package com.truckerload.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Telegram Bot HTTP API: getUpdates (long-poll) and sendMessage.
 * Token from BuildConfig.TELEGRAM_BOT_TOKEN (local.properties).
 */
class TelegramApi(private val token: String) {

    private val baseUrl get() = "https://api.telegram.org/bot$token"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    fun isConfigured(): Boolean = token.isNotBlank()

    /**
     * Delete webhook so getUpdates (long polling) works.
     * If a webhook is set, Telegram sends updates only to the webhook URL and getUpdates returns empty.
     */
    suspend fun deleteWebhook(): Result<Unit> = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext Result.failure(IllegalStateException("Bot token not set"))
        val request = Request.Builder().url("$baseUrl/deleteWebhook").post(FormBody.Builder().build()).build()
        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("deleteWebhook failed: ${response.code}")
            }
        }
    }

    suspend fun sendMessage(chatId: String, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext Result.failure(IllegalStateException("Bot token not set"))
        val body = FormBody.Builder()
            .add("chat_id", chatId)
            .add("text", text)
            .add("parse_mode", "HTML")
            .build()
        val request = Request.Builder()
            .url("$baseUrl/sendMessage")
            .post(body)
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    throw Exception("Telegram API error: ${response.code} $errBody")
                }
            }
        }
    }

    /**
     * Long polling: waits up to [timeoutSeconds] for new updates.
     * Returns updates and the next offset to use (last update_id + 1).
     */
    suspend fun getUpdates(offset: Long? = null, timeoutSeconds: Int = 25): Result<TelegramGetUpdatesResult> = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext Result.failure(IllegalStateException("Bot token not set"))
        val url = buildString {
            append("$baseUrl/getUpdates?timeout=$timeoutSeconds")
            offset?.let { append("&offset=$it") }
        }
        val request = Request.Builder().url(url).get().build()
        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    val hint = if (response.code == 401) " Invalid token - get a new one from @BotFather and set TELEGRAM_BOT_TOKEN in local.properties." else ""
                    throw Exception("getUpdates failed: ${response.code}.$hint $errBody")
                }
                val json = JSONObject(response.body?.string() ?: "{}")
                val arr = json.optJSONArray("result") ?: return@runCatching TelegramGetUpdatesResult(emptyList(), offset ?: 0L)
                val updates = List(arr.length()) { i ->
                    val obj = arr.getJSONObject(i)
                    val msg = obj.optJSONObject("message") ?: obj.optJSONObject("channel_post")
                    val chat = msg?.optJSONObject("chat")
                    val chatId = chat?.optString("id") ?: ""
                    val chatType = chat?.optString("type") ?: "private"
                    var text = msg?.optString("text") ?: ""
                    var documentFileId: String? = null
                    var documentFileName: String? = null
                    var documentMimeType: String? = null
                    var photoFileId: String? = null
                    val doc = msg?.optJSONObject("document")
                    val photoArr = msg?.optJSONArray("photo")
                    if (doc != null) {
                        documentFileId = doc.optString("file_id").takeIf { it.isNotEmpty() }
                        documentFileName = doc.optString("file_name").takeIf { it.isNotEmpty() }
                        documentMimeType = doc.optString("mime_type").takeIf { it.isNotEmpty() }
                    }
                    if (photoArr != null && photoArr.length() > 0) {
                        val largest = photoArr.getJSONObject(photoArr.length() - 1)
                        photoFileId = largest.optString("file_id").takeIf { it.isNotEmpty() }
                    }
                    val updateId = obj.optLong("update_id")
                    val rawDate = msg?.optLong("date") ?: 0L
                    val messageDateSeconds = if (rawDate > 0) rawDate else null
                    TelegramUpdate(updateId, chatId, chatType, text, documentFileId, documentFileName, documentMimeType, photoFileId, messageDateSeconds)
                }
                val nextOffset = updates.maxOfOrNull { it.updateId }?.let { it + 1 } ?: (offset ?: 0L)
                TelegramGetUpdatesResult(updates, nextOffset)
            }
        }
    }

    /**
     * Download file by file_id. Returns byte array or null on failure.
     */
    suspend fun downloadFile(fileId: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext Result.failure(IllegalStateException("Bot token not set"))
        runCatching {
            val getFileUrl = "$baseUrl/getFile?file_id=$fileId"
            val getReq = Request.Builder().url(getFileUrl).get().build()
            client.newCall(getReq).execute().use { resp ->
                if (!resp.isSuccessful) throw Exception("getFile failed: ${resp.code}")
                val body = resp.body?.string() ?: throw Exception("Empty getFile response")
                val path = JSONObject(body).optJSONObject("result")?.optString("file_path")
                    ?: throw Exception("No file_path in response")
                val fileUrl = "https://api.telegram.org/file/bot$token/$path"
                val fileReq = Request.Builder().url(fileUrl).get().build()
                client.newCall(fileReq).execute().use { fileResp ->
                    if (!fileResp.isSuccessful) throw Exception("download failed: ${fileResp.code}")
                    fileResp.body?.bytes() ?: throw Exception("Empty file body")
                }
            }
        }
    }
}

data class TelegramUpdate(
    val updateId: Long,
    val chatId: String,
    val chatType: String,
    val text: String,
    val documentFileId: String? = null,
    val documentFileName: String? = null,
    val documentMimeType: String? = null,
    val photoFileId: String? = null,
    /** Telegram message date (Unix seconds). Used to save loads by message date in groups. */
    val messageDateSeconds: Long? = null
)
data class TelegramGetUpdatesResult(val updates: List<TelegramUpdate>, val nextOffset: Long)
