package com.truckerload.backend

import com.truckerload.contract.AccountCloudSnapshot
import com.truckerload.contract.ContractJson
import com.truckerload.contract.SyncCursor
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import org.flywaydb.core.Flyway

fun createDataSource(config: AppConfig): HikariDataSource {
    val hikari = HikariConfig().apply {
        jdbcUrl = config.databaseUrl
        config.databaseUser?.let { username = it }
        config.databasePassword?.let { password = it }
        maximumPoolSize = 10
        minimumIdle = 1
        connectionTimeout = 10_000
        validationTimeout = 3_000
        poolName = "truckerload-db"
    }
    return HikariDataSource(hikari)
}

fun migrateDatabase(dataSource: DataSource) {
    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .load()
        .migrate()
}

fun jdbcRepositories(dataSource: DataSource): Repositories = Repositories(
    users = JdbcUserRepository(dataSource),
    snapshots = JdbcSnapshotRepository(dataSource),
    cursors = JdbcCursorRepository(dataSource),
    telegram = JdbcTelegramRepository(dataSource),
    media = JdbcMediaRepository(dataSource),
    pushTokens = JdbcPushTokenRepository(dataSource),
    health = JdbcDatabaseHealth(dataSource),
)

private suspend fun <T> DataSource.query(block: (Connection) -> T): T =
    withContext(Dispatchers.IO) { connection.use(block) }

private fun <T> Connection.transaction(block: (Connection) -> T): T {
    val previous = autoCommit
    autoCommit = false
    return try {
        block(this).also { commit() }
    } catch (error: Throwable) {
        rollback()
        throw error
    } finally {
        autoCommit = previous
    }
}

private class JdbcUserRepository(private val dataSource: DataSource) : UserRepository {
    override suspend fun upsert(user: AuthenticatedUser) {
        dataSource.query { connection ->
            connection.prepareStatement(
                """
                INSERT INTO app_users (id, email)
                VALUES (?, ?)
                ON CONFLICT (id) DO UPDATE
                SET email = COALESCE(EXCLUDED.email, app_users.email), updated_at = now()
                """.trimIndent(),
            ).use { statement ->
                statement.setObject(1, user.id)
                statement.setString(2, user.email)
                statement.executeUpdate()
            }
        }
    }
}

private class JdbcSnapshotRepository(private val dataSource: DataSource) : SnapshotRepository {
    override suspend fun get(userId: UUID): AccountCloudSnapshot? =
        dataSource.query { it.selectSnapshot(userId) }

    override suspend fun putLww(
        userId: UUID,
        snapshot: AccountCloudSnapshot,
        checksum: String,
    ): SnapshotPutResult = dataSource.query { connection ->
        connection.transaction {
            val normalized = snapshot.copy(accountId = userId.toString()).withResolvedEntityCount()
            val accepted = connection.prepareStatement(
                """
                INSERT INTO account_snapshots (user_id, payload, updated_at, entity_count, checksum)
                VALUES (?, ?::jsonb, ?, ?, ?)
                ON CONFLICT (user_id) DO UPDATE SET
                    payload = EXCLUDED.payload,
                    updated_at = EXCLUDED.updated_at,
                    entity_count = EXCLUDED.entity_count,
                    checksum = EXCLUDED.checksum,
                    stored_at = now()
                WHERE account_snapshots.updated_at < EXCLUDED.updated_at
                """.trimIndent(),
            ).use { statement ->
                statement.setObject(1, userId)
                statement.setString(2, ContractJson.encodeToString(normalized))
                statement.setLong(3, normalized.updatedAt)
                statement.setInt(4, normalized.resolvedEntityCount())
                statement.setString(5, checksum)
                statement.executeUpdate() == 1
            }
            SnapshotPutResult(checkNotNull(connection.selectSnapshot(userId)), accepted)
        }
    }

    private fun Connection.selectSnapshot(userId: UUID): AccountCloudSnapshot? =
        prepareStatement("SELECT payload::text FROM account_snapshots WHERE user_id = ?").use { statement ->
            statement.setObject(1, userId)
            statement.executeQuery().use { result ->
                if (result.next()) ContractJson.decodeFromString<AccountCloudSnapshot>(result.getString(1)) else null
            }
        }
}

private class JdbcCursorRepository(private val dataSource: DataSource) : CursorRepository {
    override suspend fun get(userId: UUID, deviceId: String): SyncCursor? =
        dataSource.query { connection ->
            connection.prepareStatement(
                "SELECT cursor, updated_at FROM sync_cursors WHERE user_id = ? AND device_id = ?",
            ).use { statement ->
                statement.setObject(1, userId)
                statement.setString(2, deviceId)
                statement.executeQuery().use { result ->
                    if (result.next()) {
                        SyncCursor(deviceId, result.getLong("cursor"), result.getLong("updated_at"))
                    } else {
                        null
                    }
                }
            }
        }

    override suspend fun put(userId: UUID, cursor: SyncCursor): SyncCursor =
        dataSource.query { connection ->
            val stored = cursor.copy(updatedAt = System.currentTimeMillis())
            connection.prepareStatement(
                """
                INSERT INTO sync_cursors (user_id, device_id, cursor, updated_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (user_id, device_id) DO UPDATE
                SET cursor = EXCLUDED.cursor, updated_at = EXCLUDED.updated_at
                """.trimIndent(),
            ).use { statement ->
                statement.setObject(1, userId)
                statement.setString(2, stored.deviceId)
                statement.setLong(3, stored.cursor)
                statement.setLong(4, stored.updatedAt)
                statement.executeUpdate()
            }
            stored
        }
}

private class JdbcTelegramRepository(private val dataSource: DataSource) : TelegramRepository {
    override suspend fun createLinkToken(userId: UUID, tokenHash: ByteArray, expiresAt: Long) {
        dataSource.query { connection ->
            connection.transaction {
                connection.prepareStatement(
                    "UPDATE telegram_link_tokens SET used_at = now() WHERE user_id = ? AND used_at IS NULL",
                ).use {
                    it.setObject(1, userId)
                    it.executeUpdate()
                }
                connection.prepareStatement(
                    "INSERT INTO telegram_link_tokens (token_hash, user_id, expires_at) VALUES (?, ?, ?)",
                ).use {
                    it.setBytes(1, tokenHash)
                    it.setObject(2, userId)
                    it.setTimestamp(3, Timestamp.from(Instant.ofEpochMilli(expiresAt)))
                    it.executeUpdate()
                }
            }
        }
    }

    override suspend fun consumeLinkTokenAndLink(
        tokenHash: ByteArray,
        chatId: Long,
        username: String?,
        now: Long,
    ): UUID? = dataSource.query { connection ->
        connection.transaction {
            val userId = connection.prepareStatement(
                """
                SELECT user_id FROM telegram_link_tokens
                WHERE token_hash = ? AND used_at IS NULL AND expires_at > ?
                FOR UPDATE
                """.trimIndent(),
            ).use {
                it.setBytes(1, tokenHash)
                it.setTimestamp(2, Timestamp.from(Instant.ofEpochMilli(now)))
                it.executeQuery().use { result -> if (result.next()) result.getObject(1, UUID::class.java) else null }
            } ?: return@transaction null

            connection.prepareStatement(
                "UPDATE telegram_link_tokens SET used_at = ? WHERE token_hash = ?",
            ).use {
                it.setTimestamp(1, Timestamp.from(Instant.ofEpochMilli(now)))
                it.setBytes(2, tokenHash)
                it.executeUpdate()
            }
            connection.prepareStatement(
                "DELETE FROM telegram_links WHERE user_id = ? OR chat_id = ?",
            ).use {
                it.setObject(1, userId)
                it.setLong(2, chatId)
                it.executeUpdate()
            }
            connection.prepareStatement(
                "INSERT INTO telegram_links (user_id, chat_id, username) VALUES (?, ?, ?)",
            ).use {
                it.setObject(1, userId)
                it.setLong(2, chatId)
                it.setString(3, username)
                it.executeUpdate()
            }
            userId
        }
    }

    override suspend fun linkedUser(chatId: Long): UUID? =
        dataSource.query { connection ->
            connection.prepareStatement("SELECT user_id FROM telegram_links WHERE chat_id = ?").use {
                it.setLong(1, chatId)
                it.executeQuery().use { result ->
                    if (result.next()) result.getObject(1, UUID::class.java) else null
                }
            }
        }

    override suspend fun insertInbox(record: TelegramInboxRecord): Boolean =
        dataSource.query { connection ->
            connection.prepareStatement(
                """
                INSERT INTO telegram_inbox
                    (update_id, user_id, message_id, chat_id, message_text, sender_username, received_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (update_id) DO NOTHING
                """.trimIndent(),
            ).use {
                it.setLong(1, record.updateId)
                it.setObject(2, record.userId)
                it.setLong(3, record.messageId)
                it.setLong(4, record.chatId)
                it.setString(5, record.text)
                it.setString(6, record.senderUsername)
                it.setTimestamp(7, Timestamp.from(Instant.ofEpochMilli(record.receivedAt)))
                it.executeUpdate() == 1
            }
        }

    override suspend fun listInbox(
        userId: UUID,
        sinceUpdateId: Long,
        limit: Int,
    ): List<TelegramInboxRecord> = dataSource.query { connection ->
        connection.prepareStatement(
            """
            SELECT update_id, user_id, message_id, chat_id, message_text, sender_username,
                   received_at, acknowledged_at
            FROM telegram_inbox
            WHERE user_id = ? AND update_id > ?
            ORDER BY update_id
            LIMIT ?
            """.trimIndent(),
        ).use {
            it.setObject(1, userId)
            it.setLong(2, sinceUpdateId)
            it.setInt(3, limit)
            it.executeQuery().use { result ->
                buildList {
                    while (result.next()) add(result.toTelegramInbox())
                }
            }
        }
    }

    override suspend fun acknowledge(userId: UUID, updateId: Long, acknowledgedAt: Long): Boolean =
        dataSource.query { connection ->
            connection.prepareStatement(
                """
                UPDATE telegram_inbox
                SET acknowledged_at = COALESCE(acknowledged_at, ?)
                WHERE user_id = ? AND update_id = ?
                """.trimIndent(),
            ).use {
                it.setTimestamp(1, Timestamp.from(Instant.ofEpochMilli(acknowledgedAt)))
                it.setObject(2, userId)
                it.setLong(3, updateId)
                it.executeUpdate() == 1
            }
        }

    override suspend fun unlink(userId: UUID): Boolean =
        dataSource.query { connection ->
            connection.prepareStatement("DELETE FROM telegram_links WHERE user_id = ?").use {
                it.setObject(1, userId)
                it.executeUpdate() > 0
            }
        }
}

private class JdbcMediaRepository(private val dataSource: DataSource) : MediaRepository {
    override suspend fun create(record: MediaRecord) {
        dataSource.query { connection ->
            connection.prepareStatement(
                """
                INSERT INTO media_objects
                    (id, user_id, object_key, file_name, content_type, size_bytes, checksum, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
            ).use {
                it.bindMedia(record)
                it.executeUpdate()
            }
        }
    }

    override suspend fun get(userId: UUID, mediaId: UUID): MediaRecord? =
        dataSource.query { it.selectMedia(mediaId, userId) }

    override suspend fun getById(mediaId: UUID): MediaRecord? =
        dataSource.query { it.selectMedia(mediaId, null) }

    override suspend fun markComplete(
        userId: UUID,
        mediaId: UUID,
        checksum: String?,
        completedAt: Long,
    ): MediaRecord? = dataSource.query { connection ->
        connection.prepareStatement(
            """
            UPDATE media_objects
            SET status = 'ready', checksum = COALESCE(?, checksum), completed_at = ?
            WHERE id = ? AND user_id = ?
            """.trimIndent(),
        ).use {
            it.setString(1, checksum)
            it.setTimestamp(2, Timestamp.from(Instant.ofEpochMilli(completedAt)))
            it.setObject(3, mediaId)
            it.setObject(4, userId)
            it.executeUpdate()
        }
        connection.selectMedia(mediaId, userId)
    }

    override suspend fun delete(userId: UUID, mediaId: UUID): MediaRecord? =
        dataSource.query { connection ->
            connection.transaction {
                val existing = connection.selectMedia(mediaId, userId) ?: return@transaction null
                connection.prepareStatement("DELETE FROM media_objects WHERE id = ? AND user_id = ?").use {
                    it.setObject(1, mediaId)
                    it.setObject(2, userId)
                    it.executeUpdate()
                }
                existing
            }
        }

    private fun Connection.selectMedia(mediaId: UUID, userId: UUID?): MediaRecord? {
        val sql = buildString {
            append(
                """
                SELECT id, user_id, object_key, file_name, content_type, size_bytes, checksum,
                       status, created_at, completed_at
                FROM media_objects WHERE id = ?
                """.trimIndent(),
            )
            if (userId != null) append(" AND user_id = ?")
        }
        return prepareStatement(sql).use {
            it.setObject(1, mediaId)
            if (userId != null) it.setObject(2, userId)
            it.executeQuery().use { result -> if (result.next()) result.toMediaRecord() else null }
        }
    }
}

private class JdbcPushTokenRepository(private val dataSource: DataSource) : PushTokenRepository {
    override suspend fun upsert(record: DevicePushTokenRecord) {
        dataSource.query { connection ->
            connection.transaction {
                connection.prepareStatement(
                    """
                    DELETE FROM device_push_tokens
                    WHERE token = ? OR (user_id = ? AND device_id = ?)
                    """.trimIndent(),
                ).use {
                    it.setString(1, record.token)
                    it.setObject(2, record.userId)
                    it.setString(3, record.deviceId)
                    it.executeUpdate()
                }
                connection.prepareStatement(
                    """
                    INSERT INTO device_push_tokens (user_id, device_id, token, platform, updated_at)
                    VALUES (?, ?, ?, ?, ?)
                    """.trimIndent(),
                ).use {
                    it.setObject(1, record.userId)
                    it.setString(2, record.deviceId)
                    it.setString(3, record.token)
                    it.setString(4, record.platform)
                    it.setTimestamp(5, Timestamp.from(Instant.ofEpochMilli(record.updatedAt)))
                    it.executeUpdate()
                }
            }
        }
    }

    override suspend fun delete(userId: UUID, deviceId: String): Boolean =
        dataSource.query { connection ->
            connection.prepareStatement(
                "DELETE FROM device_push_tokens WHERE user_id = ? AND device_id = ?",
            ).use {
                it.setObject(1, userId)
                it.setString(2, deviceId)
                it.executeUpdate() > 0
            }
        }

    override suspend fun listForUser(
        userId: UUID,
        excludingDeviceId: String?,
    ): List<DevicePushTokenRecord> = dataSource.query { connection ->
        val sql = buildString {
            append(
                """
                SELECT user_id, device_id, token, platform, updated_at
                FROM device_push_tokens
                WHERE user_id = ?
                """.trimIndent(),
            )
            if (excludingDeviceId != null) append(" AND device_id <> ?")
        }
        connection.prepareStatement(sql).use {
            it.setObject(1, userId)
            if (excludingDeviceId != null) it.setString(2, excludingDeviceId)
            it.executeQuery().use { result ->
                buildList {
                    while (result.next()) {
                        add(
                            DevicePushTokenRecord(
                                userId = result.getObject("user_id", UUID::class.java),
                                deviceId = result.getString("device_id"),
                                token = result.getString("token"),
                                platform = result.getString("platform"),
                                updatedAt = result.getTimestamp("updated_at").time,
                            ),
                        )
                    }
                }
            }
        }
    }
}

private class JdbcDatabaseHealth(private val dataSource: DataSource) : DatabaseHealth {
    override suspend fun isReady(): Boolean = runCatching {
        dataSource.query { connection ->
            connection.prepareStatement("SELECT 1").use { statement ->
                statement.executeQuery().use { it.next() && it.getInt(1) == 1 }
            }
        }
    }.getOrDefault(false)
}

private fun java.sql.PreparedStatement.bindMedia(record: MediaRecord) {
    setObject(1, record.id)
    setObject(2, record.userId)
    setString(3, record.objectKey)
    setString(4, record.fileName)
    setString(5, record.contentType)
    setLong(6, record.sizeBytes)
    setString(7, record.checksum)
    setString(8, record.status)
    setTimestamp(9, Timestamp.from(Instant.ofEpochMilli(record.createdAt)))
}

private fun ResultSet.toMediaRecord(): MediaRecord = MediaRecord(
    id = getObject("id", UUID::class.java),
    userId = getObject("user_id", UUID::class.java),
    objectKey = getString("object_key"),
    fileName = getString("file_name"),
    contentType = getString("content_type"),
    sizeBytes = getLong("size_bytes"),
    checksum = getString("checksum"),
    status = getString("status"),
    createdAt = getTimestamp("created_at").time,
    completedAt = getTimestamp("completed_at")?.time,
)

private fun ResultSet.toTelegramInbox(): TelegramInboxRecord = TelegramInboxRecord(
    updateId = getLong("update_id"),
    userId = getObject("user_id", UUID::class.java),
    messageId = getLong("message_id"),
    chatId = getLong("chat_id"),
    text = getString("message_text"),
    senderUsername = getString("sender_username"),
    receivedAt = getTimestamp("received_at").time,
    acknowledgedAt = getTimestamp("acknowledged_at")?.time,
)
