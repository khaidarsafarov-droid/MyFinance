package com.truckerload.data.local

import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationHelpersTest {

    @Test
    fun hasTable_hasColumn_addColumnIfMissing() {
        val context = RuntimeEnvironment.getApplication()
        val name = "helpers-test"
        context.deleteDatabase(name)
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE t (id INTEGER PRIMARY KEY NOT NULL)")
                }

                override fun onUpgrade(
                    db: androidx.sqlite.db.SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int,
                ) = Unit
            })
            .build()

        FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase.use { db ->
            assertTrue(db.hasTable("t"))
            assertFalse(db.hasTable("missing"))
            assertTrue(db.hasColumn("t", "id"))
            assertFalse(db.hasColumn("t", "name"))
            db.addColumnIfMissing("t", "name", "TEXT NOT NULL DEFAULT ''")
            assertTrue(db.hasColumn("t", "name"))
            db.addColumnIfMissing("t", "name", "TEXT NOT NULL DEFAULT ''") // idempotent
            assertTrue(db.hasColumn("t", "name"))
        }
    }
}
