package com.truckerload.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import net.zetetic.database.sqlcipher.SQLiteDatabase as CipherDatabase

/**
 * Real SQLCipher on a real Android runtime. The Robolectric twins of these tests
 * cannot load the Android native library on a Linux JVM and skip there, so this class
 * is the CI gate for the encryption path. It never skips: if the native library does
 * not load, the tests fail. CI also checks that this class actually ran.
 */
@RunWith(AndroidJUnit4::class)
class SqlCipherInstrumentedTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun plaintextDatabaseBecomesEncryptedAndKeepsTheRowAndVersion() {
        val name = "it-encrypt-${System.nanoTime()}"
        val dbFile = context.getDatabasePath(name).also { it.parentFile?.mkdirs() }
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
        openCipher(dbFile, passphrase).use { database ->
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
        val name = "it-version-${System.nanoTime()}"
        val dbFile = context.getDatabasePath(name).also { it.parentFile?.mkdirs() }
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
        ).use { database -> database.rawExecSQL("PRAGMA user_version = 0") }

        PlaintextDatabaseEncryptor.restoreClearedUserVersion(context, name, passphrase)

        openCipher(dbFile, passphrase).use { database ->
            database.rawQuery("PRAGMA user_version", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(42, cursor.getInt(0))
            }
        }
        context.deleteDatabase(name)
    }

    @Test
    fun encryptedDatabaseReopensAndKeepsPhotoGpsAndTime() {
        val name = "it-cipher-open-${System.nanoTime()}"
        val dbFile = context.getDatabasePath(name).also { it.parentFile?.mkdirs() }
        val passphrase = ByteArray(32) { index -> (index + 1).toByte() }
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { plain ->
            plain.execSQL(PHOTOS_TABLE_SQL)
            plain.execSQL(PHOTO_INSERT_SQL)
        }
        PlaintextDatabaseEncryptor.ensureEncrypted(context, name, passphrase)
        assertFalse(PlaintextDatabaseEncryptor.isPlaintextSqlite(dbFile))

        repeat(2) {
            openCipher(dbFile, passphrase).use { database -> assertPhotoMetadata(database) }
        }
        context.deleteDatabase(name)
    }

    @Test
    fun wrongPassphraseDoesNotOpenEncryptedDatabase() {
        val name = "it-cipher-wrong-${System.nanoTime()}"
        val dbFile = context.getDatabasePath(name).also { it.parentFile?.mkdirs() }
        val passphrase = ByteArray(32) { 7 }
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { plain ->
            plain.execSQL("CREATE TABLE loads (id TEXT PRIMARY KEY)")
            plain.execSQL("INSERT INTO loads (id) VALUES ('T-1')")
        }
        PlaintextDatabaseEncryptor.ensureEncrypted(context, name, passphrase)

        val failed = runCatching { openCipher(dbFile, ByteArray(32) { 9 }).close() }
        assertTrue(failed.isFailure)
        assertTrue(failed.exceptionOrNull() is SQLiteException)

        openCipher(dbFile, passphrase).use { database ->
            database.rawQuery("SELECT id FROM loads WHERE id = 'T-1'", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("T-1", cursor.getString(0))
            }
        }
        context.deleteDatabase(name)
    }

    @Test
    fun sqlCipherMigration25To26KeepsPhotoGpsAndTime() {
        val name = "it-cipher-mig-25-26-${System.nanoTime()}"
        context.deleteDatabase(name)
        val passphrase = ByteArray(32) { index -> (index + 11).toByte() }
        PlaintextDatabaseEncryptor.openHelperFactory(passphrase)
            .create(legacyPhotosConfig(name))
            .writableDatabase
            .use { db ->
                MIGRATION_25_26.migrate(db)
                db.query(
                    "SELECT latitude, longitude, timestamp, city, state, zipCode, cloudMediaId " +
                        "FROM photos WHERE id = 'photo-1'",
                ).use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(35.711001, cursor.getDouble(0), 0.0)
                    assertEquals(-78.614002, cursor.getDouble(1), 0.0)
                    assertEquals(1_720_000_000_000L, cursor.getLong(2))
                    assertEquals("Garner", cursor.getString(3))
                    assertEquals("NC", cursor.getString(4))
                    assertEquals("27529", cursor.getString(5))
                    assertTrue(cursor.isNull(6))
                }
            }
        context.deleteDatabase(name)
    }

    private fun legacyPhotosConfig(name: String): SupportSQLiteOpenHelper.Configuration =
        SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(25) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(PHOTOS_TABLE_SQL)
                    db.execSQL(SCANS_TABLE_SQL)
                    db.execSQL(PHOTO_INSERT_SQL)
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

    private fun openCipher(dbFile: java.io.File, passphrase: ByteArray): CipherDatabase = CipherDatabase.openDatabase(
        dbFile.absolutePath,
        passphrase,
        null,
        CipherDatabase.OPEN_READONLY,
        null,
    )

    private fun assertPhotoMetadata(database: CipherDatabase) {
        database.rawQuery(
            "SELECT latitude, longitude, timestamp, city, state, zipCode FROM photos WHERE id = 'photo-1'",
            null,
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(35.711001, cursor.getDouble(0), 0.0)
            assertEquals(-78.614002, cursor.getDouble(1), 0.0)
            assertEquals(1_720_000_000_000L, cursor.getLong(2))
            assertEquals("Garner", cursor.getString(3))
            assertEquals("NC", cursor.getString(4))
            assertEquals("27529", cursor.getString(5))
        }
    }

    private companion object {
        const val PHOTOS_TABLE_SQL = """
            CREATE TABLE photos (
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
        """
        const val SCANS_TABLE_SQL = """
            CREATE TABLE scans (
                id TEXT NOT NULL PRIMARY KEY,
                fileName TEXT NOT NULL,
                filePath TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                loadId TEXT
            )
        """
        const val PHOTO_INSERT_SQL = """
            INSERT INTO photos (id, fileName, filePath, latitude, longitude, city, state, zipCode, timestamp, loadId)
            VALUES ('photo-1', 'garner.jpg', '/photos/garner.jpg', 35.711001, -78.614002, 'Garner', 'NC', '27529', 1720000000000, 'load-1')
        """
    }
}
