package com.truckerload.data.local

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import net.zetetic.database.sqlcipher.SQLiteDatabase as CipherDatabase
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SqlCipherOpenAndMigrationTest {

    @Test
    fun encryptedDatabaseReopensAndKeepsPhotoGpsAndTime() {
        loadSqlCipherOrSkip()
        val context = RuntimeEnvironment.getApplication()
        val name = "cipher-open-${System.nanoTime()}"
        val dbFile = context.getDatabasePath(name)
        dbFile.parentFile?.mkdirs()
        val passphrase = ByteArray(32) { index -> (index + 1).toByte() }

        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { plain ->
            plain.execSQL(PHOTOS_TABLE_SQL)
            plain.execSQL(PHOTO_INSERT_SQL)
        }
        PlaintextDatabaseEncryptor.ensureEncrypted(context, name, passphrase)
        assertFalse(PlaintextDatabaseEncryptor.isPlaintextSqlite(dbFile))

        openCipher(dbFile, passphrase).use { first ->
            assertPhotoMetadata(first)
        }
        openCipher(dbFile, passphrase).use { second ->
            assertPhotoMetadata(second)
        }
        context.deleteDatabase(name)
    }

    @Test
    fun wrongPassphraseDoesNotOpenEncryptedDatabase() {
        loadSqlCipherOrSkip()
        val context = RuntimeEnvironment.getApplication()
        val name = "cipher-wrong-${System.nanoTime()}"
        val dbFile = context.getDatabasePath(name)
        dbFile.parentFile?.mkdirs()
        val passphrase = ByteArray(32) { 7 }
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { plain ->
            plain.execSQL("CREATE TABLE loads (id TEXT PRIMARY KEY)")
            plain.execSQL("INSERT INTO loads (id) VALUES ('T-1')")
        }
        PlaintextDatabaseEncryptor.ensureEncrypted(context, name, passphrase)

        val wrong = ByteArray(32) { 9 }
        val failed = runCatching { openCipher(dbFile, wrong).close() }
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
    fun sqlCipherMigration25To26_keepsPhotoGpsAndTime() {
        loadSqlCipherOrSkip()
        val context = RuntimeEnvironment.getApplication()
        val name = "cipher-mig-25-26-${System.nanoTime()}"
        context.deleteDatabase(name)
        val passphrase = ByteArray(32) { index -> (index + 11).toByte() }
        openLegacyPhotosHelper(
            context,
            name,
            PlaintextDatabaseEncryptor.openHelperFactory(passphrase),
        ).writableDatabase.use { db ->
            MIGRATION_25_26.migrate(db)
            assertMigratedPhotoMetadata(db)
        }
        context.deleteDatabase(name)
    }

    @Test
    fun roomMigration25To26_keepsPhotoGpsAndTime() {
        val context = RuntimeEnvironment.getApplication()
        val name = "room-mig-25-26-gps-${System.nanoTime()}"
        context.deleteDatabase(name)
        openLegacyPhotosHelper(
            context,
            name,
            FrameworkSQLiteOpenHelperFactory(),
        ).writableDatabase.use { db ->
            MIGRATION_25_26.migrate(db)
            assertMigratedPhotoMetadata(db)
        }
        context.deleteDatabase(name)
    }

    private fun openLegacyPhotosHelper(
        context: android.content.Context,
        name: String,
        factory: SupportSQLiteOpenHelper.Factory,
    ): SupportSQLiteOpenHelper {
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(25) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(PHOTOS_TABLE_SQL)
                    db.execSQL(SCANS_TABLE_SQL)
                    db.execSQL(PHOTO_INSERT_SQL)
                }

                override fun onUpgrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int,
                ) = Unit
            })
            .build()
        return factory.create(config)
    }

    private fun openCipher(dbFile: File, passphrase: ByteArray): CipherDatabase =
        CipherDatabase.openDatabase(
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

    private fun assertMigratedPhotoMetadata(db: SupportSQLiteDatabase) {
        db.query(
            "SELECT latitude, longitude, timestamp, city, state, zipCode, cloudMediaId FROM photos WHERE id = 'photo-1'",
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
        assertTrue(columnExists(db, "photos", "cloudSyncStatus"))
    }

    private fun columnExists(
        db: SupportSQLiteDatabase,
        table: String,
        column: String,
    ): Boolean {
        db.query("PRAGMA table_info($table)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == column) return true
            }
        }
        return false
    }

    private fun loadSqlCipherOrSkip() {
        val so = sqlCipherLibrary()
        assumeTrue("SQLCipher native library is not in the Gradle cache", so != null)
        val loaded = runCatching { System.load(so!!.absolutePath) }.isSuccess
        assumeTrue("This host cannot load the Android SQLCipher binary", loaded)
        System.setProperty("truckerload.sqlcipher.lib", so!!.absolutePath)
    }

    private fun sqlCipherLibrary(): File? {
        val home = File(System.getProperty("user.home"), ".gradle/caches")
        return home.walkTopDown()
            .maxDepth(12)
            .firstOrNull { file ->
                file.name == "libsqlcipher.so" && file.parentFile?.name == "x86_64"
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
