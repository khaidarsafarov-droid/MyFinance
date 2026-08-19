package com.truckerload.data.sync

import android.content.Context
import com.truckerload.BuildConfig
import com.truckerload.contract.DevicePushTokenRequest
import com.truckerload.contract.PushPlatforms
import com.truckerload.contract.TelegramInboxListResponse
import com.truckerload.contract.TelegramLinkTokenResponse
import com.truckerload.data.preferences.AuthStore
import com.google.gson.JsonObject
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class AccountCloudWriteResult(
    val localWritten: Boolean,
    val remoteConfigured: Boolean,
    val remoteAcknowledged: Boolean,
) {
    val successful: Boolean
        get() = localWritten && (!remoteConfigured || remoteAcknowledged)
}

interface AccountCloudBackend {
    val remoteConfigured: Boolean

    suspend fun read(accountId: String): AccountCloudSnapshot?
    suspend fun write(snapshot: AccountCloudSnapshot): AccountCloudWriteResult
}

class LocalAccountCloudBackend(
    private val mirror: AccountCloudMirror,
) : AccountCloudBackend {
    override val remoteConfigured: Boolean = false

    override suspend fun read(accountId: String): AccountCloudSnapshot? = mirror.read(accountId)

    override suspend fun write(snapshot: AccountCloudSnapshot): AccountCloudWriteResult {
        mirror.write(snapshot)
        return AccountCloudWriteResult(
            localWritten = true,
            remoteConfigured = false,
            remoteAcknowledged = false,
        )
    }
}

class HybridAccountCloudBackend(
    private val local: AccountCloudBackend,
    private val remote: AccountCloudBackend,
) : AccountCloudBackend {
    override val remoteConfigured: Boolean = true

    override suspend fun read(accountId: String): AccountCloudSnapshot? {
        val remoteSnapshot = runCatching { remote.read(accountId) }.getOrNull()
        if (remoteSnapshot != null) {
            local.write(remoteSnapshot)
            return remoteSnapshot
        }
        return local.read(accountId)
    }

    override suspend fun write(snapshot: AccountCloudSnapshot): AccountCloudWriteResult {
        local.write(snapshot)
        val acknowledged = runCatching { remote.write(snapshot).remoteAcknowledged }.getOrDefault(false)
        return AccountCloudWriteResult(
            localWritten = true,
            remoteConfigured = true,
            remoteAcknowledged = acknowledged,
        )
    }
}

class RemoteAccountCloudClient(
    backendUrl: String,
    private val accessToken: () -> String?,
    private val deviceId: String,
    private val client: OkHttpClient = OkHttpClient(),
    allowInsecureHttp: Boolean = BuildConfig.DEBUG,
) : AccountCloudBackend {
    private val baseUrl: HttpUrl = validatedBaseUrl(backendUrl, allowInsecureHttp)

    override val remoteConfigured: Boolean = true

    override suspend fun read(accountId: String): AccountCloudSnapshot? = withContext(Dispatchers.IO) {
        val response = client.newCall(
            authorizedRequest(endpoint("v1", "sync", "snapshot")).get().build(),
        ).execute()
        response.use {
            if (it.code == 204) return@withContext null
            if (!it.isSuccessful) throw IOException("Snapshot download failed with HTTP ${it.code}")
            val json = it.body?.string()?.takeIf(String::isNotBlank)
                ?: throw IOException("Snapshot response body is empty")
            val snapshot = decodeSnapshot(json)
            if (snapshot.accountId != accountId) {
                throw IOException("Snapshot response account does not match the active user")
            }
            snapshot
        }
    }

    override suspend fun write(snapshot: AccountCloudSnapshot): AccountCloudWriteResult =
        withContext(Dispatchers.IO) {
            val body = AccountCloudSnapshotCodec.toJson(snapshot).toRequestBody(JSON)
            val response = client.newCall(
                authorizedRequest(endpoint("v1", "sync", "snapshot"))
                    .put(body)
                    .build(),
            ).execute()
            response.use {
                if (it.code == 204) return@withContext acknowledgedRemoteWrite()
                if (!it.isSuccessful) throw IOException("Snapshot upload failed with HTTP ${it.code}")
                val json = it.body?.string()?.takeIf(String::isNotBlank)
                    ?: throw IOException("Snapshot acknowledgement body is empty")
                val acknowledged = decodeSnapshot(json)
                if (acknowledged.accountId != snapshot.accountId) {
                    throw IOException("Snapshot acknowledgement account does not match the active user")
                }
                if (
                    acknowledged.version != snapshot.version ||
                    acknowledged.updatedAt != snapshot.updatedAt ||
                    acknowledged.backup != snapshot.backup ||
                    acknowledged.driverProfileJson != snapshot.driverProfileJson
                ) {
                    throw IOException("Snapshot acknowledgement did not accept the uploaded version")
                }
                acknowledgedRemoteWrite()
            }
        }

    suspend fun getTelegramInbox(sinceUpdateId: Long): TelegramInboxListResponse =
        withContext(Dispatchers.IO) {
            val url = endpoint("v1", "telegram", "inbox").newBuilder()
                .addQueryParameter("sinceUpdateId", sinceUpdateId.coerceAtLeast(0).toString())
                .build()
            executeJsonGet(url, TelegramInboxListResponse::class.java)
        }

    suspend fun acknowledgeTelegramInbox(updateId: Long) = withContext(Dispatchers.IO) {
        val response = client.newCall(
            authorizedRequest(endpoint("v1", "telegram", "inbox", updateId.toString(), "ack"))
                .post(EMPTY_BODY)
                .build(),
        ).execute()
        response.use {
            if (it.code != 204) throw IOException("Telegram acknowledgement failed with HTTP ${it.code}")
        }
    }

    suspend fun createTelegramLinkToken(): TelegramLinkTokenResponse =
        withContext(Dispatchers.IO) {
            executeJsonPost(
                endpoint("v1", "telegram", "link-token"),
                EMPTY_BODY,
                TelegramLinkTokenResponse::class.java,
            )
        }

    fun telegramLinkDeepLink(
        token: TelegramLinkTokenResponse,
        botUsername: String = BuildConfig.TELEGRAM_SERVER_BOT_USERNAME,
    ): String? {
        val username = botUsername.trim().removePrefix("@")
        if (username.isBlank() || !username.all { it.isLetterOrDigit() || it == '_' }) return null
        return endpointForTelegram(username, token.token)
    }

    suspend fun registerPushToken(token: String) = withContext(Dispatchers.IO) {
        val request = DevicePushTokenRequest(
            deviceId = deviceId,
            token = token,
            platform = PushPlatforms.ANDROID,
        )
        val body = AccountCloudSnapshotCodec.gson.toJson(request).toRequestBody(JSON)
        val response = client.newCall(
            authorizedRequest(endpoint("v1", "devices", "push-token"))
                .put(body)
                .build(),
        ).execute()
        response.use {
            if (it.code != 204) throw IOException("Push token registration failed with HTTP ${it.code}")
        }
    }

    suspend fun deletePushToken() = withContext(Dispatchers.IO) {
        val url = endpoint("v1", "devices", "push-token").newBuilder()
            .addQueryParameter("deviceId", deviceId)
            .build()
        val response = client.newCall(authorizedRequest(url).delete().build()).execute()
        response.use {
            if (it.code != 204) throw IOException("Push token deletion failed with HTTP ${it.code}")
        }
    }

    private fun <T> executeJsonGet(url: HttpUrl, type: Class<T>): T {
        val response = client.newCall(authorizedRequest(url).get().build()).execute()
        response.use {
            if (!it.isSuccessful) throw IOException("Backend request failed with HTTP ${it.code}")
            val json = it.body?.string()?.takeIf(String::isNotBlank)
                ?: throw IOException("Backend response body is empty")
            return runCatching { AccountCloudSnapshotCodec.gson.fromJson(json, type) }
                .getOrNull()
                ?: throw IOException("Backend response is malformed")
        }
    }

    private fun <T> executeJsonPost(url: HttpUrl, body: okhttp3.RequestBody, type: Class<T>): T {
        val response = client.newCall(authorizedRequest(url).post(body).build()).execute()
        response.use {
            if (!it.isSuccessful) throw IOException("Backend request failed with HTTP ${it.code}")
            val json = it.body?.string()?.takeIf(String::isNotBlank)
                ?: throw IOException("Backend response body is empty")
            return runCatching { AccountCloudSnapshotCodec.gson.fromJson(json, type) }
                .getOrNull()
                ?: throw IOException("Backend response is malformed")
        }
    }

    private fun authorizedRequest(url: HttpUrl): Request.Builder {
        val token = accessToken()?.takeIf { it.isNotBlank() }
            ?: throw IOException("Authenticated backend session is unavailable")
        return Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("X-Device-Id", deviceId)
    }

    private fun endpoint(vararg segments: String): HttpUrl =
        baseUrl.newBuilder().apply {
            segments.forEach(::addPathSegment)
        }.build()

    private fun acknowledgedRemoteWrite() = AccountCloudWriteResult(
        localWritten = false,
        remoteConfigured = true,
        remoteAcknowledged = true,
    )

    private fun decodeSnapshot(json: String): AccountCloudSnapshot {
        val objectValue = runCatching {
            AccountCloudSnapshotCodec.gson.fromJson(json, JsonObject::class.java)
        }.getOrNull()
            ?: throw IOException("Snapshot response is malformed")
        val validEnvelope = runCatching {
            objectValue["version"]?.asInt?.let { it >= 1 } == true &&
                objectValue["accountId"]?.asString?.isNotBlank() == true &&
                objectValue["updatedAt"]?.asLong?.let { it >= 0 } == true &&
                objectValue["backup"]?.isJsonObject == true
        }.getOrDefault(false)
        if (!validEnvelope) throw IOException("Snapshot response is malformed")
        return AccountCloudSnapshotCodec.fromJson(json)
            ?: throw IOException("Snapshot response is malformed")
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private val EMPTY_BODY = ByteArray(0).toRequestBody(null)

        fun validatedBaseUrl(value: String, allowInsecureHttp: Boolean): HttpUrl {
            val url = value.trim().trimEnd('/').toHttpUrl()
            if (url.scheme == "https") return url
            val loopback = url.host.equals("localhost", ignoreCase = true) ||
                url.host == "127.0.0.1" ||
                url.host == "::1"
            require(url.scheme == "http" && (loopback || allowInsecureHttp)) {
                "SYNC_BACKEND_URL must use HTTPS outside loopback/debug builds"
            }
            return url
        }

        private fun endpointForTelegram(username: String, token: String): String =
            "https://t.me/$username?start=$token"
    }
}

object AccountCloudBackendFactory {
    fun create(context: Context): AccountCloudBackend {
        val app = context.applicationContext
        val local = LocalAccountCloudBackend(AccountCloudMirror(app))
        val remote = remoteClientOrNull(app) ?: return local
        return HybridAccountCloudBackend(local, remote)
    }

    fun remoteClientOrNull(context: Context): RemoteAccountCloudClient? {
        val url = BuildConfig.SYNC_BACKEND_URL.trim()
        if (BuildConfig.LOCAL_ONLY_MODE || url.isBlank()) return null
        val app = context.applicationContext
        return RemoteAccountCloudClient(
            backendUrl = url,
            accessToken = { AuthStore(app).accessTokenOrNull() },
            deviceId = DeviceIdentity(app).id(),
        )
    }
}
