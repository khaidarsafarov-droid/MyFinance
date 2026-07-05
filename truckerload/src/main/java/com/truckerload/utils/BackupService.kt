package com.truckerload.utils

import android.content.Context
import com.truckerload.R
import com.truckerload.data.backup.BackupData
import com.truckerload.data.local.toEntity
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.truckerload.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

/**
 * Создание и восстановление резервной копии БД в JSON.
 */
class BackupService(
    private val context: Context,
    private val loadRepository: LoadRepository,
    private val paycheckRepository: PaycheckRepository,
    private val dieselRepository: DieselRepository
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val db = AppDatabase.getInstance(context)
    private val loadDao = db.loadDao()
    private val stopDao = db.stopDao()
    private val penaltyDao = db.penaltyDao()
    private val paycheckDao = db.paycheckDao()
    private val dieselDao = db.dieselDao()

    suspend fun createBackup(): StorageHelper.SaveResult? = withContext(Dispatchers.IO) {
        try {
            val loads = loadRepository.getAllLoads().first().mapNotNull { load ->
                loadRepository.getLoadById(load.id)
            }
            val paychecks = paycheckRepository.getAllPaychecksOnce()
            val diesel = dieselRepository.getAllDieselOnce()
            val backup = BackupData(
                loads = loads,
                paychecks = paychecks,
                diesel = diesel
            )
            val json = gson.toJson(backup)
            val fileName = "TruckerLoad_Backup_${System.currentTimeMillis()}.json"
            val storageHelper = StorageHelper(context)
            storageHelper.saveToPublicDownloads(fileName, "TruckerLoad", "application/json") { out ->
                OutputStreamWriter(out, StandardCharsets.UTF_8).use { it.write(json) }
            }
                ?: run {
                    val file = storageHelper.saveToAppStorage(fileName, "backups") { out ->
                        OutputStreamWriter(out, StandardCharsets.UTF_8).use { it.write(json) }
                    }
                    StorageHelper.SaveResult(storageHelper.getShareableUri(file), "TruckerLoad/$fileName")
                }
        } catch (e: Exception) {
            android.util.Log.e("BackupService", "createBackup failed", e)
            null
        }
    }

    suspend fun restoreFromUri(uri: android.net.Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.use { inp ->
                InputStreamReader(inp, StandardCharsets.UTF_8).readText()
            } ?: return@withContext Result.failure(
                IllegalStateException(context.getString(R.string.backup_restore_read_failed))
            )
            val backup = gson.fromJson(json, BackupData::class.java)
                ?: return@withContext Result.failure(
                    IllegalStateException(context.getString(R.string.backup_restore_bad_format))
                )

            // Очистка: diesel, paychecks, loads (cascade удалит stops, penalties)
            dieselDao.deleteAll()
            paycheckDao.deleteAll()
            loadDao.deleteAll()

            // Вставка в обратном порядке зависимостей: loads -> stops, penalties; paychecks; diesel
            backup.loads.forEach { load ->
                loadDao.insert(load.toEntity())
                if (load.stops.isNotEmpty()) stopDao.insertAll(load.stops.map { it.toEntity(load.id) })
                if (load.penalties.isNotEmpty()) penaltyDao.insertAll(load.penalties.map { it.toEntity(load.id) })
            }
            if (backup.paychecks.isNotEmpty()) paycheckDao.insertAll(backup.paychecks.map { it.toEntity() })
            if (backup.diesel.isNotEmpty()) dieselDao.insertAll(backup.diesel.map { it.toEntity() })

            Result.success(
                context.getString(
                    R.string.backup_restore_success,
                    backup.loads.size,
                    backup.paychecks.size,
                    backup.diesel.size
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("BackupService", "restoreFromUri failed", e)
            Result.failure(e)
        }
    }
}
