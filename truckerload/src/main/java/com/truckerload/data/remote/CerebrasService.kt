package com.truckerload.data.remote

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * Cerebras Cloud Inference client (API v1), OpenAI-compatible Chat Completions.
 */
class CerebrasService(
    private val apiKey: String,
    private val model: String = DEFAULT_MODEL
) {

    companion object {
        private const val BASE_URL = "https://api.cerebras.ai/v1"
        private const val CHAT_COMPLETIONS_PATH = "/chat/completions"
        private const val MAX_RETRIES = 3
        const val DEFAULT_MODEL = "llama3.1-8b"
        const val TEST_MODEL = "llama3.1-8b"
    }

    private fun isRetryableStatus(code: Int): Boolean = code == 429 || code in 500..599

    private fun backoffMs(attempt: Int): Long = when (attempt) {
        1 -> 600L
        2 -> 1200L
        else -> 2400L
    }

    class AiProviderException(
        val code: Int? = null,
        message: String
    ) : Exception(message)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun isConfigured(): Boolean = apiKey.isNotBlank()

    /**
     * Streaming chat — emits each token as it arrives.
     */
    fun chatStream(
        messages: List<Pair<String, String>>,
        systemInstruction: String,
        userMessage: String,
        temperature: Double = 0.2,
        topP: Double = 0.9
    ): Flow<String> = flow {
        if (apiKey.isBlank()) return@flow
        val cerebrasMessages = mutableListOf<JSONObject>()
        cerebrasMessages.add(JSONObject().apply {
            put("role", "system")
            put("content", systemInstruction)
        })
        messages.forEach { (role, text) ->
            cerebrasMessages.add(JSONObject().apply {
                put("role", if (role == "model") "assistant" else role)
                put("content", text)
            })
        }
        cerebrasMessages.add(JSONObject().apply {
            put("role", "user")
            put("content", userMessage)
        })
        val body = JSONObject().apply {
            put("model", model)
            put("stream", true)
            put("messages", JSONArray(cerebrasMessages))
            put("temperature", temperature)
            put("top_p", topP)
            put("max_completion_tokens", 1024)
        }.toString()
        val request = Request.Builder()
            .url("$BASE_URL$CHAT_COMPLETIONS_PATH")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        var emittedAny = false
        for (attempt in 1..MAX_RETRIES) {
            try {
                client.newCall(request).execute().use { response ->
                    if (response.code !in 200..299) {
                        val bodyStr = response.body?.string().orEmpty()
                        if (isRetryableStatus(response.code) && attempt < MAX_RETRIES && !emittedAny) {
                            Log.w("CerebrasService", "Stream retryable HTTP ${response.code}; attempt=$attempt")
                            delay(backoffMs(attempt))
                            return@use
                        }
                        throw AiProviderException(
                            code = response.code,
                            message = "Cerebras stream error ${response.code}: ${bodyStr.take(200)}"
                        )
                    }
                    val reader = BufferedReader(InputStreamReader(response.body?.byteStream() ?: return@flow))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val l = line!!
                        if (l.startsWith("data: ")) {
                            val jsonStr = l.removePrefix("data: ").trim()
                            if (jsonStr == "[DONE]") break
                            try {
                                val json = JSONObject(jsonStr)
                                val delta = json.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta")
                                val content = delta?.optString("content")
                                if (!content.isNullOrBlank()) {
                                    emittedAny = true
                                    emit(content)
                                }
                            } catch (_: Exception) { /* skip malformed */ }
                        }
                    }
                    return@flow
                }
            } catch (e: Exception) {
                val retryable = e is IOException || e is SocketTimeoutException
                if (retryable && attempt < MAX_RETRIES && !emittedAny) {
                    Log.w("CerebrasService", "Stream network retry; attempt=$attempt, err=${e.message}")
                    delay(backoffMs(attempt))
                    continue
                }
                throw e
            }
        }

        throw AiProviderException(message = "Cerebras stream failed after retries")
    }.flowOn(Dispatchers.IO)

    /**
     * Send chat completion request.
     */
    suspend fun chat(
        messages: List<Pair<String, String>>,
        systemInstruction: String,
        userMessage: String,
        temperature: Double = 0.2,
        topP: Double = 0.9
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.failure(Exception("Cerebras API key not set"))
        for (attempt in 1..MAX_RETRIES) {
            try {
            val cerebrasMessages = mutableListOf<JSONObject>()
            cerebrasMessages.add(JSONObject().apply {
                put("role", "system")
                put("content", systemInstruction)
            })
            messages.forEach { (role, text) ->
                cerebrasMessages.add(JSONObject().apply {
                    put("role", if (role == "model") "assistant" else role)
                    put("content", text)
                })
            }
            cerebrasMessages.add(JSONObject().apply {
                put("role", "user")
                put("content", userMessage)
            })

            val body = JSONObject().apply {
                put("model", model)
                put("stream", false)
                put("messages", JSONArray(cerebrasMessages))
                put("temperature", temperature)
                put("top_p", topP)
                put("max_completion_tokens", 1024)
            }.toString()

            val request = Request.Builder()
                .url("$BASE_URL$CHAT_COMPLETIONS_PATH")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            var responseCode = 0
            var bodyStr = ""
            client.newCall(request).execute().use { response ->
                responseCode = response.code
                bodyStr = response.body?.string() ?: ""
            }

            if (responseCode in 200..299) {
                val json = JSONObject(bodyStr)
                val choices = json.optJSONArray("choices")
                val content = choices?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
                if (!content.isNullOrBlank()) {
                    return@withContext Result.success(content)
                }
                return@withContext Result.failure(AiProviderException(code = 200, message = "Empty Cerebras response"))
            }

            if (isRetryableStatus(responseCode) && attempt < MAX_RETRIES) {
                Log.w("CerebrasService", "Retryable HTTP $responseCode; attempt=$attempt")
                delay(backoffMs(attempt))
                continue
            }

            Log.w("CerebrasService", "Cerebras error $responseCode: $bodyStr")
            return@withContext Result.failure(
                AiProviderException(
                    code = responseCode,
                    message = "Cerebras API error $responseCode: ${bodyStr.take(200)}"
                )
            )
            } catch (e: Exception) {
                val retryable = e is IOException || e is SocketTimeoutException
                if (retryable && attempt < MAX_RETRIES) {
                    Log.w("CerebrasService", "Network retry; attempt=$attempt, err=${e.message}")
                    delay(backoffMs(attempt))
                    continue
                }
                Log.w("CerebrasService", "Cerebras request failed: ${e.message}", e)
                return@withContext Result.failure(e)
            }
        }
        Result.failure(AiProviderException(message = "Cerebras request failed after retries"))
    }

    suspend fun healthCheck(): Result<String> =
        chat(
            messages = emptyList(),
            systemInstruction = "Reply with a short health status only.",
            userMessage = "Reply exactly with: CEREBRAS_OK"
        )
}
