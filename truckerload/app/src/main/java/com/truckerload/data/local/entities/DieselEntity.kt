package com.truckerload.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "diesel",
    indices = [Index(value = ["weekNumber", "year"]), Index(value = ["addedAt"])],
)
data class DieselEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val weekNumber: Int,
    val year: Int,
    val weekLabel: String,
    val weekStartDate: String,
    val weekEndDate: String,
    /** Paid amount in cents. */
    val totalAmount: Long,
    val gallons: Double?,
    /** List price in thousandths of a dollar per gallon. */
    val pricePerGallon: Long?,
    /** Discounted price in thousandths of a dollar per gallon. */
    val discountPricePerGallon: Long? = null,
    val location: String?,
    val rawExtractedText: String,
    val sourceFileName: String?,
    val addedAt: Long
)
