package com.truckerload.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "paychecks",
    indices = [Index(value = ["weekNumber", "year"]), Index(value = ["addedAt"])],
)
data class PaycheckEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val weekNumber: Int,
    val year: Int,
    val weekLabel: String,
    val weekStartDate: String,
    val weekEndDate: String,
    val driverName: String?,
    val grossAmount: Double?,
    val netAmount: Double,
    val rawExtractedText: String,
    val sourceFileName: String?,
    val addedAt: Long,
    val sourceFilePath: String? = null,
)
