package com.truckerload.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photos",
    indices = [Index(value = ["timestamp"]), Index(value = ["loadId"])],
)
data class PhotoEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val filePath: String,
    val latitude: Double,
    val longitude: Double,
    val city: String,
    val state: String,
    val zipCode: String,
    val timestamp: Long,
    val loadId: String? = null,
)
