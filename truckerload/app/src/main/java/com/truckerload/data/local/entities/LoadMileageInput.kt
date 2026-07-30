package com.truckerload.data.local.entities

/**
 * Lean Room projection for ТО mileage math — avoids SELECT * + stop hydrate.
 * Maps to [com.truckerload.domain.maintenance.MaintenanceMileageUseCase.LoadInput].
 */
data class LoadMileageInput(
    val id: String,
    val tripId: String,
    val totalMiles: Double,
    val date: String,
    val actualFinishDate: String? = null,
    val lastDelMillis: Long? = null,
)
