package com.truckerload.data.repository

import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.ScanEntity
import kotlinx.coroutines.flow.Flow
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
        scanDao.deleteById(id)
    }
}
