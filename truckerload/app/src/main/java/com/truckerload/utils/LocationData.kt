package com.truckerload.utils

import java.util.Locale

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val city: String,
    val state: String,
    val zipCode: String,
) {
    val cityStateLine: String
        get() = buildString {
            if (city.isNotBlank()) append(city)
            if (state.isNotBlank()) {
                if (isNotEmpty()) append(", ")
                append(state)
            }
            if (isEmpty()) append("—")
        }

    fun formattedTimestamp(timestamp: Long): String =
        PhotoManager.formatDateTime(timestamp)

    val coordinatesLine: String
        get() = formatCoordinates(latitude, longitude)

    companion object {
        fun formatCoordinates(lat: Double, lon: Double): String {
            val latDir = if (lat >= 0) "N" else "S"
            val lonDir = if (lon >= 0) "E" else "W"
            return String.format(
                Locale.US,
                "%.3f° %s, %.3f° %s",
                kotlin.math.abs(lat),
                latDir,
                kotlin.math.abs(lon),
                lonDir,
            )
        }
    }
}
