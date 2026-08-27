package com.truckerload.presentation.screens.maintenance

import com.truckerload.domain.model.MaintenanceArchiveEntry
import com.truckerload.domain.model.MaintenanceReminderType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class TaskDraft(
    val title: String = "",
    val startDate: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val reminderType: MaintenanceReminderType = MaintenanceReminderType.MILES,
    val intervalMiles: String = "",
    val odometerAtStart: String = "",
    val dueDate: String = LocalDate.now().plusMonths(1).format(DateTimeFormatter.ISO_LOCAL_DATE),
)

data class ArchiveLineDraft(
    val description: String = "",
    val amount: String = "",
) {
    fun parsedAmount(): Double? = amount.replace(',', '.').trim().toDoubleOrNull()
}

data class ArchiveDraft(
    val serviceName: String = "",
    val serviceDate: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val lines: List<ArchiveLineDraft> = listOf(ArchiveLineDraft()),
    val photoPath: String? = null,
    val ocrText: String? = null,
) {
    fun filledLines(): List<ArchiveLineDraft> =
        lines.filter { it.description.isNotBlank() || it.amount.isNotBlank() }

    fun lineTotal(): Double =
        filledLines().sumOf { it.parsedAmount()?.takeIf { amount -> amount >= 0 } ?: 0.0 }

    fun validationError(): String? {
        val filled = filledLines()
        if (filled.isEmpty()) return "empty_lines"
        filled.forEach { line ->
            if (line.description.isBlank()) return "empty_description"
            val amount = line.parsedAmount()
            if (amount == null || amount < 0) return "invalid_amount"
        }
        return null
    }

    fun toEntries(createdAt: Long = System.currentTimeMillis()): List<MaintenanceArchiveEntry> {
        if (validationError() != null) return emptyList()
        return filledLines().map { line ->
            MaintenanceArchiveEntry(
                serviceName = serviceName.trim(),
                serviceDate = serviceDate,
                description = line.description.trim(),
                amount = line.parsedAmount() ?: 0.0,
                photoPath = photoPath,
                ocrText = ocrText,
                createdAt = createdAt,
            )
        }
    }

    companion object {
        fun formatAmount(value: Double): String =
            if (value > 0) String.format(Locale.US, "%.2f", value) else ""
    }
}
