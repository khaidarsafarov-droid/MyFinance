package com.truckerload.data.local

import com.truckerload.data.local.entities.MaintenanceArchiveEntity
import com.truckerload.data.local.entities.MaintenanceTaskEntity
import com.truckerload.domain.model.MaintenanceArchiveEntry
import com.truckerload.domain.model.MaintenanceReminderType
import com.truckerload.domain.model.MaintenanceTask

fun MaintenanceTaskEntity.toDomain(): MaintenanceTask =
    MaintenanceTask(
        id = id,
        title = title,
        startDate = startDate,
        reminderType = runCatching { MaintenanceReminderType.valueOf(reminderType) }
            .getOrDefault(MaintenanceReminderType.MILES),
        intervalMiles = intervalMiles,
        odometerAtStart = odometerAtStart,
        dueDate = dueDate,
        isCompleted = isCompleted,
        completedAt = completedAt,
        notifiedAt = notifiedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun MaintenanceTask.toEntity(): MaintenanceTaskEntity =
    MaintenanceTaskEntity(
        id = id,
        title = title,
        startDate = startDate,
        reminderType = reminderType.name,
        intervalMiles = intervalMiles,
        odometerAtStart = odometerAtStart,
        dueDate = dueDate,
        isCompleted = isCompleted,
        completedAt = completedAt,
        notifiedAt = notifiedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun MaintenanceArchiveEntity.toDomain(): MaintenanceArchiveEntry =
    MaintenanceArchiveEntry(
        id = id,
        serviceDate = serviceDate,
        description = description,
        amount = amount,
        photoPath = photoPath,
        ocrText = ocrText,
        createdAt = createdAt,
    )

fun MaintenanceArchiveEntry.toEntity(): MaintenanceArchiveEntity =
    MaintenanceArchiveEntity(
        id = id,
        serviceDate = serviceDate,
        description = description,
        amount = amount,
        photoPath = photoPath,
        ocrText = ocrText,
        createdAt = createdAt,
    )
