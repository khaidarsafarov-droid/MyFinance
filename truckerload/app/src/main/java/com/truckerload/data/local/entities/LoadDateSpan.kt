package com.truckerload.data.local.entities

/**
 * Lightweight start/end dates for calendar dots (no stop hydrate).
 * [endDate] is [LoadEntity.actualFinishDate] when set.
 */
data class LoadDateSpan(
    val startDate: String,
    val endDate: String?,
)
