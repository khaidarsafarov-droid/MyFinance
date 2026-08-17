package com.truckerload.domain.parser

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import com.truckerload.domain.model.withRouteMetrics
import com.truckerload.utils.getLoadReportingWeek
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds a [Load] from typed fields so drivers can add cargo without a bot message.
 */
object ManualLoadFactory {

    fun build(
        tripId: String,
        date: String,
        rate: Double,
        miles: Double,
        pointA: String,
        pointB: String,
        rawMessage: String = "",
        nowMillis: Long = System.currentTimeMillis(),
    ): Load {
        val resolvedTrip = tripId.trim().ifBlank { generateTripId(nowMillis, pointA, pointB, rate) }
        val resolvedDate = ParseUtils.normalizeDate(date).takeIf { it.length >= 10 }
            ?: todayIso(nowMillis)
        val sanitizedMiles = ParseUtils.sanitizeLoadedMiles(miles.coerceAtLeast(0.0), rate)
        val origin = ParseUtils.parseAddressLine(pointA)
        val dest = ParseUtils.parseAddressLine(pointB)
        val stops = buildList {
            if (pointA.isNotBlank()) {
                add(stop(resolvedTrip, 1, StopType.PU, origin, pointA))
            }
            if (pointB.isNotBlank()) {
                add(stop(resolvedTrip, size + 1, StopType.DEL, dest, pointB))
            }
        }
        val draft = Load(
            id = resolvedTrip,
            tripId = resolvedTrip,
            date = resolvedDate,
            totalRate = rate,
            totalMiles = sanitizedMiles,
            pointA = pointA.trim(),
            pointB = pointB.trim(),
            puCount = stops.count { it.type == StopType.PU },
            delCount = stops.count { it.type == StopType.DEL },
            weekNumber = 0,
            year = 0,
            rawMessage = rawMessage.ifBlank { formatRaw(resolvedTrip, rate, sanitizedMiles, pointA, pointB) },
            parsedAt = nowMillis,
            updatedAt = nowMillis,
            stops = stops,
        )
        val (week, year) = getLoadReportingWeek(draft)
        return draft.copy(weekNumber = week, year = year).withRouteMetrics()
    }

    fun generateTripId(
        nowMillis: Long,
        pointA: String = "",
        pointB: String = "",
        rate: Double = 0.0,
    ): String {
        val day = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(nowMillis))
        val seed = "$pointA|$pointB|$rate|$nowMillis".hashCode().toUInt().toString(16)
            .uppercase(Locale.US)
            .padStart(6, '0')
            .take(6)
        return "M-$day-$seed"
    }

    private fun stop(
        tripId: String,
        number: Int,
        type: StopType,
        addr: ParseUtils.AddressParts,
        fallback: String,
    ) = Stop(
        id = 0,
        loadId = tripId,
        stopNumber = number,
        type = type,
        puNumber = null,
        note = null,
        scheduledTime = "",
        timezone = "",
        facilityCode = addr.facilityCode,
        fullAddress = addr.fullAddress.ifBlank { fallback.trim() },
        city = addr.city,
        state = addr.state,
        zip = addr.zip,
    )

    private fun formatRaw(
        tripId: String,
        rate: Double,
        miles: Double,
        pointA: String,
        pointB: String,
    ): String = buildString {
        appendLine("Trip ID: $tripId")
        appendLine("Total Rate: $rate")
        if (miles > 0) appendLine("Total Loaded Miles: $miles mi")
        if (pointA.isNotBlank()) appendLine("Pu-address: $pointA")
        if (pointB.isNotBlank()) appendLine("Del-address: $pointB")
    }.trim()

    private fun todayIso(nowMillis: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMillis))
}
