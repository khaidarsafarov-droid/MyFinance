package com.truckerload.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "misc_expenses",
    indices = [Index(value = ["date"])],
)
data class MiscExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val description: String,
    val date: String,
    /** Absolute path to an attached receipt photo in app files (optional). */
    val receiptPhotoPath: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
