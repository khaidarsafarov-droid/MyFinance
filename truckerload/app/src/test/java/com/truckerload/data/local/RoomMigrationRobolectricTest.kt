package com.truckerload.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.db.SupportSQLiteOpenHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * JVM/CI-friendly mirror of MigrationTestHelper smoke coverage.
 * Runs the full 6→31 path and asserts fixture survival + key columns.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomMigrationRobolectricTest {

    @Test
    fun migrate6To31_smoke() {
        val context = RuntimeEnvironment.getApplication()
        val dbName = "robo-migration-6-30"
        context.deleteDatabase(dbName)

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(6) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS loads (
                            id TEXT NOT NULL PRIMARY KEY,
                            tripId TEXT NOT NULL,
                            date TEXT NOT NULL,
                            totalRate REAL NOT NULL,
                            totalMiles REAL NOT NULL,
                            pointA TEXT NOT NULL,
                            pointB TEXT NOT NULL,
                            puCount INTEGER NOT NULL,
                            delCount INTEGER NOT NULL,
                            weekNumber INTEGER NOT NULL,
                            year INTEGER NOT NULL,
                            rawMessage TEXT NOT NULL,
                            parsedAt INTEGER NOT NULL,
                            updatedAt INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        INSERT INTO loads (
                          id, tripId, date, totalRate, totalMiles, pointA, pointB,
                          puCount, delCount, weekNumber, year, rawMessage, parsedAt, updatedAt
                        ) VALUES (
                          'id-1', 'T-SMOKE', '2026-01-15', 1000.0, 400.0, 'A', 'B',
                          1, 1, 3, 2026, 'raw', 1, 1
                        )
                        """.trimIndent(),
                    )
                    // Pre-v6 tables never created by forward migrations — stub for INDEX DDL.
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS diesel (
                            id TEXT NOT NULL PRIMARY KEY,
                            date TEXT NOT NULL,
                            gallons REAL NOT NULL,
                            pricePerGallon REAL NOT NULL,
                            total REAL NOT NULL,
                            location TEXT NOT NULL,
                            weekNumber INTEGER NOT NULL,
                            year INTEGER NOT NULL,
                            addedAt INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS paychecks (
                            id TEXT NOT NULL PRIMARY KEY,
                            date TEXT NOT NULL,
                            amount REAL NOT NULL,
                            note TEXT NOT NULL,
                            weekNumber INTEGER NOT NULL,
                            year INTEGER NOT NULL,
                            addedAt INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase.use { db ->
            assertEquals(6, db.version)
            var version = 6
            for (migration in ALL_MIGRATIONS_FROM_V6.sortedBy { it.startVersion }) {
                if (migration.startVersion == version) {
                    migration.migrate(db)
                    version = migration.endVersion
                    db.version = version
                }
            }
            assertEquals(31, version)
            db.query("SELECT COUNT(*) FROM loads").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(1, c.getInt(0))
            }
            assertTrue(db.hasColumn("loads", "firstPuMillis"))
            assertTrue(db.hasColumn("loads", "actualFinishDate"))
            assertTrue(db.hasTable("crowd_rates"))
            assertTrue(db.hasTable("media_sync_queue"))
            assertTrue(db.hasTable("maintenance_archive"))
            assertTrue(db.hasColumn("maintenance_archive", "serviceName"))
            assertTrue(db.hasColumn("voice_rooms", "description"))
            assertTrue(db.hasColumn("voice_rooms", "moderatorId"))
            assertTrue(db.hasTable("user_accounts"))
            assertTrue(db.hasTable("driver_professional_profiles"))
            assertTrue(db.hasTable("community_profiles"))
        }
    }

    @Test
    fun migrate25To26_isIdempotent() {
        val context = RuntimeEnvironment.getApplication()
        val dbName = "robo-migration-25-26"
        context.deleteDatabase(dbName)
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(25) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE photos (
                            id TEXT NOT NULL PRIMARY KEY,
                            fileName TEXT NOT NULL,
                            filePath TEXT NOT NULL,
                            createdAt INTEGER NOT NULL,
                            loadId TEXT,
                            tripId TEXT,
                            loadDate TEXT
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        CREATE TABLE scans (
                            id TEXT NOT NULL PRIMARY KEY,
                            fileName TEXT NOT NULL,
                            filePath TEXT NOT NULL,
                            createdAt INTEGER NOT NULL,
                            loadId TEXT,
                            tripId TEXT,
                            loadDate TEXT
                        )
                        """.trimIndent(),
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase.use { db ->
            MIGRATION_25_26.migrate(db)
            MIGRATION_25_26.migrate(db)
            assertTrue(db.hasColumn("photos", "cloudMediaId"))
            assertTrue(db.hasColumn("scans", "cloudSyncStatus"))
            assertTrue(db.hasTable("media_sync_queue"))
            MEDIA_MIGRATION_25_26_COLUMNS.forEach { qualified ->
                val (table, column) = qualified.split('.')
                assertTrue("$qualified missing", db.hasColumn(table, column))
            }
        }
    }

    @Test
    fun blockedLegacyMigrationThrows() {
        val context = RuntimeEnvironment.getApplication()
        val dbName = "robo-blocked-v3"
        context.deleteDatabase(dbName)
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: SupportSQLiteDatabase) = Unit
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase.use { db ->
            try {
                MIGRATION_3_6_BLOCKED.migrate(db)
                org.junit.Assert.fail("expected UnsupportedDatabaseUpgradeException")
            } catch (e: UnsupportedDatabaseUpgradeException) {
                assertEquals(3, e.fromVersion)
                assertTrue(e.message!!.contains("бэкап") || e.message!!.contains("backup"))
            }
        }
    }
}
