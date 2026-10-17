package com.truckerload.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.truckerload.data.preferences.AccountIds
import com.truckerload.data.preferences.AuthProvider
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.preferences.TelegramTokenStore
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SQLiteDatabase as CipherDatabase

/**
 * An update can open a new empty profile while the previous journal is still
 * on disk (the Room file, token, and chat id are all named after the user id).
 * When there is exactly one journal that still has loads, attach it again.
 * Two populated journals are left alone — guessing would mix drivers.
 */
object InstalledJournalRecovery {
    private const val TAG = "JournalRecovery"
    private val started = AtomicBoolean(false)
    private val sidecars = listOf("-wal", "-shm", "-journal")

    fun recoverIfNeeded(context: Context, authStore: AuthStore) {
        if (!started.compareAndSet(false, true)) return
        runCatching { recover(context.applicationContext, authStore) }
            .onFailure { error -> Log.e(TAG, "Installed journal recovery failed", error) }
    }

    internal fun recover(context: Context, authStore: AuthStore) {
        val journals = scan(context)
        val populated = journals.filter { it.loadCount > 0 }
        val activeId = authStore.currentUserIdOrNull()
        if (activeId == null) {
            val only = populated.singleOrNull() ?: return
            if (only.userId == null) {
                adoptInto(context, AccountIds.LOCAL_DEV, only)
                loginAs(authStore, AccountIds.LOCAL_DEV)
            } else {
                loginAs(authStore, only.userId)
            }
            Log.i(TAG, "Restored signed-out journal ${only.databaseName}")
            return
        }
        val activeName = AppDatabase.databaseNameFor(activeId)
        val activeCount = journals.firstOrNull { it.databaseName == activeName }?.loadCount ?: 0
        if (activeCount > 0) return
        val only = populated.filter { it.databaseName != activeName }.singleOrNull() ?: return
        adoptInto(context, activeId, only)
        Log.i(TAG, "Copied ${only.databaseName} into empty profile $activeId")
    }

    /**
     * Platform SQLite cannot open a SQLCipher file, so a ciphertext journal
     * must still count as present. Plaintext files have to pass integrity_check.
     */
    internal fun isPresentDatabase(file: File): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        if (PlaintextDatabaseEncryptor.isPlaintextSqlite(file)) {
            return DatabaseFileCopy.isHealthyDatabase(file)
        }
        return file.length() > 100L
    }

    private fun adoptInto(context: Context, targetUserId: String, source: JournalFile) {
        copyDatabase(context, source, targetUserId)
        copyToken(context, source.userId, targetUserId)
        copyLink(context, source.userId, targetUserId)
    }

    private fun loginAs(authStore: AuthStore, userId: String) {
        val provider = when {
            userId == AccountIds.LOCAL_DEV -> AuthProvider.LOCAL
            userId.startsWith("google_") -> AuthProvider.GOOGLE
            else -> AuthProvider.EMAIL
        }
        authStore.login(
            userId = userId,
            email = "local@device",
            rememberMe = true,
            provider = provider,
        )
    }

    private fun scan(context: Context): List<JournalFile> {
        val dir = context.getDatabasePath(LegacyDatabaseAbsorb.LEGACY_SINGLE_FILE).parentFile
        if (dir == null || !dir.isDirectory) return emptyList()
        return dir.listFiles().orEmpty().mapNotNull { file ->
            val name = file.name
            if (!file.isFile || shouldSkip(name)) return@mapNotNull null
            val userId = when {
                name == LegacyDatabaseAbsorb.LEGACY_SINGLE_FILE -> null
                name.startsWith("truckerload_") -> name.removePrefix("truckerload_").ifBlank { return@mapNotNull null }
                else -> return@mapNotNull null
            }
            JournalFile(file, name, userId, countLoads(context, file, name))
        }
    }

    private fun shouldSkip(name: String): Boolean {
        if (sidecars.any { name.endsWith(it) }) return true
        return name.endsWith(".plaintext-bak") ||
            name.endsWith(".encrypting") ||
            name.endsWith(".tmp") ||
            name.endsWith(".adopting")
    }

    private fun countLoads(context: Context, file: File, databaseName: String): Int {
        if (!file.exists() || file.length() == 0L) return 0
        return runCatching {
            if (PlaintextDatabaseEncryptor.isPlaintextSqlite(file)) {
                countPlaintextLoads(file)
            } else {
                countEncryptedLoads(context, file, databaseName)
            }
        }.getOrDefault(0)
    }

    private fun countPlaintextLoads(file: File): Int {
        return SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            val hasLoads = db.rawQuery(
                "SELECT 1 FROM sqlite_master WHERE type='table' AND name='loads'",
                null,
            ).use { it.moveToFirst() }
            if (!hasLoads) return 0
            db.rawQuery("SELECT COUNT(*) FROM loads", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
        }
    }

    private fun countEncryptedLoads(context: Context, file: File, databaseName: String): Int {
        val passphrase = DatabasePassphraseStore.forDatabase(context, databaseName).readIfPresent()
            ?: return 0
        PlaintextDatabaseEncryptor.loadSqlCipher()
        val db = CipherDatabase.openDatabase(
            file.absolutePath,
            passphrase,
            null,
            CipherDatabase.OPEN_READONLY,
            null,
        )
        db.use { database ->
            val hasLoads = database.rawQuery(
                "SELECT 1 FROM sqlite_master WHERE type='table' AND name='loads'",
                null,
            ).use { it.moveToFirst() }
            if (!hasLoads) return 0
            return database.rawQuery("SELECT COUNT(*) FROM loads", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
        }
    }

    /**
     * Byte copy, not [DatabaseFileCopy.copyWithSidecars]: that helper opens the
     * copy with platform SQLite and deletes it when the file is SQLCipher.
     */
    private fun copyDatabase(context: Context, source: JournalFile, targetUserId: String) {
        val targetName = AppDatabase.databaseNameFor(targetUserId)
        if (targetName == source.databaseName) return
        val target = context.getDatabasePath(targetName)
        try {
            DatabaseFileCopy.deleteDbTree(target)
            target.parentFile?.mkdirs()
            source.file.copyTo(target, overwrite = false)
            sidecars.forEach { suffix ->
                val side = File(source.file.path + suffix)
                if (side.exists()) side.copyTo(File(target.path + suffix), overwrite = false)
            }
            copyWrap(context, source.databaseName, targetName)
        } catch (error: Throwable) {
            DatabaseFileCopy.deleteDbTree(target)
            throw error
        }
    }

    private fun copyWrap(context: Context, sourceName: String, targetName: String) {
        if (sourceName == targetName) return
        val dir = File(context.filesDir, "room-passphrases")
        val sourceWrap = File(dir, "$sourceName.wrap")
        val targetWrap = File(dir, "$targetName.wrap")
        if (sourceWrap.exists() && sourceWrap.length() > 0L) {
            dir.mkdirs()
            sourceWrap.copyTo(targetWrap, overwrite = true)
        } else if (targetWrap.exists()) {
            targetWrap.delete()
        }
    }

    private fun copyToken(context: Context, sourceUserId: String?, targetUserId: String) {
        runCatching {
            val target = TelegramTokenStore(context, targetUserId)
            if (target.getToken().isNotBlank()) return
            val token = TelegramTokenStore(context, sourceUserId).getToken()
            if (token.isNotBlank()) target.setToken(token)
        }.onFailure { error -> Log.w(TAG, "token copy skipped", error) }
    }

    private fun copyLink(context: Context, sourceUserId: String?, targetUserId: String) {
        runCatching {
            runBlocking(Dispatchers.IO) {
                SettingsDataStore(context).copyTelegramLinkIfTargetEmpty(sourceUserId, targetUserId)
            }
        }.onFailure { error -> Log.w(TAG, "telegram link copy skipped", error) }
    }

    private data class JournalFile(
        val file: File,
        val databaseName: String,
        val userId: String?,
        val loadCount: Int,
    )
}
