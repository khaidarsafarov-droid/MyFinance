package com.truckerload.data.repository

import androidx.room.withTransaction
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.ScanEntity
import com.truckerload.data.sync.MediaSyncEnqueuer
import com.truckerload.domain.model.ScanDocumentCategory
import com.truckerload.domain.model.ScanDocumentFinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.util.UUID

class ScanRepository(
    private val db: AppDatabase,
    private val mediaSync: MediaSyncEnqueuer = MediaSyncEnqueuer.forDatabase(db),
) {

    private val scanDao = db.scanDao()

    fun watchScans(): Flow<List<ScanEntity>> =
        scanDao.getAllScans().flowOn(Dispatchers.IO)

    fun watchScansByLoadId(loadId: String): Flow<List<ScanEntity>> =
        scanDao.getScansByLoadId(loadId).flowOn(Dispatchers.IO)

    suspend fun saveScan(
        fileName: String,
        filePath: String,
        timestamp: Long,
        fileSizeBytes: Long,
        pageCount: Int,
        ocrText: String,
        loadId: String? = null,
        category: String? = null,
    ): ScanEntity {
        val syncEnabled = mediaSync.enabled()
        val resolvedCategory = category?.let { ScanDocumentCategory.fromStored(it).name }
            ?: ScanDocumentFinder.infer(loadId, fileName, ocrText).name
        val entity = ScanEntity(
            id = UUID.randomUUID().toString(),
            fileName = fileName,
            filePath = filePath,
            timestamp = timestamp,
            fileSizeBytes = fileSizeBytes,
            pageCount = pageCount,
            ocrText = ocrText,
            loadId = loadId,
            category = resolvedCategory,
            cloudSyncStatus = if (syncEnabled) ScanEntity.CLOUD_PENDING else ScanEntity.CLOUD_LOCAL,
        )
        db.withTransaction {
            scanDao.insert(entity)
            if (syncEnabled) mediaSync.enqueueScanUpsert(entity)
        }
        if (syncEnabled) mediaSync.schedule()
        return entity
    }

    suspend fun linkScanToLoad(scanId: String, loadId: String?) {
        val existing = scanDao.getById(scanId) ?: return
        val syncEnabled = mediaSync.enabled()
        val updated = existing.copy(
            loadId = loadId,
            category = if (!loadId.isNullOrBlank()) {
                ScanDocumentCategory.LOAD.name
            } else {
                existing.category
            },
            cloudSyncStatus = if (syncEnabled) ScanEntity.CLOUD_PENDING else existing.cloudSyncStatus,
        )
        db.withTransaction {
            scanDao.insert(updated)
            if (syncEnabled) mediaSync.enqueueScanUpsert(updated)
        }
        if (syncEnabled) mediaSync.schedule()
    }

    suspend fun updateScanCategory(scanId: String, category: ScanDocumentCategory) {
        val existing = scanDao.getById(scanId) ?: return
        val syncEnabled = mediaSync.enabled()
        val updated = existing.copy(
            category = category.name,
            cloudSyncStatus = if (syncEnabled) ScanEntity.CLOUD_PENDING else existing.cloudSyncStatus,
        )
        db.withTransaction {
            scanDao.insert(updated)
            if (syncEnabled) mediaSync.enqueueScanUpsert(updated)
        }
        if (syncEnabled) mediaSync.schedule()
    }

    suspend fun deleteScan(id: String) {
        val existing = scanDao.getById(id)
        val syncEnabled = existing != null && mediaSync.enabled()
        db.withTransaction {
            if (syncEnabled) mediaSync.enqueueScanDelete(requireNotNull(existing))
            scanDao.deleteById(id)
        }
        existing?.filePath?.takeIf { it.isNotBlank() }?.let { path ->
            runCatching { File(path).delete() }
        }
        if (syncEnabled) mediaSync.schedule()
    }

    /** Deletes PDF files under scans/ that are not referenced by any DB row. */
    suspend fun cleanupOrphanScanFiles(scansDir: File): Int {
        if (!scansDir.isDirectory) return 0
        val known = scanDao.getAllScansOnce().map { it.filePath }.toHashSet()
        var removed = 0
        scansDir.listFiles()?.forEach { file ->
            if (file.isFile && file.absolutePath !in known) {
                if (file.delete()) removed++
            }
        }
        return removed
    }

    suspend fun deleteScansForLoad(loadId: String) {
        val scans = scanDao.getScansByLoadIdOnce(loadId)
        val syncEnabled = scans.isNotEmpty() && mediaSync.enabled()
        db.withTransaction {
            if (syncEnabled) scans.forEach { mediaSync.enqueueScanDelete(it) }
            scanDao.deleteByLoadId(loadId)
        }
        scans.forEach { scan ->
            runCatching { File(scan.filePath).delete() }
        }
        if (syncEnabled) mediaSync.schedule()
    }

    suspend fun deleteAllScansAndFiles() {
        val all = scanDao.getAllScansOnce()
        val syncEnabled = all.isNotEmpty() && mediaSync.enabled()
        db.withTransaction {
            if (syncEnabled) all.forEach { mediaSync.enqueueScanDelete(it) }
            scanDao.deleteAll()
        }
        all.forEach { scan ->
            runCatching { File(scan.filePath).delete() }
        }
        if (syncEnabled) mediaSync.schedule()
    }
}
