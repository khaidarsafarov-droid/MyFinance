package com.truckerload.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import com.truckerload.utils.logFailure
import net.zetetic.database.sqlcipher.SQLiteDatabase as CipherDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File
import java.io.FileInputStream
import java.io.IOException

/**
 * Turns a plaintext Room file into a SQLCipher database.
 *
 * The original file is moved aside only after `sqlcipher_export` finishes.
 * Any failure deletes the temporary encrypted file and leaves the plaintext
 * database where Room found it.
 *
 * Google Drive backup does not upload this file. [com.truckerload.utils.BackupService]
 * reads rows through Room (already decrypted) and writes JSON. Restore writes
 * those rows back through Room into the encrypted database.
 */
internal object PlaintextDatabaseEncryptor {
    private const val TAG = "DbEncrypt"

    fun openHelperFactory(passphrase: ByteArray): SupportOpenHelperFactory {
        loadSqlCipher()
        return SupportOpenHelperFactory(passphrase)
    }

    fun ensureEncrypted(context: Context, databaseName: String, passphrase: ByteArray) {
        val dbFile = context.getDatabasePath(databaseName)
        if (!dbFile.exists() || dbFile.length() == 0L) return
        if (!isPlaintextSqlite(dbFile)) return
        loadSqlCipher()
        checkpointPlaintext(dbFile)
        stageEncryptedDatabase(dbFile) { tmp ->
            exportPlaintextIntoEncrypted(dbFile, tmp, passphrase)
        }
    }

    /**
     * Encryption of an older build left `PRAGMA user_version` at 0. Room then
     * validates the dollar REAL columns as a new database and crashes before
     * [MIGRATION_42_43] can convert them to cents.
     */
    fun restoreClearedUserVersion(context: Context, databaseName: String, passphrase: ByteArray) {
        val dbFile = context.getDatabasePath(databaseName)
        if (!dbFile.exists() || dbFile.length() == 0L || isPlaintextSqlite(dbFile)) return
        loadSqlCipher()
        val db = CipherDatabase.openDatabase(
            dbFile.absolutePath,
            passphrase,
            null,
            CipherDatabase.OPEN_READWRITE,
            null,
        )
        db.use { database ->
            val info = loadsMoneyColumns(database)
            val restored = restoredUserVersion(
                currentVersion = readIntPragma(database, "PRAGMA user_version"),
                totalRateType = info.totalRateType,
                hasDeadheadMiles = info.hasDeadheadMiles,
            ) ?: return@use
            database.rawExecSQL("PRAGMA user_version = $restored")
        }
    }

    /**
     * Version 42 is the last schema that stored load money as REAL and already
     * had `deadheadMiles`. Version 43 stores those amounts as integer cents.
     * Older REAL schemas are left alone so a guessed version cannot drop them.
     */
    internal fun restoredUserVersion(
        currentVersion: Int,
        totalRateType: String?,
        hasDeadheadMiles: Boolean,
    ): Int? {
        if (currentVersion > 0) return null
        return when (totalRateType?.uppercase()) {
            "INTEGER", "INT" -> 43
            "REAL" -> if (hasDeadheadMiles) 42 else null
            else -> null
        }
    }

    /**
     * Writes the encrypted file beside [plain], then swaps it into place.
     * If [writeEncrypted] throws, [plain] is unchanged.
     */
    internal fun stageEncryptedDatabase(plain: File, writeEncrypted: (File) -> Unit) {
        val tmp = File(plain.path + ".encrypting")
        val backup = File(plain.path + ".plaintext-bak")
        if (tmp.exists()) tmp.delete()
        var movedOriginal = false
        try {
            writeEncrypted(tmp)
            if (backup.exists()) backup.delete()
            if (!plain.renameTo(backup)) error("could not move the plaintext database aside")
            movedOriginal = true
            if (!tmp.renameTo(plain)) error("could not install the encrypted database")
            File(plain.path + "-wal").delete()
            File(plain.path + "-shm").delete()
            backup.delete()
        } catch (error: IOException) {
            restorePlaintextAfterFailedEncryption(tmp, backup, plain, movedOriginal, error)
        } catch (error: SQLiteException) {
            restorePlaintextAfterFailedEncryption(tmp, backup, plain, movedOriginal, error)
        } catch (error: IllegalStateException) {
            restorePlaintextAfterFailedEncryption(tmp, backup, plain, movedOriginal, error)
        } catch (error: IllegalArgumentException) {
            restorePlaintextAfterFailedEncryption(tmp, backup, plain, movedOriginal, error)
        }
    }

    private fun restorePlaintextAfterFailedEncryption(
        tmp: File,
        backup: File,
        plain: File,
        movedOriginal: Boolean,
        error: Exception,
    ): Nothing {
        tmp.delete()
        if (movedOriginal && backup.exists() && !plain.exists()) {
            backup.renameTo(plain)
        }
        logFailure(TAG, "plaintext encryption failed; original database kept", error)
        throw error
    }

    private fun exportPlaintextIntoEncrypted(plain: File, encrypted: File, passphrase: ByteArray) {
        val db = CipherDatabase.openOrCreateDatabase(encrypted.absolutePath, passphrase, null, null, null)
        try {
            val path = plain.absolutePath.replace("'", "''")
            db.rawExecSQL("ATTACH DATABASE '$path' AS plaintext KEY ''")
            db.rawExecSQL("SELECT sqlcipher_export('main', 'plaintext')")
            // sqlcipher_export copies tables, not PRAGMA user_version. A 0 version
            // makes Room call onCreate and reject the existing file as pre-packaged.
            val version = readIntPragma(db, "PRAGMA plaintext.user_version")
            db.rawExecSQL("DETACH DATABASE plaintext")
            if (version > 0) {
                db.rawExecSQL("PRAGMA user_version = $version")
            }
        } finally {
            db.close()
        }
    }

    private fun checkpointPlaintext(dbFile: File) {
        val plain = SQLiteDatabase.openDatabase(
            dbFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE,
        )
        plain.use { database ->
            database.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { it.moveToFirst() }
        }
    }

    fun isPlaintextSqlite(file: File): Boolean {
        if (!file.exists() || file.length() < 16L) return false
        val header = ByteArray(16)
        FileInputStream(file).use { input ->
            if (input.read(header) != header.size) return false
        }
        return header.contentEquals(SQLITE_HEADER)
    }

    private val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    private data class LoadsMoneyColumns(
        val totalRateType: String?,
        val hasDeadheadMiles: Boolean,
    )

    private fun loadsMoneyColumns(db: CipherDatabase): LoadsMoneyColumns {
        var totalRateType: String? = null
        var hasDeadheadMiles = false
        db.rawQuery("PRAGMA table_info(loads)", null).use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            val typeIndex = cursor.getColumnIndex("type")
            if (nameIndex < 0 || typeIndex < 0) return LoadsMoneyColumns(null, false)
            while (cursor.moveToNext()) {
                when (cursor.getString(nameIndex)) {
                    "totalRate" -> totalRateType = cursor.getString(typeIndex)
                    "deadheadMiles" -> hasDeadheadMiles = true
                }
            }
        }
        return LoadsMoneyColumns(totalRateType, hasDeadheadMiles)
    }

    private fun readIntPragma(db: CipherDatabase, pragma: String): Int {
        db.rawQuery(pragma, null).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    private fun loadSqlCipher() {
        if (loaded) return
        val override = System.getProperty("truckerload.sqlcipher.lib")
        if (!override.isNullOrBlank()) {
            System.load(override)
        } else {
            System.loadLibrary("sqlcipher")
        }
        loaded = true
    }

    @Volatile
    private var loaded = false
}
