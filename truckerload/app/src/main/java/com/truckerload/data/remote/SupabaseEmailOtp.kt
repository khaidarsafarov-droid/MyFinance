package com.truckerload.data.remote

import com.truckerload.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Supabase GoTrue email OTP / resend helpers for soft email verification.
 * Kept separate from [SupabaseAuthService] to stay under the Kotlin file-size gate.
 */
class SupabaseEmailOtp {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    fun isConfigured(): Boolean =
        !BuildConfig.LOCAL_ONLY_MODE &&
            BuildConfig.SUPABASE_URL.isNotBlank() &&
            BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    /**
     * Asks Supabase to (re)send a signup / email OTP to [email].
     * Tries `/resend` first (post-signup), then `/otp` as fallback.
     */
    suspend fun sendVerificationCode(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(IllegalStateException("Supabase not configured"))
        }
        val trimmed = email.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("email required"))
        }
        val resend = postJson(
            path = "/auth/v1/resend",
            body = JSONObject().apply {
                put("type", "signup")
                put("email", trimmed)
            },
        )
        if (resend.isSuccess) return@withContext Result.success(Unit)

        val otp = postJson(
            path = "/auth/v1/otp",
            body = JSONObject().apply {
                put("email", trimmed)
                put("create_user", false)
            },
        )
        if (otp.isSuccess) return@withContext Result.success(Unit)

        Result.failure(
            otp.exceptionOrNull()
                ?: resend.exceptionOrNull()
                ?: Exception("Failed to send verification email"),
        )
    }

    /**
     * Verifies a 6-digit (or longer) email OTP with Supabase.
     * Tries `signup` then `email` token types.
     */
    suspend fun verifyCode(email: String, token: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(IllegalStateException("Supabase not configured"))
        }
        val trimmed = email.trim()
        val code = token.trim()
        if (trimmed.isBlank() || code.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("email and token required"))
        }
        for (type in listOf("signup", "email")) {
            val result = postJson(
                path = "/auth/v1/verify",
                body = JSONObject().apply {
                    put("type", type)
                    put("email", trimmed)
                    put("token", code)
                },
            )
            if (result.isSuccess) return@withContext Result.success(Unit)
        }
        Result.failure(Exception("Invalid verification code"))
    }

    private fun postJson(path: String, body: JSONObject): Result<Unit> {
        val url = "${BuildConfig.SUPABASE_URL.trimEnd('/')}$path"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val raw = response.body?.string().orEmpty()
                    val msg = try {
                        JSONObject(raw).optString(
                            "msg",
                            JSONObject(raw).optString(
                                "error_description",
                                JSONObject(raw).optString("message", raw),
                            ),
                        )
                    } catch (_: Exception) {
                        raw
                    }
                    Result.failure(Exception(msg.ifBlank { "HTTP ${response.code}" }))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
