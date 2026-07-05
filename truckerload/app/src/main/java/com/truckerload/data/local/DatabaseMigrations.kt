package com.truckerload.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Добавляет PU/DEL millis без удаления существующих грузов. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE loads ADD COLUMN firstPuMillis INTEGER")
        db.execSQL("ALTER TABLE loads ADD COLUMN lastDelMillis INTEGER")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS telegram_inbox (
                updateId INTEGER NOT NULL PRIMARY KEY,
                chatId TEXT NOT NULL,
                text TEXT NOT NULL,
                messageDateSeconds INTEGER,
                receivedAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_telegram_inbox_chatId_messageDateSeconds " +
                "ON telegram_inbox (chatId, messageDateSeconds)",
        )
    }
}

/** Route metrics: duration, pace, stop count for weekly yield. */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE loads ADD COLUMN route TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE loads ADD COLUMN firstPuCityState TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE loads ADD COLUMN lastDelCityState TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE loads ADD COLUMN durationDays REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE loads ADD COLUMN pace REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE loads ADD COLUMN stopCount INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            """
            UPDATE loads SET
                route = pointA || ' → ' || pointB,
                firstPuCityState = pointA,
                lastDelCityState = pointB
            WHERE pointA != '' AND pointB != ''
            """.trimIndent(),
        )
        db.execSQL(
            """
            UPDATE loads SET durationDays = MAX(1.0,
                CAST((lastDelMillis - firstPuMillis + 86399999) / 86400000 AS REAL))
            WHERE firstPuMillis IS NOT NULL
              AND lastDelMillis IS NOT NULL
              AND lastDelMillis > firstPuMillis
              AND durationDays = 0
            """.trimIndent(),
        )
        db.execSQL(
            """
            UPDATE loads SET pace = totalRate / durationDays
            WHERE durationDays > 0 AND pace = 0
            """.trimIndent(),
        )
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS photos (
                id TEXT NOT NULL PRIMARY KEY,
                fileName TEXT NOT NULL,
                filePath TEXT NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                city TEXT NOT NULL,
                state TEXT NOT NULL,
                zipCode TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                loadId TEXT
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_photos_timestamp ON photos (timestamp)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_photos_loadId ON photos (loadId)")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS scans (
                id TEXT NOT NULL PRIMARY KEY,
                fileName TEXT NOT NULL,
                filePath TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                fileSizeBytes INTEGER NOT NULL,
                pageCount INTEGER NOT NULL,
                ocrText TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_scans_timestamp ON scans (timestamp)")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS load_history (
                id TEXT NOT NULL PRIMARY KEY,
                loadId TEXT NOT NULL,
                field TEXT NOT NULL,
                oldValue TEXT NOT NULL,
                newValue TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_load_history_loadId ON load_history (loadId)")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS driver_profile (
                id TEXT NOT NULL PRIMARY KEY,
                displayName TEXT NOT NULL DEFAULT '',
                avatarUrl TEXT,
                truckType TEXT NOT NULL DEFAULT '',
                experienceYears INTEGER NOT NULL DEFAULT 0,
                homeState TEXT NOT NULL DEFAULT '',
                routesJson TEXT NOT NULL DEFAULT '',
                about TEXT NOT NULL DEFAULT '',
                status TEXT NOT NULL DEFAULT 'OFFLINE',
                joinedDate INTEGER NOT NULL,
                lastActive INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS social_chats (
                id TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                type TEXT NOT NULL,
                participantCount INTEGER NOT NULL,
                lastMessage TEXT NOT NULL,
                lastMessageAt INTEGER NOT NULL,
                unreadCount INTEGER NOT NULL,
                avatarEmoji TEXT NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS social_messages (
                id TEXT NOT NULL PRIMARY KEY,
                chatId TEXT NOT NULL,
                senderId TEXT NOT NULL,
                senderName TEXT NOT NULL,
                text TEXT NOT NULL,
                sentAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_social_messages_chatId ON social_messages (chatId)")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE social_chats ADD COLUMN onlineCount INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE social_chats ADD COLUMN description TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE social_chats ADD COLUMN rating REAL NOT NULL DEFAULT 4.5")
        db.execSQL("ALTER TABLE social_chats ADD COLUMN isPublic INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE social_messages ADD COLUMN attachmentUrl TEXT")
        db.execSQL("ALTER TABLE social_messages ADD COLUMN replyToId TEXT")
        db.execSQL("ALTER TABLE social_messages ADD COLUMN locationLabel TEXT")
        db.execSQL("ALTER TABLE social_messages ADD COLUMN isAnnouncement INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_social_chats_lastMessageAt ON social_chats (lastMessageAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_social_chats_title ON social_chats (title)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_social_messages_chatId_sentAt " +
                "ON social_messages (chatId, sentAt)",
        )
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE social_messages ADD COLUMN messageType TEXT NOT NULL DEFAULT 'TEXT'")
        db.execSQL("ALTER TABLE social_chats ADD COLUMN category TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE social_chats ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS blocked_users (
                blockerId TEXT NOT NULL,
                blockedId TEXT NOT NULL,
                blockedAt INTEGER NOT NULL,
                PRIMARY KEY(blockerId, blockedId)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS driver_statuses (
                id TEXT NOT NULL PRIMARY KEY,
                userId TEXT NOT NULL,
                displayName TEXT NOT NULL,
                type TEXT NOT NULL,
                text TEXT,
                mediaPath TEXT,
                createdAt INTEGER NOT NULL,
                expiresAt INTEGER NOT NULL,
                viewed INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_driver_statuses_expiresAt ON driver_statuses (expiresAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_driver_statuses_userId ON driver_statuses (userId)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS challenge_participation (
                challengeId TEXT NOT NULL,
                userId TEXT NOT NULL,
                score REAL NOT NULL,
                joinedAt INTEGER NOT NULL,
                PRIMARY KEY(challengeId, userId)
            )
            """.trimIndent(),
        )
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE driver_profile ADD COLUMN coverImageUrl TEXT")
        db.execSQL("ALTER TABLE driver_profile ADD COLUMN licenseClass TEXT NOT NULL DEFAULT 'A'")
        db.execSQL("ALTER TABLE driver_profile ADD COLUMN endorsementsJson TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE driver_profile ADD COLUMN maxRadius INTEGER NOT NULL DEFAULT 500")
        db.execSQL("ALTER TABLE driver_profile ADD COLUMN specialtiesJson TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE driver_profile ADD COLUMN languagesJson TEXT NOT NULL DEFAULT 'Русский,Английский'")
        db.execSQL("ALTER TABLE driver_profile ADD COLUMN phoneNumber TEXT")
        db.execSQL("ALTER TABLE driver_profile ADD COLUMN telegramUsername TEXT")
        db.execSQL("ALTER TABLE driver_profile ADD COLUMN whatsappNumber TEXT")
        db.execSQL("ALTER TABLE driver_profile ADD COLUMN reputation INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE driver_profile ADD COLUMN followers INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE driver_profile ADD COLUMN following INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE driver_profile ADD COLUMN ratingCount INTEGER NOT NULL DEFAULT 124")
        db.execSQL("ALTER TABLE driver_profile ADD COLUMN currentRoute TEXT")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS message_reactions (
                messageId TEXT NOT NULL,
                userId TEXT NOT NULL,
                reaction TEXT NOT NULL,
                reactedAt INTEGER NOT NULL,
                PRIMARY KEY(messageId, userId, reaction)
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_message_reactions_messageId ON message_reactions (messageId)",
        )
    }
}

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
        db.execSQL("ALTER TABLE social_chats ADD COLUMN creatorId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE social_chats ADD COLUMN inviteCode TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE social_messages ADD COLUMN durationMs INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE driver_statuses ADD COLUMN durationMs INTEGER NOT NULL DEFAULT 0")
    }
}
