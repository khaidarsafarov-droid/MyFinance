package com.truckerload.data.repository

import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.ScanEntity
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.UUID

class ScanRepository(private val db: AppDatabase) {

    private val scanDao = db.scanDao()

    fun watchScans(): Flow<List<ScanEntity>> = scanDao.getAllScans()

    fun watchScansByLoadId(loadId: String): Flow<List<ScanEntity>> =
        scanDao.getScansByLoadId(loadId)

    suspend fun saveScan(
        fileName: String,
        filePath: String,
        timestamp: Long,
        fileSizeBytes: Long,
        pageCount: Int,
        ocrText: String,
        loadId: String? = null,
    ): ScanEntity {
        val entity = ScanEntity(
            id = UUID.randomUUID().toString(),
            fileName = fileName,
            filePath = filePath,
            timestamp = timestamp,
            fileSizeBytes = fileSizeBytes,
            pageCount = pageCount,
            ocrText = ocrText,
            loadId = loadId,
        )
        scanDao.insert(entity)
        return entity
    }

    suspend fun deleteScan(id: String) {
        val existing = scanDao.getById(id)
        scanDao.deleteById(id)
        existing?.filePath?.takeIf { it.isNotBlank() }?.let { path ->
            runCatching { File(path).delete() }
        }
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
}
