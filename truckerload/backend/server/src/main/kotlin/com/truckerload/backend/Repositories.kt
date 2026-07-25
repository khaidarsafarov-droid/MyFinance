package com.truckerload.backend

import com.truckerload.contract.AccountCloudSnapshot
import com.truckerload.contract.MediaMetadata
import com.truckerload.contract.SyncCursor
import com.truckerload.contract.TelegramInboxItem
import java.util.UUID

data class AuthenticatedUser(val id: UUID, val email: String?)

data class MediaRecord(
    val id: UUID,
    val userId: UUID,
    val objectKey: String,
    val fileName: String,
    val contentType: String,
    val sizeBytes: Long,
    val checksum: String?,
    val status: String,
    val createdAt: Long,
    val completedAt: Long?,
) {
    fun toContract(): MediaMetadata = MediaMetadata(
        mediaId = id.toString(),
        fileName = fileName,
        contentType = contentType,
        sizeBytes = sizeBytes,
        checksum = checksum,
        status = status,
        createdAt = createdAt,
        completedAt = completedAt,
    )
}

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

interface UserRepository {
    suspend fun upsert(user: AuthenticatedUser)
}

interface SnapshotRepository {
    suspend fun get(userId: UUID): AccountCloudSnapshot?
    /** Returns the row after applying strict last-write-wins semantics. */
    suspend fun putLww(userId: UUID, snapshot: AccountCloudSnapshot, checksum: String): AccountCloudSnapshot
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
    suspend fun create(record: MediaRecord)
    suspend fun get(userId: UUID, mediaId: UUID): MediaRecord?
    suspend fun getById(mediaId: UUID): MediaRecord?
    suspend fun markComplete(userId: UUID, mediaId: UUID, checksum: String?, completedAt: Long): MediaRecord?
    suspend fun delete(userId: UUID, mediaId: UUID): MediaRecord?
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
    val health: DatabaseHealth,
)
