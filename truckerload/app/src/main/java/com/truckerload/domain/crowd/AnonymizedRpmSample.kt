package com.truckerload.domain.crowd

import com.truckerload.domain.model.EquipmentType

/**
 * Fields allowed in anonymized Crowd RPM sharing.
 *
 * Never include tripId, rawMessage, street addresses, or other Load PII —
 * only RPM, miles, state pair, reporting week, and optional equipment type.
 */
data class AnonymizedRpmSample(
    val rpm: Double,
    val miles: Double,
    val fromState: String,
    val toState: String,
    val week: Int,
    val year: Int,
    val equipmentType: EquipmentType?,
)
