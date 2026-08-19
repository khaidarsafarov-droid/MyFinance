package com.truckerload.contract

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray

/** The single tolerant JSON configuration used at API boundaries. */
val ContractJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

@Serializable
data class HealthResponse(
    val status: String,
    val service: String = "truckerload-backend",
    val timestamp: Long,
    val version: String = "dev",
)

/**
 * Compatible with the Gson JSON currently written by the Android app.
 * [entityCount] is absent in old files and is safely derived from known backup arrays.
 */
@Serializable
data class AccountCloudSnapshot(
    val version: Int = 1,
    val accountId: String,
    val updatedAt: Long,
    val backup: JsonObject = buildJsonObject { },
    val driverProfileJson: String? = null,
    val entityCount: Int? = null,
) {
    fun resolvedEntityCount(): Int {
        entityCount?.let { return it.coerceAtLeast(0) }
        val count = listOf("loads", "paychecks", "diesel").sumOf { key ->
            runCatching { backup[key]?.jsonArray?.size?.toLong() ?: 0L }.getOrDefault(0L)
        }
        return count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    fun withResolvedEntityCount(): AccountCloudSnapshot = copy(entityCount = resolvedEntityCount())
}

@Serializable
data class SyncCursor(
    val deviceId: String,
    val cursor: Long,
    val updatedAt: Long = 0,
)

@Serializable
enum class MediaKind {
    PHOTO,
    SCAN,
}

@Serializable
data class MediaUploadRequest(
    val fileName: String,
    val contentType: String,
    val sizeBytes: Long,
    val checksum: String? = null,
    // Nullable defaults keep old serialized payloads decodable. The media endpoint
    // requires both fields for new uploads.
    val kind: MediaKind? = null,
    val clientId: String? = null,
    val loadId: String? = null,
    val metadata: JsonObject = buildJsonObject { },
)

@Serializable
data class MediaUploadResponse(
    val mediaId: String,
    val uploadUrl: String? = null,
    val method: String = "PUT",
    val headers: Map<String, String> = emptyMap(),
    val expiresAt: Long,
    val alreadyComplete: Boolean = false,
    val media: MediaMetadata? = null,
)

@Serializable
data class MediaUploadCompleteRequest(
    val mediaId: String,
    val checksum: String? = null,
)

@Serializable
data class MediaMetadata(
    val mediaId: String,
    val fileName: String,
    val contentType: String,
    val sizeBytes: Long,
    val checksum: String? = null,
    val status: String,
    val createdAt: Long,
    val completedAt: Long? = null,
    val kind: MediaKind = MediaKind.SCAN,
    val clientId: String = mediaId,
    val loadId: String? = null,
    val metadata: JsonObject = buildJsonObject { },
    val updatedAt: Long = completedAt ?: createdAt,
    val deletedAt: Long? = null,
    val downloadUrl: String? = null,
    val expiresAt: Long? = null,
)

@Serializable
data class MediaListResponse(
    val items: List<MediaMetadata>,
    /** Opaque, account-scoped revision to pass back as `since`. */
    val nextSince: Long,
)

@Serializable
data class TelegramLinkTokenResponse(
    val token: String,
    val expiresAt: Long,
)

@Serializable
data class TelegramInboxItem(
    val updateId: Long,
    val messageId: Long,
    val chatId: Long,
    val text: String,
    val senderUsername: String? = null,
    val receivedAt: Long,
    val acknowledgedAt: Long? = null,
)

@Serializable
data class TelegramInboxListResponse(
    val items: List<TelegramInboxItem>,
    val nextUpdateId: Long? = items.maxOfOrNull { it.updateId },
)

@Serializable
data class DevicePushTokenRequest(
    val deviceId: String,
    val token: String,
    val platform: String = PushPlatforms.ANDROID,
)

@Serializable
data class ApiError(
    val code: String,
    val message: String,
    val requestId: String? = null,
    val details: JsonElement? = null,
)
