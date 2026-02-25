package com.truckerload.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "penalties",
    foreignKeys = [ForeignKey(
        entity = LoadEntity::class,
        parentColumns = ["id"],
        childColumns = ["loadId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class PenaltyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val loadId: String,
    val description: String,
    val amount: Double
)
