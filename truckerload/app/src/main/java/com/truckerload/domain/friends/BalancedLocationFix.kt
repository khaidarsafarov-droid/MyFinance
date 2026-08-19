package com.truckerload.domain.friends

/** Single GPS sample from [PRIORITY_BALANCED_POWER_ACCURACY] — no reverse-geocode. */
data class BalancedLocationFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timestampMillis: Long,
)
