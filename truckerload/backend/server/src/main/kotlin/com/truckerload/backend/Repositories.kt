package com.truckerload.backend

import com.truckerload.contract.AccountCloudSnapshot
import com.truckerload.contract.MediaKind
import com.truckerload.contract.MediaMetadata
import com.truckerload.contract.SyncCursor
import com.truckerload.contract.TelegramInboxItem
import java.util.UUID
import kotlinx.serialization.json.JsonObject

data class AuthenticatedUser(val id: UUID, val email: String?)

data class MediaRecord(
    val id: UUID,
    val userId: UUID,
    val objectKey: String,
    val fileName: String,
    val contentType: String,
    val sizeBytes: Long,
    val checksum: String?,
    val kind: MediaKind,
    val clientId: String,
    val loadId: String?,
    val metadata: JsonObject,
    val status: String,
    val createdAt: Long,
    val completedAt: Long?,
    val updatedAt: Long,
    val deletedAt: Long?,
    val revision: Long = 0,
) {
    fun toContract(download: PresignedDownload? = null): MediaMetadata = MediaMetadata(
        mediaId = id.toString(),
        fileName = fileName,
        contentType = contentType,
        sizeBytes = sizeBytes,
        checksum = checksum,
        status = if (deletedAt == null) status else "deleted",
        createdAt = createdAt,
        completedAt = completedAt,
        kind = kind,
        clientId = clientId,
        loadId = loadId,
        metadata = metadata,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        downloadUrl = download?.url,
        expiresAt = download?.expiresAt,
    )
}

data class MediaCreateResult(
    val record: MediaRecord,
    val created: Boolean,
)

data class TelegramInboxRecord(
    val updateId: Long,
    val userId: UUID,
    val messageId: Long,
    val chatId: Long,
    val text: String,
    val senderUsername: String?,
    val receivedAt: Long,
    val acknowledgedAt: Long?,
) {
    fun toContract() = TelegramInboxItem(
        updateId = updateId,
        messageId = messageId,
        chatId = chatId,
        text = text,
        senderUsername = senderUsername,
        receivedAt = receivedAt,
        acknowledgedAt = acknowledgedAt,
    )
}

data class SnapshotPutResult(
    val snapshot: AccountCloudSnapshot,
    val accepted: Boolean,
)

data class DevicePushTokenRecord(
    val userId: UUID,
    val deviceId: String,
    val token: String,
    val platform: String,
    val updatedAt: Long,
)

interface UserRepository {
    suspend fun upsert(user: AuthenticatedUser)
}

interface SnapshotRepository {
    suspend fun get(userId: UUID): AccountCloudSnapshot?
    /** Returns the row after applying strict last-write-wins semantics. */
    suspend fun putLww(userId: UUID, snapshot: AccountCloudSnapshot, checksum: String): SnapshotPutResult
}

interface CursorRepository {
    suspend fun get(userId: UUID, deviceId: String): SyncCursor?
    suspend fun put(userId: UUID, cursor: SyncCursor): SyncCursor
}

interface TelegramRepository {
    suspend fun createLinkToken(userId: UUID, tokenHash: ByteArray, expiresAt: Long)
    suspend fun consumeLinkTokenAndLink(
        tokenHash: ByteArray,
        chatId: Long,
        username: String?,
        now: Long,
    ): UUID?

    suspend fun linkedUser(chatId: Long): UUID?
    suspend fun insertInbox(record: TelegramInboxRecord): Boolean
    suspend fun listInbox(userId: UUID, sinceUpdateId: Long, limit: Int): List<TelegramInboxRecord>
    suspend fun acknowledge(userId: UUID, updateId: Long, acknowledgedAt: Long): Boolean
    suspend fun unlink(userId: UUID): Boolean
}

interface MediaRepository {
    suspend fun createOrGet(record: MediaRecord): MediaCreateResult
    suspend fun get(userId: UUID, mediaId: UUID): MediaRecord?
    suspend fun getById(mediaId: UUID): MediaRecord?
    suspend fun list(userId: UUID, sinceRevision: Long, kind: MediaKind?, limit: Int): List<MediaRecord>
    suspend fun markComplete(userId: UUID, mediaId: UUID, checksum: String?, completedAt: Long): MediaRecord?
    suspend fun softDelete(userId: UUID, mediaId: UUID, deletedAt: Long): MediaRecord?
}

interface PushTokenRepository {
    suspend fun upsert(record: DevicePushTokenRecord)
    suspend fun delete(userId: UUID, deviceId: String): Boolean
    suspend fun listForUser(userId: UUID, excludingDeviceId: String?): List<DevicePushTokenRecord>
}

interface DatabaseHealth {
    suspend fun isReady(): Boolean
}

data class Repositories(
    val users: UserRepository,
    val snapshots: SnapshotRepository,
    val cursors: CursorRepository,
    val telegram: TelegramRepository,
    val media: MediaRepository,
    val pushTokens: PushTokenRepository,
    val health: DatabaseHealth,
)
