package com.truckerload.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Room migrations for schema versions 25→29 (startVersion 25..28). */

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
