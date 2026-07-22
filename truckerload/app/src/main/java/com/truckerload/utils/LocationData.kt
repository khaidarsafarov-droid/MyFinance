package com.truckerload.utils

import java.util.Locale

data class LocationData(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
) {
    val hasCoordinates: Boolean
        get() = latitude != null && longitude != null

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
        get() {
            val lat = latitude ?: return "—"
            val lon = longitude ?: return "—"
            return formatCoordinates(lat, lon)
        }

    companion object {
        fun formatCoordinates(lat: Double, lon: Double): String {
            val latDir = if (lat >= 0) "N" else "S"
            val lonDir = if (lon >= 0) "E" else "W"
            return String.format(Locale.US,
                "%.3f° %s, %.3f° %s",
                kotlin.math.abs(lat),
                latDir,
                kotlin.math.abs(lon),
                lonDir,
            )
        }
    }
}
