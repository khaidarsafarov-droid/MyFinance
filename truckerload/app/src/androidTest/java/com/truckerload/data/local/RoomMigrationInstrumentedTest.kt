package com.truckerload.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * MigrationTestHelper coverage for Room forward path.
 * Schemas 28/29 are exported under app/schemas and mounted as androidTest assets.
 * Older start versions use a fixture DB (no historical schema JSON) then
 * [MigrationTestHelper.runMigrationsAndValidate] with validate=false.
 */
@RunWith(AndroidJUnit4::class)
class RoomMigrationInstrumentedTest {

    private val testDb = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    @Throws(IOException::class)
    fun migrate6To34_smoke() {
        createFixtureDatabase(6) {
            execSQL(V6_LOADS_DDL)
            execSQL(
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
            execSQL(DIESEL_DDL)
            execSQL(PAYCHECKS_DDL)
        }

        // validate=false: pre-v6 core tables (stops/penalties/…) were never created by
        // forward migrations; we assert row survival + key columns via PRAGMA instead.
        val db = helper.runMigrationsAndValidate(testDb, 34, false, *ALL_MIGRATIONS_FROM_V6)
        db.query("SELECT COUNT(*) FROM loads").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1, c.getInt(0))
        }
        assertTrue(db.hasColumn("loads", "firstPuMillis"))
        assertTrue(db.hasColumn("loads", "actualFinishDate"))
        assertTrue(db.hasColumn("loads", "equipmentType"))
        assertTrue(db.hasTable("crowd_rates"))
        assertTrue(db.hasColumn("crowd_rates", "equipmentType"))
        assertTrue(db.hasColumn("diesel", "discountPricePerGallon"))
        assertTrue(db.hasTable("media_sync_queue"))
        assertTrue(db.hasTable("user_accounts"))
        assertTrue(db.hasTable("driver_professional_profiles"))
        assertTrue(!db.hasTable("community_profiles"))
        assertTrue(!db.hasTable("voice_rooms"))
        assertTrue(!db.hasTable("social_chats"))
        db.query("SELECT tripId FROM loads WHERE id = 'id-1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("T-SMOKE", c.getString(0))
        }
        assertTableInfoHas(db, "loads", "firstPuMillis")
    }

    @Test
    @Throws(IOException::class)
    fun migrate22To23() {
        createFixtureDatabase(22) {
            execSQL(DIESEL_DDL)
            execSQL(PAYCHECKS_DDL)
            execSQL(
                "INSERT INTO diesel (id, date, gallons, pricePerGallon, total, location, weekNumber, year, addedAt) " +
                    "VALUES ('d1', '2026-01-01', 10.0, 3.0, 30.0, 'X', 1, 2026, 1)",
            )
        }
        val db = helper.runMigrationsAndValidate(testDb, 23, false, MIGRATION_22_23)
        db.query("SELECT COUNT(*) FROM diesel").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1, c.getInt(0))
        }
        assertTrue(indexExists(db, "index_diesel_weekNumber_year"))
        assertTrue(indexExists(db, "index_paychecks_weekNumber_year"))
    }

    @Test
    @Throws(IOException::class)
    fun migrate25To26() {
        createFixtureDatabase(25) {
            execSQL(PHOTOS_V25_DDL)
            execSQL(SCANS_V25_DDL)
            execSQL(
                "INSERT INTO photos (id, fileName, filePath, createdAt, loadId, tripId, loadDate) " +
                    "VALUES ('p1', 'a.jpg', '/a', 1, NULL, NULL, NULL)",
            )
        }
        val db = helper.runMigrationsAndValidate(testDb, 26, false, MIGRATION_25_26)
        assertTrue(db.hasColumn("photos", "cloudMediaId"))
        assertTrue(db.hasColumn("photos", "cloudSyncStatus"))
        assertTrue(db.hasColumn("scans", "cloudSyncStatus"))
        assertTrue(db.hasTable("media_sync_queue"))
        db.query("SELECT COUNT(*) FROM photos").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1, c.getInt(0))
        }
        // Idempotent re-run of column adds must not throw.
        MIGRATION_25_26.migrate(db)
    }

    @Test
    @Throws(IOException::class)
    fun migrate27To28() {
        createFixtureDatabase(27) {
            execSQL(MAINTENANCE_ARCHIVE_V27_DDL)
            execSQL(
                "INSERT INTO maintenance_archive (id, serviceDate, description, amount, createdAt) " +
                    "VALUES (1, '2026-01-01', 'oil', 50.0, 1)",
            )
        }
        val db = helper.runMigrationsAndValidate(testDb, 28, false, MIGRATION_27_28)
        assertTrue(db.hasColumn("maintenance_archive", "serviceName"))
        db.query("SELECT serviceName FROM maintenance_archive WHERE id = 1").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("", c.getString(0))
        }
        MIGRATION_27_28.migrate(db) // idempotent
    }

    @Test
    @Throws(IOException::class)
    fun migrate28To29() {
        // Full exported schema for v28 — create via MigrationTestHelper.
        helper.createDatabase(testDb, 28).apply { close() }
        val db = helper.runMigrationsAndValidate(testDb, 29, true, MIGRATION_28_29)
        assertTrue(db.hasTable("crowd_rates"))
        assertTrue(indexExists(db, "index_crowd_rates_fromState_reportedAtMillis"))
    }

    @Test
    @Throws(IOException::class)
    fun migrate29To30() {
        helper.createDatabase(testDb, 29).apply { close() }
        val db = helper.runMigrationsAndValidate(testDb, 30, true, MIGRATION_29_30)
        assertTrue(db.hasColumn("voice_rooms", "description"))
        assertTrue(db.hasColumn("voice_rooms", "moderatorId"))
    }

    @Test
    @Throws(IOException::class)
    fun migrate30To31() {
        helper.createDatabase(testDb, 30).apply { close() }
        val db = helper.runMigrationsAndValidate(testDb, 31, true, MIGRATION_30_31)
        assertTrue(db.hasTable("user_accounts"))
        assertTrue(db.hasTable("driver_professional_profiles"))
        assertTrue(db.hasTable("community_profiles"))
    }

    @Test
    @Throws(IOException::class)
    fun migrate31To32() {
        helper.createDatabase(testDb, 31).apply { close() }
        val db = helper.runMigrationsAndValidate(testDb, 32, true, MIGRATION_31_32)
        assertTrue(db.hasColumn("loads", "equipmentType"))
        assertTrue(db.hasColumn("crowd_rates", "equipmentType"))
    }

    @Test
    @Throws(IOException::class)
    fun migrate32To33() {
        helper.createDatabase(testDb, 32).apply { close() }
        val db = helper.runMigrationsAndValidate(testDb, 33, true, MIGRATION_32_33)
        assertTrue(!db.hasTable("community_profiles"))
        assertTrue(!db.hasTable("voice_rooms"))
        assertTrue(!db.hasTable("social_chats"))
        assertTrue(db.hasTable("driver_profile"))
        assertTrue(db.hasTable("crowd_rates"))
    }

    @Test
    @Throws(IOException::class)
    fun migrate33To34() {
        helper.createDatabase(testDb, 33).apply { close() }
        val db = helper.runMigrationsAndValidate(testDb, 34, true, MIGRATION_33_34)
        assertTrue(db.hasColumn("diesel", "discountPricePerGallon"))
    }

    /**
     * Builds a named DB at [version] without requiring an exported schema JSON
     * (historical schemas are not committed; only 28–34 are).
     */
    private fun createFixtureDatabase(version: Int, setup: SupportSQLiteDatabase.() -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(testDb)
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(testDb)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(version) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.setup()
                    }

                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int,
                    ) = Unit
                },
            )
            .build()
        FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase.close()
    }

    private fun indexExists(db: SupportSQLiteDatabase, name: String): Boolean {
        db.query(
            "SELECT 1 FROM sqlite_master WHERE type = 'index' AND name = ? LIMIT 1",
            arrayOf(name),
        ).use { return it.moveToFirst() }
    }

    private fun assertTableInfoHas(db: SupportSQLiteDatabase, table: String, column: String) {
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            var found = false
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == column) {
                    found = true
                    break
                }
            }
            assertTrue("PRAGMA table_info($table) missing $column", found)
        }
    }

    companion object {
        private val V6_LOADS_DDL = """
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
        """.trimIndent()

        private val DIESEL_DDL = """
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
        """.trimIndent()

        private val PAYCHECKS_DDL = """
            CREATE TABLE IF NOT EXISTS paychecks (
                id TEXT NOT NULL PRIMARY KEY,
                date TEXT NOT NULL,
                amount REAL NOT NULL,
                note TEXT NOT NULL,
                weekNumber INTEGER NOT NULL,
                year INTEGER NOT NULL,
                addedAt INTEGER NOT NULL
            )
        """.trimIndent()

        private val PHOTOS_V25_DDL = """
            CREATE TABLE IF NOT EXISTS photos (
                id TEXT NOT NULL PRIMARY KEY,
                fileName TEXT NOT NULL,
                filePath TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                loadId TEXT,
                tripId TEXT,
                loadDate TEXT
            )
        """.trimIndent()

        private val SCANS_V25_DDL = """
            CREATE TABLE IF NOT EXISTS scans (
                id TEXT NOT NULL PRIMARY KEY,
                fileName TEXT NOT NULL,
                filePath TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                loadId TEXT,
                tripId TEXT,
                loadDate TEXT
            )
        """.trimIndent()

        private val MAINTENANCE_ARCHIVE_V27_DDL = """
            CREATE TABLE IF NOT EXISTS maintenance_archive (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                serviceDate TEXT NOT NULL,
                description TEXT NOT NULL,
                amount REAL NOT NULL,
                photoPath TEXT,
                ocrText TEXT,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent()
    }
}
