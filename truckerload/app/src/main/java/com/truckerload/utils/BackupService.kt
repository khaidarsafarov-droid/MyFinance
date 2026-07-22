package com.truckerload.utils

import androidx.core.content.edit
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.truckerload.R
import com.truckerload.data.backup.BackupData
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.toEntity
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.data.repository.PhotoRepository
import com.truckerload.data.repository.ScanRepository
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
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

object BackupService {

    private const val PREFS = "backup_companion"
    private const val AUTO_BACKUP_SUBDIR = "backups/auto"
    private const val DEFAULT_KEEP_COUNT = 5
    private const val TAG = "BackupRestore"
    private const val AUTO_BACKUP_DEBOUNCE_MS = 45_000L

    private val gson: Gson = GsonBuilder().create()
    private val autoBackupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val autoBackupTimestampRef = AtomicReference(SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US))
    private val autoBackupMutex = Mutex()
    @Volatile private var pendingAutoBackupJob: Job? = null

    data class CreateResult(
        val save: StorageHelper.SaveResult,
        val loadCount: Int,
        val visibleText: String
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
        val loadRepository = LoadRepository(db)
        val paycheckRepository = PaycheckRepository(db)
        val dieselRepository = DieselRepository(db)

        val loads = loadRepository.getAllLoadsOnce()
        if (loads.isEmpty() &&
            paycheckRepository.getAllPaychecksOnce().isEmpty() &&
            dieselRepository.getAllDieselOnce().isEmpty()
        ) {
            return@withContext
        }

        val backup = BackupData(
            loads = loads,
            paychecks = paycheckRepository.getAllPaychecksOnce(),
            diesel = dieselRepository.getAllDieselOnce()
        )
        val json = gson.toJson(backup)
        val dir = autoBackupDir(appContext).apply { mkdirs() }
        val fileName = "auto_backup_${formatAutoBackupTimestamp()}.tlb"
        File(dir, fileName).writeText(json, Charsets.UTF_8)
        pruneAutoBackups(appContext, DEFAULT_KEEP_COUNT)
        Log.d(TAG, "createAutoBackup saved $fileName (${loads.size} loads)")
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

    /** Полный JSON бэкапа (loads/paychecks/diesel) для локального файла или Google Drive. */
    suspend fun createBackupJson(context: Context): String? = withContext(Dispatchers.IO) {
        runCatching {
            val appContext = context.applicationContext
            val db = AppDatabase.getInstanceForActiveUser(appContext) ?: return@withContext null
            val loadRepository = LoadRepository(db)
            val paycheckRepository = PaycheckRepository(db)
            val dieselRepository = DieselRepository(db)
            val loads = loadRepository.getAllLoadsOnce()
            val paychecks = paycheckRepository.getAllPaychecksOnce()
            val diesel = dieselRepository.getAllDieselOnce()
            if (loads.isEmpty() && paychecks.isEmpty() && diesel.isEmpty()) return@withContext null
            gson.toJson(
                BackupData(
                    loads = loads,
                    paychecks = paychecks,
                    diesel = diesel,
                )
            )
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
            val loadRepository = LoadRepository(db)
            val paycheckRepository = PaycheckRepository(db)
            val dieselRepository = DieselRepository(db)

            val loads = loadRepository.getAllLoadsOnce()
            val backup = BackupData(
                loads = loads,
                paychecks = paycheckRepository.getAllPaychecksOnce(),
                diesel = dieselRepository.getAllDieselOnce()
            )
            val json = gson.toJson(backup)
            val note = BackupNoteFormatter.buildNote(backup)
            val fileName = BackupNoteFormatter.noteFileName(backup.exportedAt)
            val textBytes = note.visibleText.toByteArray(StandardCharsets.UTF_8)
            val storageHelper = StorageHelper(appContext)

            val saveResult = storageHelper.saveToPublicDownloads(fileName, BrandConstants.DOWNLOADS_FOLDER, "text/plain") { out ->
                out.write(textBytes)
            } ?: run {
                val file = storageHelper.saveToAppStorage(fileName, "backups") { out ->
                    out.write(textBytes)
                }
                StorageHelper.SaveResult(storageHelper.getShareableUri(file), "${BrandConstants.DOWNLOADS_FOLDER}/$fileName")
            }

            saveCompanionBackup(appContext, fileName, json)
            CreateResult(
                save = saveResult,
                loadCount = note.loadCount,
                visibleText = note.visibleText
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "createManualBackup failed", e)
            null
        }
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
                ?: return@withContext Result.failure(
                    IllegalStateException(appContext.getString(R.string.backup_restore_read_failed))
                )

            val json = BackupNoteFormatter.extractBackupJson(bytes)
                ?: return@withContext Result.failure(
                    IllegalStateException(appContext.getString(R.string.backup_restore_bad_format))
                )

            restoreFromJson(appContext, json).map { backup ->
                appContext.getString(
                    R.string.backup_restore_success,
                    backup.loads.size,
                    backup.paychecks.size,
                    backup.diesel.size
                )
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "restoreFromUri failed", e)
            Result.failure(e)
        }
    }

    suspend fun restoreLatestCompanionBackupIfEmpty(context: Context): Result<String>? = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val db = AppDatabase.getInstanceForActiveUser(appContext) ?: return@withContext null
        val loadRepository = LoadRepository(db)
        if (loadRepository.getAllLoadsOnce().isNotEmpty()) return@withContext null

        val dir = File(appContext.getExternalFilesDir(null), "backups")
        if (!dir.isDirectory) return@withContext null

        val latest = dir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".tlb", ignoreCase = true) }
            ?.maxByOrNull { it.lastModified() }
            ?: return@withContext null

        android.util.Log.i(TAG, "Auto-restore from companion ${latest.name}")
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

    private fun autoBackupDir(context: Context): File =
        File(context.getExternalFilesDir(null), AUTO_BACKUP_SUBDIR)

    private fun backupSortKey(file: File): String {
        val name = file.name
        val stamp = name.removePrefix("auto_backup_").removeSuffix(".tlb")
        return stamp.ifBlank { file.lastModified().toString() }
    }

    private fun saveCompanionBackup(context: Context, txtFileName: String, json: String) {
        val dir = File(context.getExternalFilesDir(null), "backups").apply { mkdirs() }
        val companion = File(dir, BackupNoteFormatter.companionFileName(txtFileName))
        companion.writeText(json, Charsets.UTF_8)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit {
                putString(txtFileName, companion.absolutePath)
            }
    }

    private fun readCompanionJson(context: Context, txtFileName: String?): String? {
        if (txtFileName.isNullOrBlank()) return null
        val path = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(txtFileName, null)
            ?: return null
        val file = File(path)
        if (!file.exists()) return null
        return file.readText(Charsets.UTF_8).takeIf { it.startsWith("{") }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? =
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
        }

    private suspend fun restoreFromJson(context: Context, json: String): Result<BackupData> {
        val backup = gson.fromJson(json, BackupData::class.java)
            ?: return Result.failure(
                IllegalStateException(context.getString(R.string.backup_restore_bad_format))
            )

        val db = AppDatabase.getInstanceForActiveUser(context.applicationContext)
            ?: return Result.failure(IllegalStateException("No active user session"))
        val loadDao = db.loadDao()
        val stopDao = db.stopDao()
        val penaltyDao = db.penaltyDao()
        val paycheckDao = db.paycheckDao()
        val dieselDao = db.dieselDao()

        db.withTransaction {
            PhotoRepository(db).deleteAllPhotosAndFiles()
            ScanRepository(db).deleteAllScansAndFiles()
            dieselDao.deleteAll()
            paycheckDao.deleteAll()
            loadDao.deleteAll()

            backup.loads.forEach { load ->
                loadDao.insert(load.toEntity())
                if (load.stops.isNotEmpty()) stopDao.insertAll(load.stops.map { it.toEntity(load.id) })
                if (load.penalties.isNotEmpty()) penaltyDao.insertAll(load.penalties.map { it.toEntity(load.id) })
            }
            if (backup.paychecks.isNotEmpty()) paycheckDao.insertAll(backup.paychecks.map { it.toEntity() })
            if (backup.diesel.isNotEmpty()) dieselDao.insertAll(backup.diesel.map { it.toEntity() })
        }

        return Result.success(backup)
    }

    private fun formatAutoBackupTimestamp(): String =
        synchronized(autoBackupTimestampRef) {
            autoBackupTimestampRef.get().format(Date())
        }
}
