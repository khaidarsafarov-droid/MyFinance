package com.example.myfinance.telegram

import com.example.myfinance.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class TelegramApi {

    private val token: String = BuildConfig.TELEGRAM_BOT_TOKEN
    private val baseUrl = "https://api.telegram.org/bot$token"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)  // long polling getUpdates can wait up to 25s
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    fun isConfigured(): Boolean = token.isNotBlank()

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
     * Long polling: request waits up to [timeoutSeconds] for new updates (real-time).
     * Use timeout 25–30 so the server returns as soon as a message arrives.
     */
    suspend fun getUpdates(offset: Long? = null, timeoutSeconds: Int = 25): Result<List<TelegramUpdate>> = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext Result.failure(IllegalStateException("Bot token not set"))
        val url = buildString {
            append("$baseUrl/getUpdates?timeout=$timeoutSeconds")
            offset?.let { append("&offset=$it") }
        }
        val request = Request.Builder().url(url).get().build()
        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("getUpdates failed: ${response.code}")
                val json = JSONObject(response.body?.string() ?: "{}")
                val arr = json.optJSONArray("result") ?: return@runCatching emptyList()
                List(arr.length()) { i ->
                    val obj = arr.getJSONObject(i)
                    val msg = obj.optJSONObject("message")
                    val chat = msg?.optJSONObject("chat")
                    val fromId = chat?.optString("id") ?: ""
                    val chatType = chat?.optString("type") ?: "private" // "private", "group", "supergroup", "channel"
                    val text = msg?.optString("text") ?: ""
                    val updateId = obj.optLong("update_id")
                    TelegramUpdate(updateId, fromId, chatType, text)
                }
            }
        }
    }
}

data class TelegramUpdate(val updateId: Long, val chatId: String, val chatType: String, val text: String)
