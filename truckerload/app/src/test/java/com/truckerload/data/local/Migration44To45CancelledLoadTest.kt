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
class Migration44To45CancelledLoadTest {

    @Test
    fun addsCancellationColumns_andKeepsEveryLoadUncancelled() {
        openV44("migration-44-45-keep-rows").writableDatabase.use { db ->
            MIGRATION_44_45.migrate(db)
            db.query(
                "SELECT id, totalRate, totalMiles, deadheadMiles, isCancelled, cancelledAt, " +
                    "originalRate, originalMiles FROM loads",
            ).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("load-1", c.getString(0))
                assertEquals(441893L, c.getLong(1))
                assertEquals(900.0, c.getDouble(2), 0.001)
                assertEquals(55.0, c.getDouble(3), 0.001)
                assertEquals(0, c.getInt(4))
                assertTrue(c.isNull(5))
                assertTrue(c.isNull(6))
                assertTrue(c.isNull(7))
                assertFalse(c.moveToNext())
            }
        }
    }

    @Test
    fun runningTwice_isHarmless() {
        openV44("migration-44-45-twice").writableDatabase.use { db ->
            MIGRATION_44_45.migrate(db)
            MIGRATION_44_45.migrate(db)
            db.query("SELECT COUNT(*) FROM loads WHERE isCancelled = 0").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(1, c.getInt(0))
            }
        }
    }

    private fun openV44(name: String): SupportSQLiteOpenHelper {
        val context = RuntimeEnvironment.getApplication()
        context.deleteDatabase(name)
        return FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(44) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(LOADS_V44)
                        db.execSQL(INSERT_LOAD)
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build(),
        )
    }

    private companion object {
        /** Exported `loads` table of schema 44. */
        const val LOADS_V44 =
            "CREATE TABLE IF NOT EXISTS `loads` (`id` TEXT NOT NULL, `tripId` TEXT NOT NULL" +
                ", `date` TEXT NOT NULL, `totalRate` INTEGER NOT NULL, `totalMiles` REAL NOT NULL" +
                ", `pointA` TEXT NOT NULL, `pointB` TEXT NOT NULL, `puCount` INTEGER NOT NULL" +
                ", `delCount` INTEGER NOT NULL, `weekNumber` INTEGER NOT NULL, `year` INTEGER NOT NULL" +
                ", `rawMessage` TEXT NOT NULL, `parsedAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL" +
                ", `firstPuMillis` INTEGER, `lastDelMillis` INTEGER, `route` TEXT NOT NULL" +
                ", `firstPuCityState` TEXT NOT NULL, `lastDelCityState` TEXT NOT NULL" +
                ", `durationDays` REAL NOT NULL, `pace` INTEGER NOT NULL, `stopCount` INTEGER NOT NULL" +
                ", `isDispute` INTEGER NOT NULL, `disputeResponseDate` TEXT" +
                ", `disputeCompleted` INTEGER NOT NULL, `disputeAmount` INTEGER" +
                ", `disputeApplyToLoad` INTEGER NOT NULL, `disputeAmountApplied` INTEGER NOT NULL" +
                ", `actualFinishDate` TEXT, `equipmentType` TEXT" +
                ", `deadheadMiles` REAL NOT NULL, PRIMARY KEY(`id`))"

        val INSERT_LOAD = """
            INSERT INTO loads (
                id, tripId, date, totalRate, totalMiles, pointA, pointB, puCount, delCount,
                weekNumber, year, rawMessage, parsedAt, updatedAt, route, firstPuCityState,
                lastDelCityState, durationDays, pace, stopCount, isDispute, disputeCompleted,
                disputeApplyToLoad, disputeAmountApplied, deadheadMiles
            ) VALUES (
                'load-1', 'T-1', '2026-01-01', 441893, 900.0, 'A', 'B', 1, 1,
                1, 2026, '', 1, 2, '', '', '', 1.0, 0, 2, 0, 0, 0, 0, 55.0
            )
        """.trimIndent()
    }
}
