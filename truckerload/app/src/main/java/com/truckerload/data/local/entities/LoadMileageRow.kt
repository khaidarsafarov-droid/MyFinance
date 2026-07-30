package com.truckerload.data.local.entities

/**
 * Narrow projection for ТО mileage math — avoids hydrating full [LoadEntity] rows
 * (rawMessage, route denorm, etc.) when only end-date + miles are needed.
 */
data class LoadMileageRow(
    val id: String,
    val tripId: String,
    val totalMiles: Double,
    val date: String,
    val actualFinishDate: String?,
    val lastDelMillis: Long?,
)

/** Calendar-dot markers: load id + journal date only. */
data class LoadDateRow(
    val id: String,
    val date: String,
)
