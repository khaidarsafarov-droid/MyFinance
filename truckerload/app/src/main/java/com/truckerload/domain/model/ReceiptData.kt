package com.truckerload.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Parsed / editable receipt from a service invoice photo (OCR + manual corrections).
 * Persisted as [MaintenanceArchiveEntry] with [imageUri] stored under app filesDir.
 */
data class ReceiptData(
    val id: Long = 0,
    val imageUri: String = "",
    val serviceName: String = "",
    val date: Long = System.currentTimeMillis(),
    val totalAmount: Double = 0.0,
    val description: String = "",
    val rawText: String? = null,
) {
    fun toArchiveEntry(): MaintenanceArchiveEntry =
        MaintenanceArchiveEntry(
            id = id,
            serviceName = serviceName.trim(),
            serviceDate = epochMillisToIsoDate(date),
            description = description.trim().ifBlank { serviceName.trim() },
            amount = totalAmount,
            photoPath = imageUri.takeIf { it.isNotBlank() },
            ocrText = rawText,
        )

    companion object {
        fun fromArchive(entry: MaintenanceArchiveEntry): ReceiptData =
            ReceiptData(
                id = entry.id,
                imageUri = entry.photoPath.orEmpty(),
                serviceName = entry.serviceName,
                date = isoDateToEpochMillis(entry.serviceDate),
                totalAmount = entry.amount,
                description = entry.description,
                rawText = entry.ocrText,
            )

        fun epochMillisToIsoDate(millis: Long): String =
            Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(DateTimeFormatter.ISO_LOCAL_DATE)

        fun isoDateToEpochMillis(iso: String): Long =
            runCatching {
                LocalDate.parse(iso)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            }.getOrElse { System.currentTimeMillis() }
    }
}
