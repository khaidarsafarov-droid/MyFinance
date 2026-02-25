package com.truckerload.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diesel")
data class DieselEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val weekNumber: Int,
    val year: Int,
    val weekLabel: String,
    val weekStartDate: String,
    val weekEndDate: String,
    val totalAmount: Double,
    val gallons: Double?,
    val pricePerGallon: Double?,
    val location: String?,
    val rawExtractedText: String,
    val sourceFileName: String?,
    val addedAt: Long
)
