package com.truckerload.data.community

import com.truckerload.BuildConfig
import com.truckerload.data.preferences.AuthStore
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.util.concurrent.TimeUnit

internal class CommunityRestClient(
    private val authStore: AuthStore,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun isReady(): Boolean {
        if (BuildConfig.LOCAL_ONLY_MODE) return false
        if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_ANON_KEY.isBlank()) return false
        val token = authStore.accessTokenOrNull().orEmpty()
        val userId = authStore.currentUserIdOrNull().orEmpty()
        return token.isNotBlank() && com.truckerload.domain.social.SocialIdentity.isUuid(userId)
    }

    fun userId(): String = authStore.currentUserIdOrNull().orEmpty()

    fun token(): String = authStore.accessTokenOrNull().orEmpty()

    fun baseUrl(): String = BuildConfig.SUPABASE_URL.trimEnd('/')

    fun get(pathAndQuery: String): Result<String> {
        val token = token()
        if (token.isBlank()) return Result.failure(IllegalStateException("no token"))
        val req = Request.Builder()
            .url("${baseUrl()}/rest/v1/$pathAndQuery")
            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        return execute(req)
    }

    fun post(path: String, body: JSONObject, prefer: String? = null): Result<String> {
        val token = token()
        if (token.isBlank()) return Result.failure(IllegalStateException("no token"))
        val builder = Request.Builder()
            .url("${baseUrl()}/rest/v1/$path")
            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .header("Prefer", prefer ?: "return=representation")
            .post(body.toString().toRequestBody(JSON))
        return execute(builder.build())
    }

    fun patch(pathAndQuery: String, body: JSONObject): Result<String> {
        val token = token()
        if (token.isBlank()) return Result.failure(IllegalStateException("no token"))
        val req = Request.Builder()
            .url("${baseUrl()}/rest/v1/$pathAndQuery")
            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .patch(body.toString().toRequestBody(JSON))
            .build()
        return execute(req)
    }

    fun delete(pathAndQuery: String): Result<String> {
        val token = token()
        if (token.isBlank()) return Result.failure(IllegalStateException("no token"))
        val req = Request.Builder()
            .url("${baseUrl()}/rest/v1/$pathAndQuery")
            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .header("Authorization", "Bearer $token")
            .delete()
            .build()
        return execute(req)
    }

    fun rpc(fn: String, body: JSONObject): Result<String> {
        val token = token()
        if (token.isBlank()) return Result.failure(IllegalStateException("no token"))
        val req = Request.Builder()
            .url("${baseUrl()}/rest/v1/rpc/$fn")
            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON))
            .build()
        return execute(req)
    }

    fun parseUuid(raw: String): String {
        val trimmed = raw.trim()
        val value = when {
            trimmed.startsWith("\"") -> JSONTokener(trimmed).nextValue() as? String
            trimmed.startsWith("[") -> JSONArray(trimmed).optString(0).takeIf { it.isNotBlank() }
                ?: JSONArray(trimmed).optJSONObject(0)?.let { firstNonBlank(it) }

            trimmed.startsWith("{") -> firstNonBlank(JSONObject(trimmed))
            else -> trimmed.trim('"')
        }
        return value?.trim().orEmpty()
    }

    private fun firstNonBlank(obj: JSONObject): String? {
        val keys = obj.keys()
        while (keys.hasNext()) {
            val v = obj.optString(keys.next())
            if (v.isNotBlank()) return v
        }
        return null
    }

    private fun execute(req: Request): Result<String> = runCatching {
        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("${req.method} ${req.url.encodedPath} HTTP ${resp.code}: $text")
            text
        }
    }

    companion object {
        private val JSON = "application/json".toMediaType()
    }
}
