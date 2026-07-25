package com.truckerload.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scans",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["loadId"]),
        Index(value = ["cloudSyncStatus"]),
    ],
)
data class ScanEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val filePath: String,
    val timestamp: Long,
    val fileSizeBytes: Long,
    val pageCount: Int,
    val ocrText: String = "",
    val loadId: String? = null,
    val cloudMediaId: String? = null,
    val cloudSyncStatus: String = CLOUD_LOCAL,
    val cloudUpdatedAt: Long = 0,
) {
    companion object {
        const val CLOUD_LOCAL = "LOCAL"
        const val CLOUD_PENDING = "PENDING"
        const val CLOUD_SYNCED = "SYNCED"
        const val CLOUD_FAILED = "FAILED"
    }
}
