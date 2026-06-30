package com.truckerload.domain.model

import com.truckerload.domain.goal.GoalMoneyMath
import com.truckerload.domain.goal.LoadYieldCalculator

/** Recomputes route, duration, pace and stop counts from stops + rate. */
fun Load.withRouteMetrics(): Load {
    val puStops = stops.filter { it.type == StopType.PU }
    val delStops = stops.filter { it.type == StopType.DEL }
    val firstPuCityState = puStops.firstOrNull()?.let(::formatCityState).orEmpty().ifBlank { pointA }
    val lastDelCityState = delStops.lastOrNull()?.let(::formatCityState).orEmpty().ifBlank { pointB }
    val route = when {
        firstPuCityState.isNotBlank() && lastDelCityState.isNotBlank() ->
            "$firstPuCityState → $lastDelCityState"
        pointA.isNotBlank() && pointB.isNotBlank() -> "$pointA → $pointB"
        else -> ""
    }
    val duration = LoadYieldCalculator.loadActiveDurationDays(this)
    val loadPace = if (duration > 0.0) {
        GoalMoneyMath.roundMoney(totalRate / duration)
    } else {
        0.0
    }
    return copy(
        pointA = firstPuCityState,
        pointB = lastDelCityState,
        route = route,
        firstPuCityState = firstPuCityState,
        lastDelCityState = lastDelCityState,
        durationDays = duration,
        pace = loadPace,
        stopCount = stops.size,
        puCount = puStops.size,
        delCount = delStops.size,
    )
}

private fun formatCityState(stop: Stop): String =
    listOf(stop.city, stop.state).filter { it.isNotBlank() }.joinToString(", ")

fun formatLoadRoute(load: Load): String =
    load.route.ifBlank { "${load.pointA} → ${load.pointB}" }

fun formatDurationDays(days: Double): String {
    val rounded = kotlin.math.ceil(days).toInt().coerceAtLeast(1)
    return when {
        rounded == 1 -> "1 день"
        rounded in 2..4 -> "$rounded дня"
        else -> "$rounded дней"
    }
}

fun formatPacePerDay(pace: Double): String {
    if (pace <= 0.0) return "—"
    val hasFraction = kotlin.math.abs(pace - pace.toLong()) > 0.009
    return if (hasFraction) {
        String.format(java.util.Locale.US, "$%,.2f/день", pace)
    } else {
        String.format(java.util.Locale.US, "$%,.0f/день", pace)
    }
}
