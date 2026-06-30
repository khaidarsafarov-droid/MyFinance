package com.truckerload.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "paychecks")
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
    val addedAt: Long
)
