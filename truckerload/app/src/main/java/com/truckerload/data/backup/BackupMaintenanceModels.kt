package com.truckerload.data.backup

import androidx.annotation.Keep
import com.truckerload.domain.model.MaintenanceArchiveEntry
import com.truckerload.domain.model.MaintenanceReminderType
import com.truckerload.domain.model.MaintenanceTask

@Keep
data class BackupMaintenanceTask(
    val id: Long = 0,
    val title: String = "",
    val startDate: String = "",
    val reminderType: String = MaintenanceReminderType.DATE.name,
    val intervalMiles: Double? = null,
    val odometerAtStart: Double? = null,
    val dueDate: String? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val notifiedAt: Long? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
) {
    fun toDomain(): MaintenanceTask = MaintenanceTask(
        id = id,
        title = title,
        startDate = startDate,
        reminderType = runCatching { MaintenanceReminderType.valueOf(reminderType) }
            .getOrDefault(MaintenanceReminderType.DATE),
        intervalMiles = intervalMiles,
        odometerAtStart = odometerAtStart,
        dueDate = dueDate,
        isCompleted = isCompleted,
        completedAt = completedAt,
        notifiedAt = notifiedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

@Keep
data class BackupMaintenanceArchive(
    val id: Long = 0,
    val serviceName: String = "",
    val serviceDate: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val photoPath: String? = null,
    val ocrText: String? = null,
    val createdAt: Long = 0,
) {
    fun toDomain(): MaintenanceArchiveEntry = MaintenanceArchiveEntry(
        id = id,
        serviceName = serviceName,
        serviceDate = serviceDate,
        description = description,
        amount = amount,
        photoPath = photoPath,
        ocrText = ocrText,
        createdAt = createdAt,
    )
}
