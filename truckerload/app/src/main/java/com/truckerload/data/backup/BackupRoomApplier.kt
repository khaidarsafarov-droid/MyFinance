package com.truckerload.data.backup

import androidx.room.withTransaction
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.toEntity
import com.truckerload.data.repository.LoadRepository

/**
 * Shared full-replace journal restore used by local backup import and cloud full hydration.
 *
 * Photos/scans are **not** wiped wholesale: after journal replace, [pruneOrphanMedia] removes
 * only rows whose [loadId] no longer exists. Unlinked media and media for restored loads stay.
 */
object BackupRoomApplier {

    suspend fun applyFullReplace(db: AppDatabase, backup: BackupData) {
        val loadDao = db.loadDao()
        val stopDao = db.stopDao()
        val penaltyDao = db.penaltyDao()
        val paycheckDao = db.paycheckDao()
        val dieselDao = db.dieselDao()

        db.withTransaction {
            dieselDao.deleteAll()
            paycheckDao.deleteAll()
            db.loadHistoryDao().deleteAll()
            loadDao.deleteAll()

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
        }
    }

    /** Drops photo/scan rows (and files) tied to load ids removed by [applyFullReplace]. */
    suspend fun pruneOrphanMedia(db: AppDatabase): Int =
        LoadRepository(db).cleanupOrphanAttachments()
}
