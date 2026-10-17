package com.truckerload.data.local

import android.database.sqlite.SQLiteDatabase
import net.zetetic.database.sqlcipher.SQLiteDatabase as CipherDatabase
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import javax.crypto.KeyGenerator

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DatabaseEncryptionTest {

    @Test
    fun passphraseIsCreatedOnceAndReused() {
        val dir = File(System.getProperty("java.io.tmpdir"), "tl-pass-${System.nanoTime()}")
        dir.mkdirs()
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val store = DatabasePassphraseStore(
            wrappedFile = File(dir, "db.wrap"),
            cipher = AesGcmPassphraseCipher(key),
        )
        val first = store.getOrCreate()
        val second = store.getOrCreate()
        assertEquals(DatabasePassphraseStore.PASSPHRASE_BYTES, first.size)
        assertArrayEquals(first, second)
        assertTrue(File(dir, "db.wrap").length() > first.size.toLong())
    }

    @Test
    fun corruptWrapIsNotReplaced() {
        val dir = File(System.getProperty("java.io.tmpdir"), "tl-pass-corrupt-${System.nanoTime()}")
        dir.mkdirs()
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val wrap = File(dir, "db.wrap")
        val store = DatabasePassphraseStore(wrap, AesGcmPassphraseCipher(key))
        store.getOrCreate()
        val corrupt = ByteArray(24) { it.toByte() }
        wrap.writeBytes(corrupt)

        val failed = runCatching { store.getOrCreate() }

        assertTrue(failed.isFailure)
        assertArrayEquals(corrupt, wrap.readBytes())
    }

    @Test
    fun failedEncryptionLeavesThePlaintextFile() {
        val dir = File(System.getProperty("java.io.tmpdir"), "tl-stage-${System.nanoTime()}")
        dir.mkdirs()
        val plain = File(dir, "journal.db")
        plain.writeBytes("SQLite format 3\u0000kept".toByteArray())
        val failed = runCatching {
            PlaintextDatabaseEncryptor.stageEncryptedDatabase(plain) {
                throw IllegalStateException("export failed")
            }
        }
        assertTrue(failed.isFailure)
        assertEquals("SQLite format 3\u0000kept", plain.readText())
        assertFalse(File(plain.path + ".encrypting").exists())
        assertFalse(File(plain.path + ".plaintext-bak").exists())
    }

    @Test
    fun successfulStagingReplacesThePlaintextFile() {
        val dir = File(System.getProperty("java.io.tmpdir"), "tl-stage-ok-${System.nanoTime()}")
        dir.mkdirs()
        val plain = File(dir, "journal.db")
        plain.writeBytes("SQLite format 3\u0000old".toByteArray())
        PlaintextDatabaseEncryptor.stageEncryptedDatabase(plain) { tmp ->
            tmp.writeBytes("encrypted-bytes".toByteArray())
        }
        assertEquals("encrypted-bytes", plain.readText())
        assertFalse(File(plain.path + ".plaintext-bak").exists())
        assertFalse(PlaintextDatabaseEncryptor.isPlaintextSqlite(plain))
    }

    @Test
    fun plaintextDatabaseBecomesEncryptedAndKeepsTheRow() {
        val so = sqlCipherLibrary()
        assumeTrue("SQLCipher native library is not in the Gradle cache", so != null)
        val loaded = runCatching { System.load(so!!.absolutePath) }.isSuccess
        assumeTrue("This host cannot load the Android SQLCipher binary", loaded)
        System.setProperty("truckerload.sqlcipher.lib", so!!.absolutePath)

        val context = RuntimeEnvironment.getApplication()
        val name = "encrypt-migrate-${System.nanoTime()}"
        val dbFile = context.getDatabasePath(name)
        dbFile.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { plain ->
            plain.execSQL(
                "CREATE TABLE loads (id TEXT PRIMARY KEY, totalRate REAL, deadheadMiles REAL NOT NULL DEFAULT 0.0)",
            )
            plain.execSQL("INSERT INTO loads (id, totalRate) VALUES ('T-1', 2500.0)")
            plain.version = 42
        }
        assertTrue(PlaintextDatabaseEncryptor.isPlaintextSqlite(dbFile))

        val passphrase = ByteArray(32) { index -> index.toByte() }
        PlaintextDatabaseEncryptor.ensureEncrypted(context, name, passphrase)

        assertFalse(PlaintextDatabaseEncryptor.isPlaintextSqlite(dbFile))
        val encrypted = CipherDatabase.openDatabase(
            dbFile.absolutePath,
            passphrase,
            null,
            CipherDatabase.OPEN_READONLY,
            null,
        )
        encrypted.use { database ->
            database.rawQuery("SELECT totalRate FROM loads WHERE id = 'T-1'", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(2500.0, cursor.getDouble(0), 0.001)
            }
            database.rawQuery("PRAGMA user_version", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(42, cursor.getInt(0))
            }
        }
        context.deleteDatabase(name)
    }

    @Test
    fun clearedEncryptedUserVersionIsRestoredFromTheMoneyColumns() {
        val so = sqlCipherLibrary()
        assumeTrue("SQLCipher native library is not in the Gradle cache", so != null)
        val loaded = runCatching { System.load(so!!.absolutePath) }.isSuccess
        assumeTrue("This host cannot load the Android SQLCipher binary", loaded)
        System.setProperty("truckerload.sqlcipher.lib", so!!.absolutePath)

        val context = RuntimeEnvironment.getApplication()
        val name = "encrypt-version-${System.nanoTime()}"
        val dbFile = context.getDatabasePath(name)
        dbFile.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { plain ->
            plain.execSQL(
                "CREATE TABLE loads (id TEXT PRIMARY KEY, totalRate REAL, deadheadMiles REAL NOT NULL DEFAULT 0.0)",
            )
            plain.version = 42
        }
        val passphrase = ByteArray(32) { index -> (index + 3).toByte() }
        PlaintextDatabaseEncryptor.ensureEncrypted(context, name, passphrase)
        CipherDatabase.openDatabase(
            dbFile.absolutePath,
            passphrase,
            null,
            CipherDatabase.OPEN_READWRITE,
            null,
        ).use { database ->
            database.rawExecSQL("PRAGMA user_version = 0")
        }

        PlaintextDatabaseEncryptor.restoreClearedUserVersion(context, name, passphrase)

        CipherDatabase.openDatabase(
            dbFile.absolutePath,
            passphrase,
            null,
            CipherDatabase.OPEN_READONLY,
            null,
        ).use { database ->
            database.rawQuery("PRAGMA user_version", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(42, cursor.getInt(0))
            }
        }
        context.deleteDatabase(name)
    }

    @Test
    fun restoredUserVersionFollowsTheMoneyColumnShape() {
        assertEquals(
            42,
            PlaintextDatabaseEncryptor.restoredUserVersion(0, "REAL", hasDeadheadMiles = true),
        )
        assertEquals(
            43,
            PlaintextDatabaseEncryptor.restoredUserVersion(0, "INTEGER", hasDeadheadMiles = true),
        )
        assertEquals(
            null,
            PlaintextDatabaseEncryptor.restoredUserVersion(42, "REAL", hasDeadheadMiles = true),
        )
        assertEquals(
            null,
            PlaintextDatabaseEncryptor.restoredUserVersion(0, "REAL", hasDeadheadMiles = false),
        )
    }

    private fun sqlCipherLibrary(): File? {
        val home = File(System.getProperty("user.home"), ".gradle/caches")
        return home.walkTopDown()
            .maxDepth(12)
            .firstOrNull { file ->
                file.name == "libsqlcipher.so" && file.parentFile.name == "x86_64"
            }
    }
}
