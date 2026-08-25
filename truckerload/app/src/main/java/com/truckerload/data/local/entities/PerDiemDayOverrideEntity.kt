package com.truckerload.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Manual include/exclude of a calendar day in the per-diem estimate. */
@Entity(tableName = "per_diem_day_overrides")
data class PerDiemDayOverrideEntity(
    @PrimaryKey val date: String,
    val included: Boolean,
)
