package com.truckerload.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "stops",
    foreignKeys = [ForeignKey(
        entity = LoadEntity::class,
        parentColumns = ["id"],
        childColumns = ["loadId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class StopEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val loadId: String,
    val stopNumber: Int,
    val type: String,
    val puNumber: String?,
    val note: String?,
    val scheduledTime: String,
    val timezone: String,
    val facilityCode: String?,
    val fullAddress: String,
    val city: String,
    val state: String,
    val zip: String
)
