package com.truckerload.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class TelegramBotHealthResult(
    val ok: Boolean,
    val username: String? = null,
    val error: String? = null,
    val isUnauthorized: Boolean = false
)

object TelegramBotHealth {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    suspend fun check(token: String): TelegramBotHealthResult = withContext(Dispatchers.IO) {
        if (token.isBlank()) {
            return@withContext TelegramBotHealthResult(
                ok = false,
                error = "Token not set",
            )
        }
        runCatching {
            val request = Request.Builder()
                .url("https://api.telegram.org/bot$token/getMe")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.code == 401) {
                    return@withContext TelegramBotHealthResult(
                        ok = false,
                        isUnauthorized = true,
                        error = "Invalid token (401). Get a new one from @BotFather → /token",
                    )
                }
                if (!response.isSuccessful) {
                    return@withContext TelegramBotHealthResult(
                        ok = false,
                        error = com.truckerload.utils.LogRedactor.redact("HTTP ${response.code}: $body")
                    )
                }
                val json = JSONObject(body)
                val username = json.optJSONObject("result")?.optString("username")
                TelegramBotHealthResult(ok = json.optBoolean("ok"), username = username)
            }
        }.getOrElse { e ->
            TelegramBotHealthResult(
                ok = false,
                error = com.truckerload.utils.LogRedactor.redact(e.message ?: "Network error"),
            )
        }
    }
}
