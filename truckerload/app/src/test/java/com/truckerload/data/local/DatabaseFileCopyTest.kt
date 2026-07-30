package com.truckerload.data.local

import android.database.sqlite.SQLiteDatabase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.io.path.createTempDirectory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseFileCopyTest {

    @Test
    fun `copy succeeds and passes integrity check`() {
        val dir = createTempDirectory("db-copy-ok").toFile()
        val source = File(dir, "source.db")
        val target = File(dir, "target.db")
        createSqlite(source, "CREATE TABLE t(id INTEGER PRIMARY KEY); INSERT INTO t VALUES (1);")

        val result = DatabaseFileCopy.copyWithSidecars(source, target, maxAttempts = 2, retryDelayMs = 0)
        assertTrue(result.isSuccess)
        assertTrue(target.exists())
        assertTrue(target.length() > 0)
        assertTrue(DatabaseFileCopy.isHealthyDatabase(target))
    }

    @Test
    fun `failed copy deletes broken target and does not leave empty file`() {
        val dir = createTempDirectory("db-copy-fail").toFile()
        val source = File(dir, "missing.db") // does not exist
        val target = File(dir, "target.db")
        target.writeText("partial")

        val result = DatabaseFileCopy.copyWithSidecars(source, target, maxAttempts = 2, retryDelayMs = 0)
        assertTrue(result.isFailure)
        assertFalse(target.exists())
    }

    @Test
    fun `corrupt sqlite fails integrity and is deleted`() {
        val dir = createTempDirectory("db-copy-corrupt").toFile()
        val source = File(dir, "corrupt.db")
        val target = File(dir, "target.db")
        source.writeBytes(ByteArray(64) { 0x41 })

        val result = DatabaseFileCopy.copyWithSidecars(source, target, maxAttempts = 1, retryDelayMs = 0)
        assertTrue(result.isFailure)
        assertFalse(target.exists())
    }

    @Test
    fun `deleteDbTree removes sidecars`() {
        val dir = createTempDirectory("db-tree").toFile()
        val db = File(dir, "x.db")
        db.writeText("x")
        File(db.path + "-wal").writeText("w")
        File(db.path + "-shm").writeText("s")
        DatabaseFileCopy.deleteDbTree(db)
        assertFalse(db.exists())
        assertFalse(File(db.path + "-wal").exists())
        assertFalse(File(db.path + "-shm").exists())
    }

    private fun createSqlite(file: File, sql: String) {
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            sql.split(';').map { it.trim() }.filter { it.isNotEmpty() }.forEach { db.execSQL(it) }
        }
    }
}
