package com.truckerload.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Room migrations for schema versions 6→15 (startVersion 6..15). */

/** Добавляет PU/DEL millis без удаления существующих грузов. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.addColumnIfMissing("loads", "firstPuMillis", "INTEGER")
        db.addColumnIfMissing("loads", "lastDelMillis", "INTEGER")
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
        db.addColumnIfMissing("loads", "route", "TEXT NOT NULL DEFAULT ''")
        db.addColumnIfMissing("loads", "firstPuCityState", "TEXT NOT NULL DEFAULT ''")
        db.addColumnIfMissing("loads", "lastDelCityState", "TEXT NOT NULL DEFAULT ''")
        db.addColumnIfMissing("loads", "durationDays", "REAL NOT NULL DEFAULT 0")
        db.addColumnIfMissing("loads", "pace", "REAL NOT NULL DEFAULT 0")
        db.addColumnIfMissing("loads", "stopCount", "INTEGER NOT NULL DEFAULT 0")
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
        db.addColumnIfMissing("social_chats", "onlineCount", "INTEGER NOT NULL DEFAULT 0")
        db.addColumnIfMissing("social_chats", "description", "TEXT NOT NULL DEFAULT ''")
        db.addColumnIfMissing("social_chats", "rating", "REAL NOT NULL DEFAULT 4.5")
        db.addColumnIfMissing("social_chats", "isPublic", "INTEGER NOT NULL DEFAULT 1")
        db.addColumnIfMissing("social_messages", "attachmentUrl", "TEXT")
        db.addColumnIfMissing("social_messages", "replyToId", "TEXT")
        db.addColumnIfMissing("social_messages", "locationLabel", "TEXT")
        db.addColumnIfMissing("social_messages", "isAnnouncement", "INTEGER NOT NULL DEFAULT 0")
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
        db.addColumnIfMissing("social_messages", "messageType", "TEXT NOT NULL DEFAULT 'TEXT'")
        db.addColumnIfMissing("social_chats", "category", "TEXT NOT NULL DEFAULT ''")
        db.addColumnIfMissing("social_chats", "archived", "INTEGER NOT NULL DEFAULT 0")
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
