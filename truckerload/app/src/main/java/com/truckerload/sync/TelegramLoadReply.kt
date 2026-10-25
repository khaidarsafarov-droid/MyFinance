package com.truckerload.sync

import com.truckerload.domain.model.Load
import com.truckerload.domain.units.DisplayUnits
import com.truckerload.domain.units.MeasurementUnits
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

/**
 * Extra lines under a Telegram "load saved" reply: rate, miles, and cities.
 */
internal object TelegramLoadReply {

    fun routeLine(load: Load): String {
        val origin = load.firstPuCityState.ifBlank { load.pointA }
        val destination = load.lastDelCityState.ifBlank { load.pointB }
        val from = origin.trim()
        val to = destination.trim()
        return when {
            from.isNotEmpty() && to.isNotEmpty() -> "$from → $to"
            from.isNotEmpty() -> from
            else -> to.trim()
        }
    }

    fun detail(load: Load): String = detail(
        rateCents = load.totalRate.cents,
        miles = load.totalMiles,
        origin = load.firstPuCityState.ifBlank { load.pointA },
        destination = load.lastDelCityState.ifBlank { load.pointB },
    )

    fun detail(rateCents: Long, miles: Double, origin: String, destination: String): String {
        val money = if (rateCents > 0L) formatMoney(rateCents) else ""
        val milesText = if (miles <= 0.0) {
            ""
        } else if (DisplayUnits.isMetric) {
            "${formatMiles(MeasurementUnits.displayDistance(miles))} km"
        } else {
            "${formatMiles(miles)} mi"
        }
        val figures = listOf(money, milesText).filter { it.isNotBlank() }.joinToString(" · ")
        val from = origin.trim()
        val to = destination.trim()
        val route = when {
            from.isNotEmpty() && to.isNotEmpty() -> "$from → $to"
            from.isNotEmpty() -> from
            else -> to
        }
        return listOf(figures, route).filter { it.isNotBlank() }.joinToString("\n")
    }

    private fun formatMoney(cents: Long): String {
        val dollars = cents / 100
        val fraction = (cents % 100).toString().padStart(2, '0')
        return "$" + "%,d".format(Locale.US, dollars) + "." + fraction
    }

    private fun formatMiles(miles: Double): String {
        val tenths = round(miles * 10.0) / 10.0
        return if (abs(tenths - tenths.toLong()) < 0.05) {
            tenths.toLong().toString()
        } else {
            String.format(Locale.US, "%.1f", tenths)
        }
    }
}
