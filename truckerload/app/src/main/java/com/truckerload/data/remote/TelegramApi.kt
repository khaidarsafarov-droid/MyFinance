package com.truckerload.data.remote

import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class TelegramApi(private val token: String) {

    private val baseUrl get() = "https://api.telegram.org/bot$token"

    // FIX: share one OkHttpClient — constructing a new pool every poll leaked threads/sockets
    private val client get() = sharedClient

    fun isConfigured(): Boolean = token.isNotBlank()

    suspend fun deleteWebhook(): Result<Unit> = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext Result.failure(IllegalStateException("Bot token not set"))
        val url = "$baseUrl/deleteWebhook?drop_pending_updates=false"
        val request = Request.Builder().url(url).post(FormBody.Builder().build()).build()
        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("deleteWebhook failed: ${response.code}")
            }
        }
    }

    suspend fun setMyCommands(): Result<Unit> = postJson(
        "setMyCommands",
        JSONObject().put("commands", TelegramBotFeatures.commandsArray())
    )

    suspend fun setChatMenuButton(): Result<Unit> = postJson(
        "setChatMenuButton",
        JSONObject().put("menu_button", JSONObject().put("type", "commands"))
    )

    /** Display name shown in Telegram chats (not the @username). */
    suspend fun setMyName(name: String): Result<Unit> = postJson(
        "setMyName",
        JSONObject(setMyNamePayload(name)),
    )

    /**
     * Uploads a new static JPG as the bot profile photo ([setMyProfilePhoto]).
     * Telegram rejects reused file_ids; the bytes must be sent as multipart.
     */
    suspend fun setMyProfilePhoto(jpegBytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext Result.failure(IllegalStateException("Bot token not set"))
        if (jpegBytes.isEmpty()) return@withContext Result.failure(IllegalStateException("empty_photo"))
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("photo", profilePhotoStaticAttachJson())
            .addFormDataPart(
                "logo",
                "logo.jpg",
                jpegBytes.toRequestBody("image/jpeg".toMediaType()),
            )
            .build()
        val request = Request.Builder()
            .url("$baseUrl/setMyProfilePhoto")
            .post(body)
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                requireTelegramOk(
                    "setMyProfilePhoto",
                    response.code,
                    response.body?.string().orEmpty(),
                )
            }
        }
    }

    suspend fun sendMessage(
        chatId: String,
        text: String,
        replyMarkup: JSONObject? = null,
        parseMode: String? = null,
    ): Result<Unit> = sendMessageReturningId(chatId, text, replyMarkup, parseMode).map { }

    suspend fun sendMessageReturningId(
        chatId: String,
        text: String,
        replyMarkup: JSONObject? = null,
        parseMode: String? = null,
    ): Result<Long> = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext Result.failure(IllegalStateException("Bot token not set"))
        val body = FormBody.Builder()
            .add("chat_id", chatId)
            .add("text", text)
        parseMode?.let { body.add("parse_mode", it) }
        replyMarkup?.let { body.add("reply_markup", it.toString()) }
        val request = Request.Builder()
            .url("$baseUrl/sendMessage")
            .post(body.build())
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    throw Exception("Telegram API error: ${response.code} $errBody")
                }
                val json = JSONObject(response.body?.string() ?: "{}")
                json.optJSONObject("result")?.optLong("message_id")
                    ?: throw Exception("No message_id in sendMessage response")
            }
        }
    }

    suspend fun editMessageText(
        chatId: String,
        messageId: Long,
        text: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext Result.failure(IllegalStateException("Bot token not set"))
        val body = FormBody.Builder()
            .add("chat_id", chatId)
            .add("message_id", messageId.toString())
            .add("text", text)
            .build()
        val request = Request.Builder()
            .url("$baseUrl/editMessageText")
            .post(body)
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    throw Exception("editMessageText failed: ${response.code} $errBody")
                }
            }
        }
    }

    suspend fun sendDocument(
        chatId: String,
        file: File,
        caption: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext Result.failure(IllegalStateException("Bot token not set"))
        if (!file.exists()) return@withContext Result.failure(IllegalStateException("File not found"))
        val bodyBuilder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId)
            .addFormDataPart(
                "document",
                file.name,
                file.asRequestBody("text/plain".toMediaType())
            )
        caption?.let { bodyBuilder.addFormDataPart("caption", it) }
        val request = Request.Builder()
            .url("$baseUrl/sendDocument")
            .post(bodyBuilder.build())
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    throw Exception("sendDocument failed: ${response.code} $errBody")
                }
            }
        }
    }

    suspend fun answerCallbackQuery(callbackQueryId: String, text: String): Result<Unit> = postJson(
        "answerCallbackQuery",
        JSONObject()
            .put("callback_query_id", callbackQueryId)
            .put("text", text)
            .put("show_alert", false)
    )

    suspend fun getUpdates(
        offset: Long? = null,
        timeoutSeconds: Int = 50,
        limit: Int = 100
    ): Result<TelegramGetUpdatesResult> =
        withContext(Dispatchers.IO) {
            if (token.isBlank()) return@withContext Result.failure(IllegalStateException("Bot token not set"))
            val allowed = JSONArray().apply {
                put("message")
                put("callback_query")
            }
            val url = buildString {
                append("$baseUrl/getUpdates?timeout=$timeoutSeconds")
                append("&limit=").append(limit.coerceIn(1, 100))
                append("&allowed_updates=").append(java.net.URLEncoder.encode(allowed.toString(), "UTF-8"))
                offset?.let { append("&offset=$it") }
            }
            val request = Request.Builder().url(url).get().build()
            // FIX: cancel the HTTP call when the coroutine is cancelled (FGS restart / account switch)
            val call = client.newCall(request)
            val cancelHandle = coroutineContext[Job]?.invokeOnCompletion { cause ->
                if (cause != null) call.cancel()
            }
            try {
                runCatching {
                    call.execute().use { response ->
                        if (!response.isSuccessful) {
                            val errBody = response.body?.string() ?: ""
                            val hint = if (response.code == 401) {
                                " Invalid token - get a new one from @BotFather."
                            } else ""
                            throw Exception("getUpdates failed: ${response.code}.$hint $errBody")
                        }
                        val json = JSONObject(response.body?.string() ?: "{}")
                        val arr = json.optJSONArray("result")
                            ?: return@runCatching TelegramGetUpdatesResult(emptyList(), offset ?: 0L)
                        var rawMaxUpdateId = 0L
                        var parsedMaxUpdateId = 0L
                        val unparsedUpdateIds = mutableListOf<Long>()
                        val updates = buildList {
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                val updateId = obj.optLong("update_id")
                                if (updateId > rawMaxUpdateId) rawMaxUpdateId = updateId
                                val parsed = parseUpdate(obj)
                                if (parsed != null) {
                                    add(parsed)
                                    if (updateId > parsedMaxUpdateId) parsedMaxUpdateId = updateId
                                } else if (updateId > 0L) {
                                    unparsedUpdateIds.add(updateId)
                                }
                            }
                        }
                        val nextOffset = when {
                            parsedMaxUpdateId > 0L -> parsedMaxUpdateId + 1L
                            unparsedUpdateIds.isNotEmpty() -> unparsedUpdateIds.max() + 1L
                            offset != null && offset > 0L -> offset
                            else -> 0L
                        }
                        TelegramGetUpdatesResult(
                            updates = updates,
                            nextOffset = nextOffset,
                            rawMaxUpdateId = rawMaxUpdateId,
                            unparsedUpdateIds = unparsedUpdateIds,
                        )
                    }
                }
            } finally {
                cancelHandle?.dispose()
            }
        }

    private fun parseUpdate(obj: JSONObject): TelegramUpdate? {
        val callback = obj.optJSONObject("callback_query")
        if (callback != null) {
            val chatId = callback.optJSONObject("message")?.optJSONObject("chat")?.optString("id").orEmpty()
            val data = callback.optString("data", "")
            return TelegramUpdate(
                updateId = obj.optLong("update_id"),
                chatId = chatId,
                chatType = "private",
                text = data,
                callbackQueryId = callback.optString("id").takeIf { it.isNotBlank() },
                isCallbackQuery = true
            )
        }

        // FIX: ignore edited_message — re-ingest was silently updating/replacing loads
        val msg = obj.optJSONObject("message")
            ?: obj.optJSONObject("channel_post")
            ?: return null

        val chat = msg.optJSONObject("chat")
        val chatId = chat?.optString("id").orEmpty()
        val chatType = chat?.optString("type") ?: "private"
        var text = msg.optString("text").orEmpty()
        if (text.isBlank()) {
            text = msg.optString("caption").orEmpty()
        }

        var documentFileId: String? = null
        var documentFileName: String? = null
        var documentMimeType: String? = null
        var documentFileSize: Long? = null
        var photoFileId: String? = null
        val doc = msg.optJSONObject("document")
        val photoArr = msg.optJSONArray("photo")
        if (doc != null) {
            documentFileId = doc.optString("file_id").takeIf { it.isNotEmpty() }
            documentFileName = doc.optString("file_name").takeIf { it.isNotEmpty() }
            documentMimeType = doc.optString("mime_type").takeIf { it.isNotEmpty() }
            val rawSize = doc.optLong("file_size")
            documentFileSize = rawSize.takeIf { it > 0L }
        }
        if (photoArr != null && photoArr.length() > 0) {
            val largest = photoArr.getJSONObject(photoArr.length() - 1)
            photoFileId = largest.optString("file_id").takeIf { it.isNotEmpty() }
        }
        val rawDate = msg.optLong("date")
        // Prefer original send time for forwards so Relay MM/DD years match history.
        val forwardDate = msg.optLong("forward_date").takeIf { it > 0L }
            ?: msg.optJSONObject("forward_origin")?.optLong("date")?.takeIf { it > 0L }
        val messageDateSeconds = when {
            forwardDate != null -> forwardDate
            rawDate > 0L -> rawDate
            else -> null
        }
        return TelegramUpdate(
            updateId = obj.optLong("update_id"),
            chatId = chatId,
            chatType = chatType,
            text = text,
            documentFileId = documentFileId,
            documentFileName = documentFileName,
            documentMimeType = documentMimeType,
            documentFileSize = documentFileSize,
            photoFileId = photoFileId,
            messageDateSeconds = messageDateSeconds,
        )
    }

    private suspend fun postJson(method: String, payload: JSONObject): Result<Unit> = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext Result.failure(IllegalStateException("Bot token not set"))
        val body = payload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$baseUrl/$method")
            .post(body)
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                requireTelegramOk(method, response.code, response.body?.string().orEmpty())
            }
        }
    }

    suspend fun downloadFile(
        fileId: String,
        maxBytes: Long = MAX_DOWNLOAD_BYTES,
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext Result.failure(IllegalStateException("Bot token not set"))
        runCatching {
            val getFileUrl = "$baseUrl/getFile?file_id=$fileId"
            val getReq = Request.Builder().url(getFileUrl).get().build()
            client.newCall(getReq).execute().use { resp ->
                if (!resp.isSuccessful) throw Exception("getFile failed: ${resp.code}")
                val body = resp.body?.string() ?: throw Exception("Empty getFile response")
                val result = JSONObject(body).optJSONObject("result")
                    ?: throw Exception("No file_path in response")
                val path = result.optString("file_path")
                    .takeIf { it.isNotBlank() }
                    ?: throw Exception("No file_path in response")
                val declaredSize = result.optLong("file_size", -1L)
                if (declaredSize > 0 && declaredSize > maxBytes) {
                    throw Exception("File too large: $declaredSize bytes (max $maxBytes)")
                }
                val fileUrl = "https://api.telegram.org/file/bot$token/$path"
                val fileReq = Request.Builder().url(fileUrl).get().build()
                client.newCall(fileReq).execute().use { fileResp ->
                    if (!fileResp.isSuccessful) throw Exception("download failed: ${fileResp.code}")
                    val responseBody = fileResp.body ?: throw Exception("Empty file body")
                    val contentLength = responseBody.contentLength()
                    if (contentLength > 0 && contentLength > maxBytes) {
                        throw Exception("File too large: $contentLength bytes (max $maxBytes)")
                    }
                    val source = responseBody.source()
                    val buffer = okio.Buffer()
                    var totalRead = 0L
                    while (true) {
                        val read = source.read(buffer, 8_192)
                        if (read == -1L) break
                        totalRead += read
                        if (totalRead > maxBytes) {
                            throw Exception("File too large while downloading (max $maxBytes)")
                        }
                    }
                    buffer.readByteArray()
                }
            }
        }
    }

    companion object {
        /** Hard cap for Telegram file downloads to avoid OOM kills in the background bot service. */
        const val MAX_DOWNLOAD_BYTES = 20L * 1024 * 1024

        internal fun profilePhotoStaticAttachJson(): String =
            JSONObject()
                .put("type", "static")
                .put("photo", "attach://logo")
                .toString()

        internal fun setMyNamePayload(name: String): String =
            JSONObject().put("name", name).toString()

        internal fun requireTelegramOk(method: String, httpCode: Int, body: String) {
            if (httpCode !in 200..299) {
                throw Exception("$method failed: $httpCode $body")
            }
            val json = JSONObject(body.ifBlank { "{}" })
            if (json.has("ok") && !json.optBoolean("ok")) {
                throw Exception("$method failed: ${json.optString("description", body)}")
            }
        }

        private val sharedClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(70, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
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
    val documentFileSize: Long? = null,
    val photoFileId: String? = null,
    val messageDateSeconds: Long? = null,
    val callbackQueryId: String? = null,
    val isCallbackQuery: Boolean = false
)

data class TelegramGetUpdatesResult(
    val updates: List<TelegramUpdate>,
    /** Pass this value as offset on the next getUpdates call. */
    val nextOffset: Long,
    val rawMaxUpdateId: Long = 0L,
    /** Update ids in the batch that could not be parsed — advance past individually. */
    val unparsedUpdateIds: List<Long> = emptyList(),
)
