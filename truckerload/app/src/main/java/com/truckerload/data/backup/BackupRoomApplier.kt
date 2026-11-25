package com.truckerload.data.backup

import androidx.room.withTransaction
import com.truckerload.data.backup.BackupRoomApplier.applyFullReplace
import com.truckerload.data.backup.BackupRoomApplier.pruneOrphanMedia
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.toEntity
import com.truckerload.data.repository.LoadRepository

/**
 * Shared full-replace restore used by local backup import and cloud full hydration.
 *
 * Photos/scans are **not** wiped wholesale: after journal replace, [pruneOrphanMedia] unlinks
 * (never deletes) rows whose [loadId] no longer exists. Unlinked media and media for restored loads stay.
 * Maintenance receipt [photoPath] files are kept when the archive row is restored.
 *
 * Schema v1 backups never carried ТО — local maintenance tables are left untouched on restore.
 */
object BackupRoomApplier {

    suspend fun applyFullReplace(db: AppDatabase, backup: BackupData) {
        val loadDao = db.loadDao()
        val stopDao = db.stopDao()
        val penaltyDao = db.penaltyDao()
        val paycheckDao = db.paycheckDao()
        val dieselDao = db.dieselDao()
        val maintenanceDao = db.maintenanceDao()
        val replaceMaintenance = carriesMaintenance(backup)

        db.withTransaction {
            dieselDao.deleteAll()
            paycheckDao.deleteAll()
            val existingLoadIds = loadDao.getAllLoadsOnce().map { it.id }.toSet()
            val newLoadIds = backup.loads.map { it.id }.toSet()
            // FIX: preserve audit history for loads that survive restore; drop only removed ids
            val removedLoadIds = existingLoadIds - newLoadIds
            removedLoadIds.forEach { db.loadHistoryDao().deleteByLoadId(it) }
            loadDao.deleteAll()
            if (replaceMaintenance) {
                maintenanceDao.deleteAllTasks()
                maintenanceDao.deleteAllArchive()
            }

            backup.loads.forEach { load ->
                loadDao.insert(load.toEntity())
                if (load.stops.isNotEmpty()) {
                    stopDao.insertAll(load.stops.map { it.toEntity(load.id) })
                }
                if (load.penalties.isNotEmpty()) {
                    penaltyDao.insertAll(load.penalties.map { it.toEntity(load.id) })
                }
            }
            if (backup.paychecks.isNotEmpty()) {
                paycheckDao.insertAll(backup.paychecks.map { it.toEntity() })
            }
            if (backup.diesel.isNotEmpty()) {
                dieselDao.insertAll(backup.diesel.map { it.toEntity() })
            }
            if (replaceMaintenance) {
                applyMaintenanceInsideTransaction(db, backup)
            }
            applyExtrasInsideTransaction(db, backup, newLoadIds)
        }
    }

    /**
     * Tables that older files did not carry. A null list means "not in this file": the local
     * rows stay. Photo and scan records are merged, never wiped, and a record that points at a
     * load the file does not restore is kept unlinked instead of dropped.
     */
    private suspend fun applyExtrasInsideTransaction(
        db: AppDatabase,
        backup: BackupData,
        restoredLoadIds: Set<String>,
    ) {
        backup.miscExpenses?.let { rows ->
            db.miscExpenseDao().deleteAll()
            rows.forEach { db.miscExpenseDao().upsert(it.toEntity()) }
        }
        backup.perDiemOverrides?.let { rows ->
            db.perDiemDayOverrideDao().deleteAll()
            rows.forEach { db.perDiemDayOverrideDao().upsert(it.toEntity()) }
        }
        backup.photos?.forEach { photo ->
            if (photo.id.isBlank()) return@forEach
            val existing = db.photoDao().getById(photo.id)
            if (existing != null) {
                // Keep the row; only repair a path whose file is gone when the backup brought one.
                if (repairsPath(existing.filePath, photo.filePath)) {
                    db.photoDao().insert(existing.copy(fileName = photo.fileName, filePath = photo.filePath))
                }
                return@forEach
            }
            val link = photo.loadId?.takeIf { it in restoredLoadIds }
            db.photoDao().insert(photo.toEntity(link))
        }
        backup.scans?.forEach { scan ->
            if (scan.id.isBlank()) return@forEach
            val existing = db.scanDao().getById(scan.id)
            if (existing != null) {
                if (repairsPath(existing.filePath, scan.filePath)) {
                    db.scanDao().insert(existing.copy(fileName = scan.fileName, filePath = scan.filePath))
                }
                return@forEach
            }
            val link = scan.loadId?.takeIf { it in restoredLoadIds }
            db.scanDao().insert(scan.toEntity(link))
        }
    }

    private fun repairsPath(localPath: String, restoredPath: String): Boolean =
        restoredPath != localPath && !java.io.File(localPath).isFile && java.io.File(restoredPath).isFile

    /** Replace ТО tables from backup (also used on incremental cloud pull). */
    suspend fun applyMaintenanceReplace(db: AppDatabase, backup: BackupData) {
        db.withTransaction {
            db.maintenanceDao().deleteAllTasks()
            db.maintenanceDao().deleteAllArchive()
            applyMaintenanceInsideTransaction(db, backup)
        }
    }

    /** True when the backup format includes maintenance (schema ≥ v2). */
    fun carriesMaintenance(backup: BackupData): Boolean =
        BackupDataCodec.resolveSchemaVersion(backup) >= BackupSchema.V2

    private suspend fun applyMaintenanceInsideTransaction(db: AppDatabase, backup: BackupData) {
        val maintenanceDao = db.maintenanceDao()
        if (backup.maintenanceTasks.isNotEmpty()) {
            maintenanceDao.insertTasks(backup.maintenanceTasks.map { it.toDomain().toEntity() })
        }
        if (backup.maintenanceArchive.isNotEmpty()) {
            maintenanceDao.insertArchives(backup.maintenanceArchive.map {
                it.toDomain().toEntity()
            })
        }
    }

    /** Unlinks photo/scan rows tied to load ids removed by [applyFullReplace]. Rows and files stay. */
    suspend fun pruneOrphanMedia(db: AppDatabase): Int =
        LoadRepository(db).cleanupOrphanAttachments()
}
