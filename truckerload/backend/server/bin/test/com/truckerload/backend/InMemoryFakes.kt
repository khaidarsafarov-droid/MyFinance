package com.truckerload.backend

import com.truckerload.contract.AccountCloudSnapshot
import com.truckerload.contract.MediaKind
import com.truckerload.contract.SyncCursor
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryBackend {
    val users = ConcurrentHashMap<UUID, AuthenticatedUser>()
    val snapshots = ConcurrentHashMap<UUID, AccountCloudSnapshot>()
    val cursors = ConcurrentHashMap<Pair<UUID, String>, SyncCursor>()
    val tokens = ConcurrentHashMap<String, Pair<UUID, Long>>()
    val links = ConcurrentHashMap<Long, UUID>()
    val inbox = ConcurrentHashMap<Long, TelegramInboxRecord>()
    val media = ConcurrentHashMap<UUID, MediaRecord>()
    val pushTokens = ConcurrentHashMap<Pair<UUID, String>, DevicePushTokenRecord>()

    val repositories = Repositories(
        users = object : UserRepository {
            override suspend fun upsert(user: AuthenticatedUser) {
                users[user.id] = user
            }
        },
        snapshots = object : SnapshotRepository {
            override suspend fun get(userId: UUID): AccountCloudSnapshot? = snapshots[userId]

            override suspend fun putLww(
                userId: UUID,
                snapshot: AccountCloudSnapshot,
                checksum: String,
            ): SnapshotPutResult {
                var accepted = false
                val stored = snapshots.compute(userId) { _, current ->
                    if (current == null || snapshot.updatedAt > current.updatedAt) {
                        accepted = true
                        snapshot.copy(accountId = userId.toString()).withResolvedEntityCount()
                    } else {
                        current
                    }
                }!!
                return SnapshotPutResult(stored, accepted)
            }
        },
        cursors = object : CursorRepository {
            override suspend fun get(userId: UUID, deviceId: String): SyncCursor? =
                cursors[userId to deviceId]

            override suspend fun put(userId: UUID, cursor: SyncCursor): SyncCursor =
                cursor.copy(updatedAt = System.currentTimeMillis()).also {
                    cursors[userId to cursor.deviceId] = it
                }
        },
        telegram = object : TelegramRepository {
            override suspend fun createLinkToken(
                userId: UUID,
                tokenHash: ByteArray,
                expiresAt: Long
            ) {
                tokens.entries.removeIf { it.value.first == userId }
                tokens[tokenKey(tokenHash)] = userId to expiresAt
            }

            override suspend fun consumeLinkTokenAndLink(
                tokenHash: ByteArray,
                chatId: Long,
                username: String?,
                now: Long,
            ): UUID? {
                val token = tokens.remove(tokenKey(tokenHash)) ?: return null
                if (token.second <= now) return null
                links.entries.removeIf { it.value == token.first || it.key == chatId }
                links[chatId] = token.first
                return token.first
            }

            override suspend fun linkedUser(chatId: Long): UUID? = links[chatId]

            override suspend fun insertInbox(record: TelegramInboxRecord): Boolean =
                inbox.putIfAbsent(record.updateId, record) == null

            override suspend fun listInbox(
                userId: UUID,
                sinceUpdateId: Long,
                limit: Int,
            ): List<TelegramInboxRecord> = inbox.values
                .filter { it.userId == userId && it.updateId > sinceUpdateId }
                .sortedBy { it.updateId }
                .take(limit)

            override suspend fun acknowledge(
                userId: UUID,
                updateId: Long,
                acknowledgedAt: Long
            ): Boolean {
                val current = inbox[updateId]?.takeIf { it.userId == userId } ?: return false
                inbox[updateId] =
                    current.copy(acknowledgedAt = current.acknowledgedAt ?: acknowledgedAt)
                return true
            }

            override suspend fun unlink(userId: UUID): Boolean =
                links.entries.removeIf { it.value == userId }
        },
        media = object : MediaRepository {
            override suspend fun createOrGet(record: MediaRecord): MediaCreateResult {
                val existing = media.values.firstOrNull {
                    it.userId == record.userId && it.kind == record.kind && it.clientId == record.clientId
                }
                if (existing != null) {
                    val compatible = existing.deletedAt == null &&
                            existing.fileName == record.fileName &&
                            existing.contentType == record.contentType &&
                            existing.sizeBytes == record.sizeBytes &&
                            (existing.checksum == null || record.checksum == null || existing.checksum == record.checksum)
                    val updated = if (compatible &&
                        (
                                existing.loadId != record.loadId ||
                                        existing.metadata != record.metadata ||
                                        (existing.checksum == null && record.checksum != null)
                                )
                    ) {
                        existing.copy(
                            loadId = record.loadId,
                            metadata = record.metadata,
                            checksum = existing.checksum ?: record.checksum,
                            updatedAt = record.updatedAt,
                            revision = nextMediaRevision(),
                        ).also { media[existing.id] = it }
                    } else {
                        existing
                    }
                    return MediaCreateResult(updated, created = false)
                }
                media[record.id] = record.copy(revision = nextMediaRevision())
                return MediaCreateResult(media.getValue(record.id), created = true)
            }

            override suspend fun get(userId: UUID, mediaId: UUID): MediaRecord? =
                media[mediaId]?.takeIf { it.userId == userId && it.deletedAt == null }

            override suspend fun getById(mediaId: UUID): MediaRecord? =
                media[mediaId]?.takeIf { it.deletedAt == null }

            override suspend fun list(
                userId: UUID,
                sinceRevision: Long,
                kind: MediaKind?,
                limit: Int,
            ): List<MediaRecord> = media.values
                .filter { it.userId == userId && it.revision > sinceRevision && (kind == null || it.kind == kind) }
                .sortedBy { it.revision }
                .take(limit)

            override suspend fun markComplete(
                userId: UUID,
                mediaId: UUID,
                checksum: String?,
                completedAt: Long,
            ): MediaRecord? {
                val existing = get(userId, mediaId) ?: return null
                return existing.copy(
                    checksum = checksum ?: existing.checksum,
                    status = "ready",
                    completedAt = completedAt,
                    updatedAt = completedAt,
                    revision = nextMediaRevision(),
                ).also { media[mediaId] = it }
            }

            override suspend fun softDelete(
                userId: UUID,
                mediaId: UUID,
                deletedAt: Long
            ): MediaRecord? {
                val existing = media[mediaId]?.takeIf { it.userId == userId } ?: return null
                if (existing.deletedAt != null) return existing
                return existing.copy(
                    deletedAt = deletedAt,
                    updatedAt = deletedAt,
                    revision = nextMediaRevision(),
                ).also { media[mediaId] = it }
            }
        },
        pushTokens = object : PushTokenRepository {
            override suspend fun upsert(record: DevicePushTokenRecord) {
                pushTokens.entries.removeIf {
                    it.value.token == record.token ||
                            (it.value.userId == record.userId && it.value.deviceId == record.deviceId)
                }
                pushTokens[record.userId to record.deviceId] = record
            }

            override suspend fun delete(userId: UUID, deviceId: String): Boolean =
                pushTokens.remove(userId to deviceId) != null

            override suspend fun listForUser(
                userId: UUID,
                excludingDeviceId: String?,
            ): List<DevicePushTokenRecord> = pushTokens.values.filter {
                it.userId == userId && it.deviceId != excludingDeviceId
            }
        },
        health = object : DatabaseHealth {
            override suspend fun isReady(): Boolean = true
        },
    )

    private var mediaRevision = 0L
    private fun nextMediaRevision(): Long = synchronized(this) { ++mediaRevision }

    private fun tokenKey(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
}

class FakeObjectStorage(
    var ready: Boolean = true,
) : ObjectStorage {
    val stored = ConcurrentHashMap<String, StoredObject>()

    override suspend fun presignUpload(
        mediaId: UUID,
        objectKey: String,
        contentType: String,
        sizeBytes: Long,
        expiresAt: Long,
    ): PresignedUpload = PresignedUpload(
        url = "https://storage.test/upload/$mediaId",
        headers = mapOf("Content-Type" to contentType, "Content-Length" to sizeBytes.toString()),
        expiresAt = expiresAt,
    )

    override suspend fun presignDownload(
        mediaId: UUID,
        objectKey: String,
        expiresAt: Long,
    ): PresignedDownload = PresignedDownload(
        url = "https://storage.test/download/$mediaId",
        expiresAt = expiresAt,
    )

    override suspend fun stat(objectKey: String): StoredObject? = stored[objectKey]

    override suspend fun delete(objectKey: String) {
        stored.remove(objectKey)
    }

    override suspend fun isReady(): Boolean = ready
}
