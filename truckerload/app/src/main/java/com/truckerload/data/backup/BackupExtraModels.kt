package com.truckerload.data.backup

import androidx.annotation.Keep
import com.truckerload.data.local.entities.MiscExpenseEntity
import com.truckerload.data.local.entities.PerDiemDayOverrideEntity
import com.truckerload.data.local.entities.PhotoEntity
import com.truckerload.data.local.entities.ScanEntity

/**
 * Rows that older backup files did not carry: misc expenses, per-diem day overrides, and the
 * photo/scan records (GPS, date and time, city, linked load). The image and PDF bytes stay on
 * the phone. A field that is absent from the JSON means "this file does not carry it", and
 * restore leaves that table alone instead of wiping it.
 */
@Keep
data class BackupMiscExpense(
    val id: Int = 0,
    val amountCents: Long = 0,
    val description: String = "",
    val date: String = "",
    val receiptPhotoPath: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
) {
    fun toEntity() = MiscExpenseEntity(
        id = id,
        amount = amountCents,
        description = description,
        date = date,
        receiptPhotoPath = receiptPhotoPath,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        fun from(row: MiscExpenseEntity) = BackupMiscExpense(
            id = row.id,
            amountCents = row.amount,
            description = row.description,
            date = row.date,
            receiptPhotoPath = row.receiptPhotoPath,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
        )
    }
}

@Keep
data class BackupPerDiemOverride(
    val date: String = "",
    val included: Boolean = false,
) {
    fun toEntity() = PerDiemDayOverrideEntity(date = date, included = included)

    companion object {
        fun from(row: PerDiemDayOverrideEntity) = BackupPerDiemOverride(row.date, row.included)
    }
}

@Keep
data class BackupPhoto(
    val id: String = "",
    val fileName: String = "",
    val filePath: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
    val timestamp: Long = 0,
    val loadId: String? = null,
) {
    fun toEntity(loadIdOverride: String? = loadId) = PhotoEntity(
        id = id,
        fileName = fileName,
        filePath = filePath,
        latitude = latitude,
        longitude = longitude,
        city = city,
        state = state,
        zipCode = zipCode,
        timestamp = timestamp,
        loadId = loadIdOverride,
    )

    companion object {
        fun from(row: PhotoEntity) = BackupPhoto(
            id = row.id,
            fileName = row.fileName,
            filePath = row.filePath,
            latitude = row.latitude,
            longitude = row.longitude,
            city = row.city,
            state = row.state,
            zipCode = row.zipCode,
            timestamp = row.timestamp,
            loadId = row.loadId,
        )
    }
}

@Keep
data class BackupScan(
    val id: String = "",
    val fileName: String = "",
    val filePath: String = "",
    val timestamp: Long = 0,
    val fileSizeBytes: Long = 0,
    val pageCount: Int = 1,
    val ocrText: String = "",
    val loadId: String? = null,
    val category: String = "OTHER",
) {
    fun toEntity(loadIdOverride: String? = loadId) = ScanEntity(
        id = id,
        fileName = fileName,
        filePath = filePath,
        timestamp = timestamp,
        fileSizeBytes = fileSizeBytes,
        pageCount = pageCount,
        ocrText = ocrText,
        loadId = loadIdOverride,
        category = category,
    )

    companion object {
        fun from(row: ScanEntity) = BackupScan(
            id = row.id,
            fileName = row.fileName,
            filePath = row.filePath,
            timestamp = row.timestamp,
            fileSizeBytes = row.fileSizeBytes,
            pageCount = row.pageCount,
            ocrText = row.ocrText,
            loadId = row.loadId,
            category = row.category,
        )
    }
}
