package com.truckerload.data.community

import com.truckerload.BuildConfig
import com.truckerload.data.preferences.AuthStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

class CommunityStorageClient(
    private val authStore: AuthStore,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun isReady(): Boolean {
        if (BuildConfig.LOCAL_ONLY_MODE) return false
        if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_ANON_KEY.isBlank()) return false
        return !authStore.accessTokenOrNull().isNullOrBlank() &&
                com.truckerload.domain.social.SocialIdentity.isUuid(
                    authStore.currentUserIdOrNull().orEmpty()
                )
    }

    suspend fun upload(localFile: File, objectPath: String, mime: String): Result<String> =
        withContext(Dispatchers.IO) {
            if (!isReady()) return@withContext Result.failure(IllegalStateException("storage offline"))
            if (!localFile.exists()) return@withContext Result.failure(IllegalStateException("missing file"))
            val token = authStore.accessTokenOrNull().orEmpty()
            val url =
                "${BuildConfig.SUPABASE_URL.trimEnd('/')}/storage/v1/object/community/$objectPath"
            val req = Request.Builder()
                .url(url)
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $token")
                .header("x-upsert", "true")
                .put(localFile.asRequestBody(mime.toMediaType()))
                .build()
            runCatching {
                client.newCall(req).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) error("upload HTTP ${resp.code}: $text")
                    objectPath
                }
            }
        }

    suspend fun download(objectPath: String, dest: File): Result<File> =
        withContext(Dispatchers.IO) {
            if (!isReady() || objectPath.isBlank()) {
                return@withContext Result.failure(IllegalStateException("storage offline"))
            }
            if (objectPath.startsWith("/") || objectPath.contains("..")) {
                return@withContext Result.failure(IllegalStateException("bad path"))
            }
            val token = authStore.accessTokenOrNull().orEmpty()
            val url =
                "${BuildConfig.SUPABASE_URL.trimEnd('/')}/storage/v1/object/community/$objectPath"
            val req = Request.Builder()
                .url(url)
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            runCatching {
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) error("download HTTP ${resp.code}")
                    dest.parentFile?.mkdirs()
                    dest.outputStream().use { out ->
                        resp.body?.byteStream()?.copyTo(out)
                    }
                    dest
                }
            }
        }

    suspend fun uploadBytes(bytes: ByteArray, objectPath: String, mime: String): Result<String> =
        withContext(Dispatchers.IO) {
            if (!isReady()) return@withContext Result.failure(IllegalStateException("storage offline"))
            val token = authStore.accessTokenOrNull().orEmpty()
            val url =
                "${BuildConfig.SUPABASE_URL.trimEnd('/')}/storage/v1/object/community/$objectPath"
            val req = Request.Builder()
                .url(url)
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $token")
                .header("x-upsert", "true")
                .put(bytes.toRequestBody(mime.toMediaType()))
                .build()
            runCatching {
                client.newCall(req).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) error("upload HTTP ${resp.code}: $text")
                    objectPath
                }
            }
        }
}
