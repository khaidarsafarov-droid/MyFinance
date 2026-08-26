package com.truckerload.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.edit
import com.truckerload.R
import com.truckerload.data.backup.BackupData
import com.truckerload.data.backup.BackupDataCodec
import com.truckerload.data.backup.BackupPrefsApplier
import com.truckerload.data.backup.BackupRestoreException
import com.truckerload.data.backup.BackupRestoreParser
import com.truckerload.data.backup.BackupRoomApplier
import com.truckerload.data.backup.BackupSchema
import com.truckerload.data.backup.BackupSnapshotBuilder
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.AccountIds
import com.truckerload.data.preferences.AuthStore
import com.truckerload.widget.WidgetDataUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

object BackupService {

    private const val PREFS = "backup_companion"
    private const val AUTO_BACKUP_SUBDIR = "backups/auto" // legacy; prefer [autoBackupDir] per user
    private const val DEFAULT_KEEP_COUNT = 5
    private const val TAG = "BackupRestore"
    private const val AUTO_BACKUP_DEBOUNCE_MS = 45_000L

    private val autoBackupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val autoBackupTimestampRef = AtomicReference(SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US))
    private val autoBackupMutex = Mutex()
    @Volatile private var pendingAutoBackupJob: Job? = null

    data class CreateResult(
        val save: StorageHelper.SaveResult,
        val loadCount: Int,
        val mimeType: String = BackupSchema.JSON_MIME,
    )

    /** Debounced auto-backup (coalesces rapid load edits into one write). */
    fun scheduleCreateAutoBackup(context: Context) {
        val appContext = context.applicationContext
        pendingAutoBackupJob?.cancel()
        pendingAutoBackupJob = autoBackupScope.launch {
            delay(AUTO_BACKUP_DEBOUNCE_MS)
            autoBackupMutex.withLock {
                runCatching { createAutoBackup(appContext) }
                    .onFailure { e -> Log.e(TAG, "createAutoBackup failed", e) }
            }
        }
    }

    suspend fun createAutoBackup(context: Context) = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val db = AppDatabase.getInstanceForActiveUser(appContext) ?: return@withContext
        val backup = BackupSnapshotBuilder.build(appContext, db)
        if (!BackupSnapshotBuilder.hasExportableContent(backup)) return@withContext

        val accountId = backup.accountId
        val json = BackupDataCodec.toJson(backup)
        val dir = autoBackupDir(appContext, accountId).apply { mkdirs() }
        val fileName = "auto_backup_${formatAutoBackupTimestamp()}.tlb"
        File(dir, fileName).writeText(json, Charsets.UTF_8)
        pruneAutoBackups(appContext, DEFAULT_KEEP_COUNT)
        Log.d(TAG, "createAutoBackup saved $fileName (${backup.loads.size} loads)")
        runCatching {
            com.truckerload.data.backup.GoogleDriveBackupService.pushAutoBackupIfEnabled(appContext)
        }.onFailure { e -> Log.e(TAG, "Drive auto-push failed", e) }
    }

    suspend fun restoreFromFile(context: Context, file: File): Result<Int> = withContext(Dispatchers.IO) {
        if (!file.exists() || !file.isFile) {
            return@withContext Result.failure(
                IllegalStateException(context.getString(R.string.auto_restore_no_file))
            )
        }
        restoreFromJson(context, file.readText(Charsets.UTF_8)).map { backup ->
            WidgetDataUpdater.updateWidgetData(context.applicationContext)
            backup.loads.size
        }
    }

    fun getLatestAutoBackup(context: Context): File? =
        getAutoBackups(context).firstOrNull()

    suspend fun restoreLatestAutoBackup(context: Context): Result<Int> = withContext(Dispatchers.IO) {
        val latest = getLatestAutoBackup(context)
            ?: return@withContext Result.failure(
                IllegalStateException(context.getString(R.string.auto_restore_no_file))
            )
        restoreFromFile(context, latest)
    }

    fun getAutoBackups(context: Context): List<File> {
        val dir = autoBackupDir(context)
        if (!dir.isDirectory) return emptyList()
        return dir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".tlb", ignoreCase = true) }
            ?.sortedByDescending { backupSortKey(it) }
            .orEmpty()
    }

    fun pruneAutoBackups(context: Context, keepCount: Int = DEFAULT_KEEP_COUNT) {
        val files = getAutoBackups(context)
        files.drop(keepCount).forEach { runCatching { it.delete() } }
    }

    suspend fun hasAutoBackups(context: Context): Boolean = withContext(Dispatchers.IO) {
        getAutoBackups(context).isNotEmpty()
    }

    /** Full-account JSON (journal + ТО + settings) for local file or Google Drive. */
    suspend fun createBackupJson(context: Context): String? = withContext(Dispatchers.IO) {
        runCatching {
            val appContext = context.applicationContext
            val db = AppDatabase.getInstanceForActiveUser(appContext) ?: return@withContext null
            val backup = BackupSnapshotBuilder.build(appContext, db)
            if (!BackupSnapshotBuilder.hasExportableContent(backup)) return@withContext null
            BackupDataCodec.toJson(backup)
        }.getOrElse { e ->
            Log.e(TAG, "createBackupJson failed", e)
            null
        }
    }

    suspend fun restoreBackupJson(context: Context, json: String): Result<BackupData> =
        restoreFromJson(context, json).onSuccess {
            WidgetDataUpdater.updateWidgetData(context.applicationContext)
        }

    suspend fun createManualBackup(context: Context): CreateResult? = withContext(Dispatchers.IO) {
        try {
            val appContext = context.applicationContext
            val db = AppDatabase.getInstanceForActiveUser(appContext) ?: return@withContext null
            val backup = BackupSnapshotBuilder.build(appContext, db)
            if (!BackupSnapshotBuilder.hasExportableContent(backup)) return@withContext null
            val json = BackupDataCodec.toJson(backup)
            val jsonBytes = BackupDataCodec.toUtf8Bytes(backup)
            val fileName = BackupSchema.jsonFileName(backup.exportedAt)
            val storageHelper = StorageHelper(appContext)

            val saveResult = storageHelper.saveToPublicDownloads(
                fileName,
                BrandConstants.DOWNLOADS_FOLDER,
                BackupSchema.JSON_MIME,
            ) { out ->
                out.write(jsonBytes)
            } ?: run {
                val file = storageHelper.saveToAppStorage(fileName, "backups") { out ->
                    out.write(jsonBytes)
                }
                StorageHelper.SaveResult(
                    storageHelper.getShareableUri(file),
                    "${BrandConstants.DOWNLOADS_FOLDER}/$fileName",
                )
            }

            saveCompanionBackup(appContext, fileName, json)
            CreateResult(
                save = saveResult,
                loadCount = backup.loads.size,
                mimeType = BackupSchema.JSON_MIME,
            )
        } catch (e: Exception) {
            Log.e(TAG, "createManualBackup failed", e)
            null
        }
    }

    fun shareBackupFile(context: Context, uri: Uri) {
        val appContext = context.applicationContext
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = BackupSchema.JSON_MIME
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, appContext.getString(R.string.settings_backup_share_title))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        appContext.startActivity(
            Intent.createChooser(intent, appContext.getString(R.string.settings_backup_share_title))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun shareNoteText(context: Context, visibleText: String) {
        if (visibleText.isBlank()) return
        val appContext = context.applicationContext
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, visibleText)
            putExtra(Intent.EXTRA_TITLE, appContext.getString(R.string.settings_backup_share_title))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        appContext.startActivity(
            Intent.createChooser(intent, appContext.getString(R.string.settings_backup_share_title))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    suspend fun restoreFromUri(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val appContext = context.applicationContext
            val fileName = queryDisplayName(appContext, uri)
            readCompanionJson(appContext, fileName)?.let {
                return@withContext restoreFromJson(appContext, it).map { backup ->
                    appContext.getString(
                        R.string.backup_restore_success,
                        backup.loads.size,
                        backup.paychecks.size,
                        backup.diesel.size
                    )
                }
            }

            val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext Result.failure(BackupRestoreException.ReadFailed())

            val json = BackupRestoreParser.parseToJson(bytes)
                .getOrElse { err ->
                    Log.e(TAG, "restoreFromUri rejected: ${err.javaClass.simpleName}")
                    return@withContext Result.failure(err)
                }

            restoreFromJson(appContext, json).map { backup ->
                appContext.getString(
                    R.string.backup_restore_success,
                    backup.loads.size,
                    backup.paychecks.size,
                    backup.diesel.size
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "restoreFromUri failed: ${e.javaClass.simpleName}")
            Result.failure(e)
        }
    }

    suspend fun restoreLatestCompanionBackupIfEmpty(context: Context): Result<String>? = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val db = AppDatabase.getInstanceForActiveUser(appContext) ?: return@withContext null
        if (db.loadDao().getAllLoadsOnce().isNotEmpty()) return@withContext null

        val userId = AuthStore(appContext).currentUserIdOrNull() ?: return@withContext null
        // FIX: only scan this account's companion dir — shared pool restored wrong user's journal
        val dir = companionBackupDir(appContext, userId)
        if (!dir.isDirectory) return@withContext null

        val latest = dir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".tlb", ignoreCase = true) }
            ?.maxByOrNull { it.lastModified() }
            ?: return@withContext null

        Log.i(TAG, "Auto-restore from companion ${latest.name}")
        restoreFromJson(appContext, latest.readText(Charsets.UTF_8)).map { backup ->
            WidgetDataUpdater.updateWidgetData(appContext)
            appContext.getString(
                R.string.backup_restore_success,
                backup.loads.size,
                backup.paychecks.size,
                backup.diesel.size
            )
        }
    }

    private fun autoBackupDir(context: Context, userId: String? = AuthStore(context).currentUserIdOrNull()): File {
        val part = AccountIds.sanitizeFilePart(userId ?: AccountIds.LOCAL_DEV)
        return File(context.getExternalFilesDir(null), "backups/$part/auto")
    }

    private fun companionBackupDir(context: Context, userId: String): File =
        File(context.getExternalFilesDir(null), "backups/${AccountIds.sanitizeFilePart(userId)}")

    private fun backupSortKey(file: File): String {
        val name = file.name
        val stamp = name.removePrefix("auto_backup_").removeSuffix(".tlb")
        return stamp.ifBlank { file.lastModified().toString() }
    }

    private fun saveCompanionBackup(context: Context, txtFileName: String, json: String) {
        val userId = AuthStore(context).currentUserIdOrNull() ?: AccountIds.LOCAL_DEV
        val dir = companionBackupDir(context, userId).apply { mkdirs() }
        val companion = File(dir, BackupNoteFormatter.companionFileName(txtFileName))
        companion.writeText(json, Charsets.UTF_8)
        // FIX: companion map was global — account B could restore A's path by display name
        companionPrefs(context, userId).edit {
            putString(txtFileName, companion.absolutePath)
        }
    }

    private fun readCompanionJson(context: Context, txtFileName: String?): String? {
        if (txtFileName.isNullOrBlank()) return null
        val userId = AuthStore(context).currentUserIdOrNull() ?: return null
        val dir = companionBackupDir(context, userId)
        val path = companionPrefs(context, userId).getString(txtFileName, null) ?: return null
        val file = File(path)
        if (!file.exists() || !file.isFile) return null
        // FIX: never follow a prefs path outside this account's companion directory
        if (!isUnderDirectory(file, dir)) return null
        return BackupDataCodec.stripBom(file.readText(Charsets.UTF_8)).trim()
            .takeIf { it.startsWith("{") }
    }

    private fun companionPrefs(context: Context, userId: String) =
        context.getSharedPreferences(
            "${PREFS}_${AccountIds.sanitizeFilePart(userId)}",
            Context.MODE_PRIVATE,
        )

    private fun isUnderDirectory(file: File, dir: File): Boolean {
        val canonical = runCatching { file.canonicalFile }.getOrElse { return false }
        val base = runCatching { dir.canonicalFile }.getOrElse { return false }
        val prefix = base.path + File.separator
        return canonical.path == base.path || canonical.path.startsWith(prefix)
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? =
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
        }

    private suspend fun restoreFromJson(context: Context, json: String): Result<BackupData> {
        val backup = try {
            BackupDataCodec.decode(json)
        } catch (e: BackupRestoreException) {
            return Result.failure(e)
        }

        val db = AppDatabase.getInstanceForActiveUser(context.applicationContext)
            ?: return Result.failure(IllegalStateException("No active user session"))
        val activeUserId = AuthStore(context.applicationContext).currentUserIdOrNull()
        // FIX: refuse restore when backup.accountId is bound to a different account
        if (!backup.accountId.isNullOrBlank() &&
            !activeUserId.isNullOrBlank() &&
            backup.accountId != activeUserId
        ) {
            return Result.failure(BackupRestoreException.WrongAccount())
        }
        BackupRoomApplier.applyFullReplace(db, backup)
        BackupRoomApplier.pruneOrphanMedia(db)
        BackupPrefsApplier.apply(context.applicationContext, backup.appSettings)

        return Result.success(backup)
    }

    private fun formatAutoBackupTimestamp(): String =
        synchronized(autoBackupTimestampRef) {
            autoBackupTimestampRef.get().format(Date())
        }
}
