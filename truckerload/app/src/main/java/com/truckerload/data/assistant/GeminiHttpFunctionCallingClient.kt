package com.truckerload.data.assistant

import com.truckerload.BuildConfig
import com.truckerload.domain.assistant.AssistantFunctionDeclarations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiHttpFunctionCallingClient(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY,
    private val model: String = BuildConfig.GEMINI_MODEL,
    private val client: OkHttpClient = defaultClient,
) : GeminiFunctionCallingClient {

    override fun isConfigured(): Boolean = apiKey.isNotBlank() && model.isNotBlank()

    override suspend fun generateFunctionCall(
        userText: String,
        localeTag: String,
        currentDateIso: String,
        currentWeekNumber: Int,
        currentWeekYear: Int,
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(IllegalStateException(ERROR_NO_KEY))
        }
        runCatching {
            val url = "$ENDPOINT/models/$model:generateContent"
            val req = Request.Builder()
                .url(url)
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", JSON_MEDIA)
                .post(requestBody(userText, localeTag, currentDateIso, currentWeekNumber, currentWeekYear)
                    .toRequestBody(JSON_MEDIA.toMediaType()))
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw IOException("gemini_http_${resp.code}")
                }
                if (body.isBlank()) throw IOException("gemini_empty")
                body
            }
        }
    }

    internal fun requestBody(
        userText: String,
        localeTag: String,
        currentDateIso: String,
        currentWeekNumber: Int,
        currentWeekYear: Int,
    ): String {
        val instruction = SYSTEM_PROMPT
            .replace("{date}", currentDateIso)
            .replace("{week}", currentWeekNumber.toString())
            .replace("{year}", currentWeekYear.toString())
            .replace("{locale}", localeTag)
        return JSONObject()
            .put(
                "systemInstruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", instruction))),
            )
            .put(
                "contents",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("parts", JSONArray().put(JSONObject().put("text", userText))),
                ),
            )
            .put("tools", AssistantFunctionDeclarations.toolsArray())
            .put(
                "toolConfig",
                JSONObject().put(
                    "functionCallingConfig",
                    JSONObject().put("mode", "AUTO"),
                ),
            )
            .toString()
    }

    companion object {
        const val ERROR_NO_KEY = "gemini_no_key"
        private const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta"
        private const val JSON_MEDIA = "application/json; charset=utf-8"
        private const val SYSTEM_PROMPT =
            "You are the Truck Log voice assistant. Today is {date}. " +
                "Current trucking week (Sunday–Saturday) is week {week} of {year}. " +
                "User locale is {locale}. Map the driver's spoken command to exactly one function. " +
                "If the intent is unclear or a required dollar amount is missing, call no function. " +
                "Never invent amounts. Dates must be YYYY-MM-DD. Amounts are US dollars."

        private val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
