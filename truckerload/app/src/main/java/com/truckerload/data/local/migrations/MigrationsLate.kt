package com.truckerload.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Room migrations for schema versions 25→36 (startVersion 25..35). */

/** Durable attachment queue and per-row cloud state (idempotent column adds). */
val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.addColumnIfMissing("photos", "cloudMediaId", "TEXT")
        db.addColumnIfMissing("photos", "cloudSyncStatus", "TEXT NOT NULL DEFAULT 'LOCAL'")
        db.addColumnIfMissing("photos", "cloudUpdatedAt", "INTEGER NOT NULL DEFAULT 0")
        db.execLogged("CREATE INDEX IF NOT EXISTS index_photos_cloudSyncStatus ON photos(cloudSyncStatus)")
        db.addColumnIfMissing("scans", "cloudMediaId", "TEXT")
        db.addColumnIfMissing("scans", "cloudSyncStatus", "TEXT NOT NULL DEFAULT 'LOCAL'")
        db.addColumnIfMissing("scans", "cloudUpdatedAt", "INTEGER NOT NULL DEFAULT 0")
        db.execLogged("CREATE INDEX IF NOT EXISTS index_scans_cloudSyncStatus ON scans(cloudSyncStatus)")
        db.execLogged(
            """
            CREATE TABLE IF NOT EXISTS media_sync_queue (
                id TEXT NOT NULL PRIMARY KEY,
                localId TEXT NOT NULL,
                kind TEXT NOT NULL,
                operation TEXT NOT NULL,
                remoteMediaId TEXT,
                filePath TEXT,
                metadataJson TEXT NOT NULL,
                attempts INTEGER NOT NULL,
                lastError TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                status TEXT NOT NULL
            )
            """.trimIndent(),
        )
        db.execLogged(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_media_sync_queue_kind_localId " +
                "ON media_sync_queue(kind, localId)",
        )
        db.execLogged(
            "CREATE INDEX IF NOT EXISTS index_media_sync_queue_status_createdAt " +
                "ON media_sync_queue(status, createdAt)",
        )
        db.execLogged(
            "CREATE INDEX IF NOT EXISTS index_media_sync_queue_status_updatedAt " +
                "ON media_sync_queue(status, updatedAt)",
        )
    }
}

/**
 * Contract surface for unit tests: documents the column names introduced in 25→26.
 * Prefer running [MIGRATION_25_26] over string-matching raw ALTER SQL.
 */
val MEDIA_MIGRATION_25_26_COLUMNS = listOf(
    "photos.cloudMediaId",
    "photos.cloudSyncStatus",
    "photos.cloudUpdatedAt",
    "scans.cloudMediaId",
    "scans.cloudSyncStatus",
    "scans.cloudUpdatedAt",
)

/** ТО (maintenance) reminders + service receipt archive. */
val MIGRATION_26_27 = object : Migration(26, 27) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS maintenance_tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                startDate TEXT NOT NULL,
                reminderType TEXT NOT NULL,
                intervalMiles REAL,
                odometerAtStart REAL,
                dueDate TEXT,
                isCompleted INTEGER NOT NULL,
                completedAt INTEGER,
                notifiedAt INTEGER,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_maintenance_tasks_isCompleted " +
                "ON maintenance_tasks(isCompleted)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_maintenance_tasks_startDate " +
                "ON maintenance_tasks(startDate)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_maintenance_tasks_dueDate " +
                "ON maintenance_tasks(dueDate)",
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS maintenance_archive (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                serviceDate TEXT NOT NULL,
                description TEXT NOT NULL,
                amount REAL NOT NULL,
                photoPath TEXT,
                ocrText TEXT,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_maintenance_archive_serviceDate " +
                "ON maintenance_archive(serviceDate)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_maintenance_archive_createdAt " +
                "ON maintenance_archive(createdAt)",
        )
    }
}

/** Add serviceName to maintenance receipt archive (OCR company / shop name). */
val MIGRATION_27_28 = object : Migration(27, 28) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.addColumnIfMissing(
            "maintenance_archive",
            "serviceName",
            "TEXT NOT NULL DEFAULT ''",
        )
    }
}

/** Add anonymized crowd rate cache for map Me/Friends/All scopes. */
val MIGRATION_28_29 = object : Migration(28, 29) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execLogged(
            """
            CREATE TABLE IF NOT EXISTS crowd_rates (
                id TEXT NOT NULL PRIMARY KEY,
                fromState TEXT NOT NULL,
                toState TEXT NOT NULL,
                rpm REAL NOT NULL,
                rate REAL NOT NULL,
                miles REAL NOT NULL,
                reportedAtMillis INTEGER NOT NULL,
                source TEXT NOT NULL,
                peerLabel TEXT,
                syncedAtMillis INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execLogged(
            "CREATE INDEX IF NOT EXISTS index_crowd_rates_fromState_reportedAtMillis " +
                "ON crowd_rates(fromState, reportedAtMillis)",
        )
        db.execLogged(
            "CREATE INDEX IF NOT EXISTS index_crowd_rates_source_reportedAtMillis " +
                "ON crowd_rates(source, reportedAtMillis)",
        )
    }
}

/** Voice room description + moderator for creator management. */
val MIGRATION_29_30 = object : Migration(29, 30) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.addColumnIfMissing("voice_rooms", "description", "TEXT NOT NULL DEFAULT ''")
        db.addColumnIfMissing("voice_rooms", "moderatorId", "TEXT NOT NULL DEFAULT ''")
    }
}

/**
 * Split User / professional DriverProfile / CommunityProfile.
 * CDL plaintext is copied as `plain:` ciphertext for app-level re-encrypt, then wiped.
 */
val MIGRATION_30_31 = object : Migration(30, 31) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execLogged(
            """
            CREATE TABLE IF NOT EXISTS `user_accounts` (
                `id` TEXT NOT NULL,
                `phone` TEXT,
                `email` TEXT,
                `authProvider` TEXT NOT NULL,
                `displayName` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `isVerified` INTEGER NOT NULL,
                `ageConfirmed` INTEGER NOT NULL,
                `acceptedTosAt` INTEGER,
                `analyticsConsentAt` INTEGER,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execLogged(
            """
            CREATE TABLE IF NOT EXISTS `driver_professional_profiles` (
                `userId` TEXT NOT NULL,
                `role` TEXT NOT NULL,
                `companyName` TEXT,
                `cdlNumberCiphertext` TEXT,
                `cdlDocumentUrlCiphertext` TEXT,
                `vehicleType` TEXT NOT NULL,
                `primaryRegion` TEXT NOT NULL,
                `dispatcherUserId` TEXT,
                `skipped` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`userId`)
            )
            """.trimIndent(),
        )
        db.execLogged(
            "CREATE INDEX IF NOT EXISTS `index_driver_professional_profiles_dispatcherUserId` " +
                "ON `driver_professional_profiles` (`dispatcherUserId`)",
        )
        db.execLogged(
            """
            CREATE TABLE IF NOT EXISTS `community_profiles` (
                `userId` TEXT NOT NULL,
                `nickname` TEXT NOT NULL,
                `avatarUrl` TEXT,
                `bio` TEXT,
                `visibilityJson` TEXT NOT NULL,
                `skipped` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`userId`)
            )
            """.trimIndent(),
        )
        if (db.hasTable("driver_profile")) {
            db.execLogged(
                """
                INSERT OR IGNORE INTO user_accounts (
                    id, phone, email, authProvider, displayName, createdAt,
                    isVerified, ageConfirmed, acceptedTosAt, analyticsConsentAt
                )
                SELECT
                    id, phoneNumber, NULL, 'EMAIL', displayName,
                    COALESCE(joinedDate, 0), 0, 0, NULL, NULL
                FROM driver_profile
                """.trimIndent(),
            )
            db.execLogged(
                """
                INSERT OR IGNORE INTO driver_professional_profiles (
                    userId, role, companyName, cdlNumberCiphertext, cdlDocumentUrlCiphertext,
                    vehicleType, primaryRegion, dispatcherUserId, skipped, updatedAt
                )
                SELECT
                    id,
                    'OWNER_OPERATOR',
                    NULL,
                    CASE WHEN cdlNumber IS NULL OR cdlNumber = '' THEN NULL
                         ELSE 'plain:' || cdlNumber END,
                    NULL,
                    COALESCE(truckType, ''),
                    COALESCE(homeState, ''),
                    NULL,
                    0,
                    COALESCE(lastActive, 0)
                FROM driver_profile
                """.trimIndent(),
            )
            db.execLogged(
                """
                INSERT OR IGNORE INTO community_profiles (
                    userId, nickname, avatarUrl, bio, visibilityJson, skipped, updatedAt
                )
                SELECT
                    id,
                    COALESCE(displayName, ''),
                    avatarUrl,
                    about,
                    '{"nickname":true,"bio":false,"avatar":true}',
                    0,
                    COALESCE(lastActive, 0)
                FROM driver_profile
                """.trimIndent(),
            )
            db.execLogged("UPDATE driver_profile SET cdlNumber = ''")
        }
    }
}

/** Optional equipment type on loads and anonymized crowd rates. */
val MIGRATION_31_32 = object : Migration(31, 32) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.addColumnIfMissing("loads", "equipmentType", "TEXT")
        db.addColumnIfMissing("crowd_rates", "equipmentType", "TEXT")
        db.execLogged(
            "CREATE INDEX IF NOT EXISTS " +
                "index_crowd_rates_fromState_equipmentType_reportedAtMillis " +
                "ON crowd_rates(fromState, equipmentType, reportedAtMillis)",
        )
    }
}

/** Drop Community / Friends / Voice-room tables. Own profile + crowd RPM stay. */
val MIGRATION_32_33 = object : Migration(32, 33) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val dropped = listOf(
            "social_chats",
            "social_messages",
            "message_reactions",
            "blocked_users",
            "driver_statuses",
            "driver_follows",
            "chat_members",
            "social_peers",
            "challenge_participation",
            "voice_rooms",
            "voice_room_participants",
            "call_sessions",
            "voice_signals",
            "community_profiles",
        )
        dropped.forEach { table ->
            db.execLogged("DROP TABLE IF EXISTS $table")
        }
    }
}

/** Fleet discount $/gal on diesel fills (for savings tracking). */
val MIGRATION_33_34 = object : Migration(33, 34) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.addColumnIfMissing("diesel", "discountPricePerGallon", "REAL")
    }
}

/** Document folders on scan gallery (load / paycheck / diesel / truck / other). */
val MIGRATION_34_35 = object : Migration(34, 35) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.addColumnIfMissing("scans", "category", "TEXT NOT NULL DEFAULT 'OTHER'")
        if (db.hasTable("scans")) {
            db.execLogged(
                "UPDATE scans SET category = 'LOAD' WHERE IFNULL(loadId, '') != ''",
            )
        }
    }
}

/** Dispute claimed amount and whether it should be added to the load rate. */
val MIGRATION_35_36 = object : Migration(35, 36) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.addColumnIfMissing("loads", "disputeAmount", "REAL")
        db.addColumnIfMissing("loads", "disputeApplyToLoad", "INTEGER NOT NULL DEFAULT 0")
        db.addColumnIfMissing("loads", "disputeAmountApplied", "INTEGER NOT NULL DEFAULT 0")
    }
}
