package com.truckerload.domain.model

enum class MaintenanceReminderType {
    MILES,
    DATE,
}

data class MaintenanceTask(
    val id: Long = 0,
    val title: String,
    val startDate: String,
    val reminderType: MaintenanceReminderType,
    val intervalMiles: Double? = null,
    val odometerAtStart: Double? = null,
    val dueDate: String? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val notifiedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

data class MaintenanceArchiveEntry(
    val id: Long = 0,
    val serviceName: String = "",
    val serviceDate: String,
    val description: String,
    val amount: Double,
    val photoPath: String? = null,
    val ocrText: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun toReceiptData(): ReceiptData = ReceiptData.fromArchive(this)
}

/** Progress snapshot for a miles-based or date-based ТО task. */
data class MaintenanceProgress(
    val task: MaintenanceTask,
    val milesDrivenSinceStart: Double,
    val estimatedOdometer: Double?,
    val targetOdometer: Double?,
    val milesRemaining: Double?,
    val daysRemaining: Long?,
    val isDue: Boolean,
    val loadsCounted: Int = 0,
    val progressFraction: Float = 0f,
)
