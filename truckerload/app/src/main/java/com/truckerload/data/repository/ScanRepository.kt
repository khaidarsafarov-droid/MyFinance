package com.truckerload.data.repository

import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.ScanEntity
import com.truckerload.domain.model.ScanDocumentCategory
import com.truckerload.domain.model.ScanDocumentFinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.util.UUID

class ScanRepository(
    private val db: AppDatabase,
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
            cloudSyncStatus = ScanEntity.CLOUD_LOCAL,
        )
        scanDao.insert(entity)
        return entity
    }

    suspend fun linkScanToLoad(scanId: String, loadId: String?) {
        val existing = scanDao.getById(scanId) ?: return
        val updated = existing.copy(
            loadId = loadId,
            category = if (!loadId.isNullOrBlank()) {
                ScanDocumentCategory.LOAD.name
            } else {
                existing.category
            },
        )
        scanDao.insert(updated)
    }

    suspend fun updateScanCategory(scanId: String, category: ScanDocumentCategory) {
        val existing = scanDao.getById(scanId) ?: return
        scanDao.insert(existing.copy(category = category.name))
    }

    suspend fun deleteScan(id: String) {
        val existing = scanDao.getById(id)
        scanDao.deleteById(id)
        existing?.filePath?.takeIf { it.isNotBlank() }?.let { path ->
            runCatching { File(path).delete() }
        }
    }

    /**
     * Deletes only stale temp leftovers (`*.partial`, `*.tmp`) under scans/.
     * Real documents are never deleted here: the folder is shared by every account on the
     * device, and a restore or migration may have placed a file before its row exists.
     */
    suspend fun cleanupOrphanScanFiles(scansDir: File, now: Long = System.currentTimeMillis()): Int {
        if (!scansDir.isDirectory) return 0
        var removed = 0
        scansDir.listFiles()?.forEach { file ->
            val temp = file.name.endsWith(".partial") || file.name.endsWith(".tmp")
            if (file.isFile && temp && now - file.lastModified() > STALE_TEMP_MS) {
                if (file.delete()) removed++
            }
        }
        return removed
    }

    suspend fun deleteScansForLoad(loadId: String) {
        val scans = scanDao.getScansByLoadIdOnce(loadId)
        scanDao.deleteByLoadId(loadId)
        scans.forEach { scan ->
            runCatching { File(scan.filePath).delete() }
        }
    }

    suspend fun deleteAllScansAndFiles() {
        val all = scanDao.getAllScansOnce()
        scanDao.deleteAll()
        all.forEach { scan ->
            runCatching { File(scan.filePath).delete() }
        }
    }

    private companion object {
        const val STALE_TEMP_MS = 24 * 60 * 60 * 1000L
    }
}
