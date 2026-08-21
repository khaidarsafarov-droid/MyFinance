package com.truckerload.domain.geo

data class LatLngPoint(
    val latitude: Double,
    val longitude: Double,
)

data class BalancedLocationFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timestampMillis: Long,
)
