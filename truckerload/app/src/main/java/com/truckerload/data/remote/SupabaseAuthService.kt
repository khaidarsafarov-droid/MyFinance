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
 * Supabase Auth: обмен Google ID token на сессию.
 * При первом входе Supabase создаёт запись в auth.users.
 */
class SupabaseAuthService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    fun isConfigured(): Boolean =
        !BuildConfig.LOCAL_ONLY_MODE &&
            BuildConfig.SUPABASE_URL.isNotBlank() &&
            BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    private fun mapSignUpError(raw: String): String {
        val lower = raw.lowercase()
        return when {
            lower.contains("already registered") || lower.contains("duplicate") || lower.contains("already exists") ->
                "Пользователь с такими данными уже существует"
            lower.contains("password") && (lower.contains("6") || lower.contains("least")) ->
                "Пароль должен быть не менее 6 символов"
            lower.contains("connection") || lower.contains("network") || lower.contains("timeout") || lower.contains("unable to resolve") ->
                "Нет подключения к интернету. Проверьте сеть."
            lower.contains("email") && lower.contains("invalid") ->
                "Некорректный адрес email"
            else -> raw
        }
    }

    data class SupabaseUser(
        val id: String,
        val email: String?,
        val fullName: String?,
        val avatarUrl: String?
    )

    data class SignInResult(
        val user: SupabaseUser,
        val accessToken: String,
        val refreshToken: String,
        val expiresIn: Int
    )

    sealed class SignInError {
        data class Network(val message: String) : SignInError()
        data class Auth(val message: String, val code: Int? = null) : SignInError()
    }

    suspend fun signInWithIdToken(idToken: String): Result<SignInResult> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(Exception("Supabase не настроен. Добавьте SUPABASE_URL и SUPABASE_ANON_KEY в local.properties"))
        }
        val url = "${BuildConfig.SUPABASE_URL.trimEnd('/')}/auth/v1/token?grant_type=id_token"
        val body = JSONObject().apply {
            put("provider", "google")
            put("id_token", idToken)
        }.toString()
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val err = try {
                    JSONObject(responseBody).optString("error_description", responseBody)
                } catch (_: Exception) {
                    responseBody
                }
                return@withContext Result.failure(Exception("Ошибка Supabase: $err"))
            }
            val json = JSONObject(responseBody)
            val userJson = json.getJSONObject("user")
            val userMeta = userJson.optJSONObject("user_metadata") ?: JSONObject()
            val user = SupabaseUser(
                id = userJson.getString("id"),
                email = userJson.optString("email").takeIf { it.isNotBlank() },
                fullName = userMeta.optString("full_name").takeIf { it.isNotBlank() }
                    ?: userMeta.optString("name").takeIf { it.isNotBlank() }
                    ?: buildString {
                        val given = userMeta.optString("given_name")
                        val family = userMeta.optString("family_name")
                        if (given.isNotBlank()) append(given)
                        if (family.isNotBlank()) {
                            if (isNotEmpty()) append(" ")
                            append(family)
                        }
                    }.takeIf { it.isNotBlank() },
                avatarUrl = userMeta.optString("avatar_url").takeIf { it.isNotBlank() }
            )
            val result = SignInResult(
                user = user,
                accessToken = json.getString("access_token"),
                refreshToken = json.getString("refresh_token"),
                expiresIn = json.optInt("expires_in", 3600)
            )
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Проверка: занят ли email или телефон. */
    suspend fun checkRegistration(email: String, phoneNumber: String): Result<Pair<Boolean, Boolean>> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.failure(Exception("Supabase не настроен"))
        val url = "${BuildConfig.SUPABASE_URL.trimEnd('/')}/rest/v1/rpc/check_registration"
        val body = JSONObject().apply {
            put("p_email", email.trim())
            put("p_phone", phoneNumber.trim())
        }.toString()
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) return@withContext Result.failure(Exception("Ошибка проверки: $responseBody"))
            val json = JSONObject(responseBody)
            Result.success(
                json.optBoolean("email_taken", false) to json.optBoolean("phone_taken", false)
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Upsert профиля в таблицу profiles (после signUp). */
    suspend fun upsertProfile(
        accessToken: String,
        userId: String,
        fullName: String,
        phoneNumber: String,
        email: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.failure(Exception("Supabase не настроен"))
        val url = "${BuildConfig.SUPABASE_URL.trimEnd('/')}/rest/v1/profiles"
        val body = JSONObject().apply {
            put("id", userId)
            put("full_name", fullName)
            put("phone_number", phoneNumber)
            put("email", email)
        }.toString()
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates,on_conflict=id")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val bodyStr = response.body?.string() ?: ""
                return@withContext Result.failure(Exception("Не удалось сохранить профиль: $bodyStr"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Регистрация по email/паролю. Данные full_name и phone_number сохраняются в user_metadata и profiles. */
    suspend fun signUp(email: String, password: String, fullName: String, phoneNumber: String): Result<SignInResult> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(Exception("Supabase не настроен"))
        }
        val url = "${BuildConfig.SUPABASE_URL.trimEnd('/')}/auth/v1/signup"
        val body = JSONObject().apply {
            put("email", email)
            put("password", password)
            put("data", JSONObject().apply {
                put("full_name", fullName)
                put("phone_number", phoneNumber)
            })
        }.toString()
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val err = try {
                    JSONObject(responseBody).optString("msg", JSONObject(responseBody).optString("error_description", JSONObject(responseBody).optString("message", responseBody)))
                } catch (_: Exception) { responseBody }
                val friendly = mapSignUpError(err)
                return@withContext Result.failure(Exception(friendly))
            }
            val json = JSONObject(responseBody)
            val userJson = json.optJSONObject("user")
                ?: json.optJSONObject("data")?.optJSONObject("user")
                ?: if (json.has("id") && json.has("email")) json else null
                ?: return@withContext Result.failure(Exception("Нет user в ответе. Supabase: Auth → Email → отключите Confirm email для теста."))
            val userMeta = userJson.optJSONObject("user_metadata") ?: JSONObject()
            val user = SupabaseUser(
                id = userJson.getString("id"),
                email = userJson.optString("email").takeIf { it.isNotBlank() },
                fullName = userMeta.optString("full_name").takeIf { it.isNotBlank() } ?: fullName,
                avatarUrl = null
            )
            val accessToken = json.optString("access_token")
            val refreshToken = json.optString("refresh_token")
            Result.success(SignInResult(user = user, accessToken = accessToken, refreshToken = refreshToken, expiresIn = 3600))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class ProfileData(val fullName: String?, val phoneNumber: String?, val email: String?)

    /** Получить профиль из таблицы profiles по user.id */
    suspend fun getProfile(accessToken: String, userId: String): Result<ProfileData> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.failure(Exception("Supabase не настроен"))
        val url = "${BuildConfig.SUPABASE_URL.trimEnd('/')}/rest/v1/profiles?id=eq.$userId&select=full_name,phone_number,email"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()
        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) return@withContext Result.failure(Exception("Ошибка загрузки профиля"))
            val arr = org.json.JSONArray(responseBody)
            if (arr.length() == 0) return@withContext Result.success(ProfileData(null, null, null))
            val row = arr.getJSONObject(0)
            Result.success(ProfileData(
                fullName = row.optString("full_name").takeIf { it.isNotBlank() },
                phoneNumber = row.optString("phone_number").takeIf { it.isNotBlank() },
                email = row.optString("email").takeIf { it.isNotBlank() }
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapSignInError(raw: String): String {
        val lower = raw.lowercase()
        return when {
            lower.contains("invalid") || lower.contains("credentials") || lower.contains("email") && lower.contains("password") ->
                "Ошибка входа. Проверьте почту или пароль"
            lower.contains("connection") || lower.contains("network") || lower.contains("timeout") || lower.contains("unable to resolve") ->
                "Нет соединения с сетью"
            else -> raw
        }
    }

    /** Вход по email и паролю. */
    suspend fun signInWithPassword(email: String, password: String): Result<SignInResult> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(Exception("Supabase не настроен"))
        }
        val url = "${BuildConfig.SUPABASE_URL.trimEnd('/')}/auth/v1/token?grant_type=password"
        val body = JSONObject().apply {
            put("email", email)
            put("password", password)
        }.toString()
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val err = try {
                    JSONObject(responseBody).optString("error_description", JSONObject(responseBody).optString("msg", responseBody))
                } catch (_: Exception) { responseBody }
                return@withContext Result.failure(Exception(mapSignInError(err)))
            }
            val json = JSONObject(responseBody)
            val userJson = json.optJSONObject("user") ?: return@withContext Result.failure(Exception("Ошибка входа. Проверьте почту или пароль"))
            val userMeta = userJson.optJSONObject("user_metadata") ?: JSONObject()
            val user = SupabaseUser(
                id = userJson.getString("id"),
                email = userJson.optString("email").takeIf { it.isNotBlank() },
                fullName = userMeta.optString("full_name").takeIf { it.isNotBlank() }
                    ?: buildString {
                        val first = userMeta.optString("first_name")
                        val last = userMeta.optString("last_name")
                        if (first.isNotBlank()) append(first)
                        if (last.isNotBlank()) { if (isNotEmpty()) append(" "); append(last) }
                    }.takeIf { it.isNotBlank() },
                avatarUrl = null
            )
            val accessToken = json.optString("access_token")
            val refreshToken = json.optString("refresh_token")
            if (accessToken.isBlank() || refreshToken.isBlank()) return@withContext Result.failure(Exception("Нет токенов в ответе"))
            Result.success(SignInResult(
                user = user,
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresIn = json.optInt("expires_in", 3600)
            ))
        } catch (e: Exception) {
            Result.failure(Exception(mapSignInError(e.message ?: e.toString())))
        }
    }
}
