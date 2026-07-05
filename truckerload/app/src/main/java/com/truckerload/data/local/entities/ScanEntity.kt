package com.truckerload.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scans",
    indices = [Index(value = ["timestamp"])],
)
data class ScanEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val filePath: String,
    val timestamp: Long,
    val fileSizeBytes: Long,
    val pageCount: Int,
    val ocrText: String = "",
)
