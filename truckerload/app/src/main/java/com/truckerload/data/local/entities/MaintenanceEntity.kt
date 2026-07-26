package com.truckerload.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "maintenance_tasks",
    indices = [
        Index(value = ["isCompleted"]),
        Index(value = ["startDate"]),
        Index(value = ["dueDate"]),
    ],
)
data class MaintenanceTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startDate: String,
    val reminderType: String,
    val intervalMiles: Double?,
    val odometerAtStart: Double?,
    val dueDate: String?,
    val isCompleted: Boolean,
    val completedAt: Long?,
    val notifiedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "maintenance_archive",
    indices = [
        Index(value = ["serviceDate"]),
        Index(value = ["createdAt"]),
    ],
)
data class MaintenanceArchiveEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serviceName: String = "",
    val serviceDate: String,
    val description: String,
    val amount: Double,
    val photoPath: String?,
    val ocrText: String?,
    val createdAt: Long,
)
