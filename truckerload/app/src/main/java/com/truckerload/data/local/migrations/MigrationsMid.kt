package com.truckerload.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Room migrations for schema versions 16→24 (startVersion 16..24). */

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS voice_rooms (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                creatorId TEXT NOT NULL,
                maxParticipants INTEGER NOT NULL,
                isActive INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS voice_room_participants (
                roomId TEXT NOT NULL,
                userId TEXT NOT NULL,
                displayName TEXT NOT NULL,
                isMuted INTEGER NOT NULL DEFAULT 0,
                isDeafened INTEGER NOT NULL DEFAULT 0,
                isSpeaking INTEGER NOT NULL DEFAULT 0,
                audioLevel INTEGER NOT NULL DEFAULT 0,
                joinedAt INTEGER NOT NULL,
                PRIMARY KEY(roomId, userId)
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_voice_room_participants_roomId " +
                "ON voice_room_participants (roomId)",
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS call_sessions (
                callId TEXT NOT NULL PRIMARY KEY,
                type TEXT NOT NULL,
                status TEXT NOT NULL,
                callerId TEXT NOT NULL,
                callerName TEXT NOT NULL,
                calleeId TEXT,
                calleeName TEXT,
                isIncoming INTEGER NOT NULL,
                startedAt INTEGER NOT NULL,
                endedAt INTEGER,
                durationMs INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_call_sessions_status ON call_sessions (status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_call_sessions_startedAt ON call_sessions (startedAt)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS voice_signals (
                id TEXT NOT NULL PRIMARY KEY,
                sessionId TEXT NOT NULL,
                fromUserId TEXT NOT NULL,
                type TEXT NOT NULL,
                sdp TEXT,
                candidate TEXT,
                sdpMid TEXT,
                sdpMLineIndex INTEGER,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_voice_signals_sessionId_timestamp " +
                "ON voice_signals (sessionId, timestamp)",
        )
    }
}

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS driver_follows (
                followerId TEXT NOT NULL,
                followingId TEXT NOT NULL,
                followedAt INTEGER NOT NULL,
                PRIMARY KEY(followerId, followingId)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS chat_members (
                chatId TEXT NOT NULL,
                userId TEXT NOT NULL,
                displayName TEXT NOT NULL,
                role TEXT NOT NULL DEFAULT 'MEMBER',
                joinedAt INTEGER NOT NULL,
                PRIMARY KEY(chatId, userId)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_members_chatId ON chat_members (chatId)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS social_peers (
                id TEXT NOT NULL PRIMARY KEY,
                displayName TEXT NOT NULL,
                rating REAL NOT NULL,
                weeklyMiles REAL NOT NULL,
                weeklyRevenue REAL NOT NULL,
                weeklyLoads INTEGER NOT NULL,
                weeklyRpm REAL NOT NULL
            )
            """.trimIndent(),
        )
        db.addColumnIfMissing("social_chats", "creatorId", "TEXT NOT NULL DEFAULT ''")
        db.addColumnIfMissing("social_chats", "inviteCode", "TEXT NOT NULL DEFAULT ''")
        db.addColumnIfMissing("social_messages", "durationMs", "INTEGER NOT NULL DEFAULT 0")
        db.addColumnIfMissing("driver_statuses", "durationMs", "INTEGER NOT NULL DEFAULT 0")
    }
}

/**
 * Idempotent repair pass for devices that interrupted an earlier migration.
 * Re-applies every column added via ALTER TABLE across v6–v18, plus dispute fields.
 */
val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.addColumnIfMissing("loads", "firstPuMillis", "INTEGER")
        db.addColumnIfMissing("loads", "lastDelMillis", "INTEGER")
        db.addColumnIfMissing("loads", "route", "TEXT NOT NULL DEFAULT ''")
        db.addColumnIfMissing("loads", "firstPuCityState", "TEXT NOT NULL DEFAULT ''")
        db.addColumnIfMissing("loads", "lastDelCityState", "TEXT NOT NULL DEFAULT ''")
        db.addColumnIfMissing("loads", "durationDays", "REAL NOT NULL DEFAULT 0")
        db.addColumnIfMissing("loads", "pace", "REAL NOT NULL DEFAULT 0")
        db.addColumnIfMissing("loads", "stopCount", "INTEGER NOT NULL DEFAULT 0")
        db.addColumnIfMissing("loads", "isDispute", "INTEGER NOT NULL DEFAULT 0")
        db.addColumnIfMissing("loads", "disputeResponseDate", "TEXT")
        db.addColumnIfMissing("loads", "disputeCompleted", "INTEGER NOT NULL DEFAULT 0")
        db.addColumnIfMissing("social_chats", "onlineCount", "INTEGER NOT NULL DEFAULT 0")
        db.addColumnIfMissing("social_chats", "description", "TEXT NOT NULL DEFAULT ''")
        db.addColumnIfMissing("social_chats", "rating", "REAL NOT NULL DEFAULT 4.5")
        db.addColumnIfMissing("social_chats", "isPublic", "INTEGER NOT NULL DEFAULT 1")
        db.addColumnIfMissing("social_chats", "category", "TEXT NOT NULL DEFAULT ''")
        db.addColumnIfMissing("social_chats", "archived", "INTEGER NOT NULL DEFAULT 0")
        db.addColumnIfMissing("social_chats", "creatorId", "TEXT NOT NULL DEFAULT ''")
        db.addColumnIfMissing("social_chats", "inviteCode", "TEXT NOT NULL DEFAULT ''")
        db.addColumnIfMissing("social_messages", "attachmentUrl", "TEXT")
        db.addColumnIfMissing("social_messages", "replyToId", "TEXT")
        db.addColumnIfMissing("social_messages", "locationLabel", "TEXT")
        db.addColumnIfMissing("social_messages", "isAnnouncement", "INTEGER NOT NULL DEFAULT 0")
        db.addColumnIfMissing("social_messages", "messageType", "TEXT NOT NULL DEFAULT 'TEXT'")
        db.addColumnIfMissing("social_messages", "durationMs", "INTEGER NOT NULL DEFAULT 0")
        db.addColumnIfMissing("driver_profile", "coverImageUrl", "TEXT")
        db.addColumnIfMissing("driver_profile", "licenseClass", "TEXT NOT NULL DEFAULT 'A'")
        db.addColumnIfMissing("driver_profile", "endorsementsJson", "TEXT NOT NULL DEFAULT ''")
        db.addColumnIfMissing("driver_profile", "maxRadius", "INTEGER NOT NULL DEFAULT 500")
        db.addColumnIfMissing("driver_profile", "specialtiesJson", "TEXT NOT NULL DEFAULT ''")
        db.addColumnIfMissing("driver_profile", "languagesJson", "TEXT NOT NULL DEFAULT 'English,Russian'")
        db.addColumnIfMissing("driver_profile", "phoneNumber", "TEXT")
        db.addColumnIfMissing("driver_profile", "telegramUsername", "TEXT")
        db.addColumnIfMissing("driver_profile", "whatsappNumber", "TEXT")
        db.addColumnIfMissing("driver_profile", "reputation", "INTEGER NOT NULL DEFAULT 0")
        db.addColumnIfMissing("driver_profile", "followers", "INTEGER NOT NULL DEFAULT 0")
        db.addColumnIfMissing("driver_profile", "following", "INTEGER NOT NULL DEFAULT 0")
        db.addColumnIfMissing("driver_profile", "ratingCount", "INTEGER NOT NULL DEFAULT 124")
        db.addColumnIfMissing("driver_profile", "currentRoute", "TEXT")
        db.addColumnIfMissing("driver_statuses", "durationMs", "INTEGER NOT NULL DEFAULT 0")
    }
}

/** Индекс для фильтрации по неделе/году (THIS_WEEK, LAST_WEEK и т.д.). */
val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS index_loads_weekNumber_year ON loads(weekNumber, year)")
    }
}

/** Фактическая дата окончания груза (override водителя). */
val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.addColumnIfMissing("loads", "actualFinishDate", "TEXT")
    }
}

/** Привязка сканов документов к грузу (как у фото). */
val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.addColumnIfMissing("scans", "loadId", "TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_scans_loadId ON scans(loadId)")
    }
}

/** Week/year indexes for diesel + paychecks (widget/tax year filters). */
val MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS index_diesel_weekNumber_year ON diesel(weekNumber, year)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_diesel_addedAt ON diesel(addedAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_paychecks_weekNumber_year ON paychecks(weekNumber, year)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_paychecks_addedAt ON paychecks(addedAt)")
    }
}

/** Driver professional fields + hybrid outbound sync outbox. */
val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.addColumnIfMissing("driver_profile", "dateOfBirthEpochDay", "INTEGER")
        db.addColumnIfMissing("driver_profile", "cdlNumber", "TEXT NOT NULL DEFAULT ''")
        db.addColumnIfMissing("driver_profile", "axleCount", "INTEGER NOT NULL DEFAULT 0")
        db.addColumnIfMissing("driver_profile", "homeHubCity", "TEXT NOT NULL DEFAULT ''")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS sync_outbox (
                id TEXT NOT NULL PRIMARY KEY,
                entityType TEXT NOT NULL,
                entityId TEXT NOT NULL,
                op TEXT NOT NULL,
                payloadJson TEXT NOT NULL,
                status TEXT NOT NULL,
                attempts INTEGER NOT NULL,
                lastError TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_sync_outbox_status_createdAt " +
                "ON sync_outbox(status, createdAt)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_sync_outbox_entityType_entityId " +
                "ON sync_outbox(entityType, entityId)",
        )
    }
}

/** Query indexes for loads/social/voice/outbox (QUALITY_IDEAL_1000). */
val MIGRATION_24_25 = object : Migration(24, 25) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS index_loads_parsedAt ON loads(parsedAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_loads_updatedAt ON loads(updatedAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_loads_firstPuCityState ON loads(firstPuCityState)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_loads_isDispute_disputeCompleted " +
                "ON loads(isDispute, disputeCompleted)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_social_chats_archived_lastMessageAt " +
                "ON social_chats(archived, lastMessageAt)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_social_chats_inviteCode " +
                "ON social_chats(inviteCode)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_driver_follows_followingId " +
                "ON driver_follows(followingId)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_members_userId ON chat_members(userId)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_social_peers_weeklyMiles ON social_peers(weeklyMiles)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_challenge_participation_challengeId_score " +
                "ON challenge_participation(challengeId, score)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_voice_rooms_isActive_updatedAt " +
                "ON voice_rooms(isActive, updatedAt)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_call_sessions_status_isIncoming_startedAt " +
                "ON call_sessions(status, isIncoming, startedAt)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_sync_outbox_status_updatedAt " +
                "ON sync_outbox(status, updatedAt)",
        )
    }
}
