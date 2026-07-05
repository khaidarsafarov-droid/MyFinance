package com.truckerload.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "load_history",
    indices = [Index(value = ["loadId"])]
)
data class LoadHistory(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val loadId: String,
    val field: String,
    val oldValue: String,
    val newValue: String,
    val timestamp: Long,
)
