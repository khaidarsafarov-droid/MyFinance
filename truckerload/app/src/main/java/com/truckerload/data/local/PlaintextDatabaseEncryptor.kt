package com.truckerload.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import net.zetetic.database.sqlcipher.SQLiteDatabase as CipherDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File
import java.io.FileInputStream

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
        } catch (error: Throwable) {
            tmp.delete()
            if (movedOriginal && backup.exists() && !plain.exists()) {
                backup.renameTo(plain)
            }
            Log.e(TAG, "plaintext encryption failed; original database kept", error)
            throw error
        }
    }

    private fun exportPlaintextIntoEncrypted(plain: File, encrypted: File, passphrase: ByteArray) {
        val db = CipherDatabase.openOrCreateDatabase(encrypted.absolutePath, passphrase, null, null, null)
        try {
            val path = plain.absolutePath.replace("'", "''")
            db.rawExecSQL("ATTACH DATABASE '$path' AS plaintext KEY ''")
            db.rawExecSQL("SELECT sqlcipher_export('main', 'plaintext')")
            db.rawExecSQL("DETACH DATABASE plaintext")
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
