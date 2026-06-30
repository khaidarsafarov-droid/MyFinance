package com.truckerload.domain.analytics

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.analytics.RouteData
import com.truckerload.domain.model.withRouteMetrics

/** Resolves human-readable city/state labels for analytics (not warehouse codes like TOL3). */
object RouteDisplayHelper {

    private val WAREHOUSE_CODE = Regex("^[A-Za-z]{2,5}\\d?$")

    fun isWarehouseCode(value: String): Boolean {
        val trimmed = value.trim()
        if (trimmed.isBlank() || trimmed.contains(',') || trimmed.contains(' ')) return false
        return trimmed.length <= 8 && WAREHOUSE_CODE.matches(trimmed)
    }

    fun looksLikeCityState(value: String): Boolean {
        val trimmed = value.trim()
        return trimmed.contains(',') && trimmed.length > 4
    }

    fun sanitizeEndpoint(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return ""
        if (looksLikeCityState(trimmed)) return trimmed
        if (isWarehouseCode(trimmed)) return ""
        return trimmed
    }

    fun resolveEndpoints(load: Load): Pair<String, String> {
        val metrics = load.withRouteMetrics()
        var origin = sanitizeEndpoint(metrics.firstPuCityState)
        var destination = sanitizeEndpoint(metrics.lastDelCityState)

        if (origin.isBlank() || destination.isBlank()) {
            val route = metrics.route.ifBlank { "${metrics.pointA} → ${metrics.pointB}" }
            val parts = route.split('→', limit = 2).map { it.trim() }
            if (parts.size == 2) {
                if (origin.isBlank()) origin = sanitizeEndpoint(parts[0])
                if (destination.isBlank()) destination = sanitizeEndpoint(parts[1])
            }
        }

        if (origin.isBlank()) origin = sanitizeEndpoint(metrics.pointA)
        if (destination.isBlank()) destination = sanitizeEndpoint(metrics.pointB)

        return origin to destination
    }

    fun topRoutes(loads: List<Load>, limit: Int): List<RouteData> {
        return loads
            .mapNotNull { load ->
                val (origin, destination) = resolveEndpoints(load)
                if (origin.isBlank() || destination.isBlank()) null
                else Triple(origin, destination, load)
            }
            .groupBy { it.first to it.second }
            .map { (pair, group) ->
                val gross = group.sumOf { it.third.totalRate }
                val miles = group.sumOf { it.third.totalMiles }
                val (origin, destination) = pair
                RouteData(
                    origin = origin,
                    destination = destination,
                    route = "$origin → $destination",
                    gross = gross,
                    miles = miles,
                    loadCount = group.size,
                    rpm = if (miles > 0) gross / miles else 0.0,
                )
            }
            .sortedByDescending { it.gross }
            .take(limit)
    }
}
