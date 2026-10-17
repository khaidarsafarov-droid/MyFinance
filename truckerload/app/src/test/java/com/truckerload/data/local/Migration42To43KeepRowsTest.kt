package com.truckerload.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration42To43KeepRowsTest {

    @Test
    fun missingMarkerColumn_keepsExistingLoadAndConvertsDollars() {
        val context = RuntimeEnvironment.getApplication()
        val name = "migration-42-43-keep-rows"
        context.deleteDatabase(name)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(42) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(OLD_LOADS)
                        db.execSQL(INSERT_LOAD)
                    }

                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int,
                    ) = Unit
                })
                .build(),
        )

        helper.writableDatabase.use { db ->
            MIGRATION_42_43.migrate(db)
            db.query("SELECT id, totalRate, deadheadMiles FROM loads").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("load-1", cursor.getString(0))
                assertEquals(250000, cursor.getInt(1))
                assertEquals(0.0, cursor.getDouble(2), 0.001)
                assertFalse(cursor.moveToNext())
            }
        }
    }

    private companion object {
        val OLD_LOADS = """
            CREATE TABLE loads (
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
                updatedAt INTEGER NOT NULL,
                firstPuMillis INTEGER,
                lastDelMillis INTEGER,
                route TEXT NOT NULL,
                firstPuCityState TEXT NOT NULL,
                lastDelCityState TEXT NOT NULL,
                durationDays REAL NOT NULL,
                pace REAL NOT NULL,
                stopCount INTEGER NOT NULL,
                isDispute INTEGER NOT NULL,
                disputeResponseDate TEXT,
                disputeCompleted INTEGER NOT NULL,
                disputeAmount REAL,
                disputeApplyToLoad INTEGER NOT NULL,
                disputeAmountApplied INTEGER NOT NULL,
                actualFinishDate TEXT,
                equipmentType TEXT
            )
        """.trimIndent()

        val INSERT_LOAD = """
            INSERT INTO loads (
                id, tripId, date, totalRate, totalMiles, pointA, pointB, puCount, delCount,
                weekNumber, year, rawMessage, parsedAt, updatedAt, firstPuMillis, lastDelMillis,
                route, firstPuCityState, lastDelCityState, durationDays, pace, stopCount,
                isDispute, disputeResponseDate, disputeCompleted, disputeAmount,
                disputeApplyToLoad, disputeAmountApplied, actualFinishDate, equipmentType
            ) VALUES (
                'load-1', 'T-1', '2026-01-01', 2500.0, 100.0, 'A', 'B', 1, 1,
                1, 2026, '', 1, 1, NULL, NULL,
                '', '', '', 1.0, 2.5, 2,
                0, NULL, 0, NULL,
                0, 0, NULL, NULL
            )
        """.trimIndent()
    }
}
