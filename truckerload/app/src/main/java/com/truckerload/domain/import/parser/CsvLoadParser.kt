package com.truckerload.domain.import.parser

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import com.truckerload.domain.model.withRouteMetrics
import com.truckerload.domain.parser.ParseUtils
import com.truckerload.utils.getLoadReportingWeek
import java.util.Locale

/** Minimal CSV: TripId,Rate,Miles,Origin,Destination,... */
class CsvLoadParser : LoadParser {
    override fun parse(input: String): List<Load> {
        val lines = input.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
        if (lines.size < 2) return emptyList()

        val headers = lines.first().split(",").map { it.trim().lowercase(Locale.US) }
        val tripIdx = headers.indexOfFirst { it.contains("trip") }
        val rateIdx = headers.indexOfFirst { it.contains("rate") }
        val milesIdx = headers.indexOfFirst { it.contains("mile") }
        val originIdx = headers.indexOfFirst { it.contains("origin") || it.contains("pickup") || it == "from" }
        val destIdx = headers.indexOfFirst { it.contains("dest") || it.contains("delivery") || it == "to" }

        if (tripIdx == -1 || rateIdx == -1) return emptyList()

        val now = System.currentTimeMillis()
        return lines.drop(1).mapNotNull { line ->
            val cols = line.split(",").map { it.trim() }
            if (cols.size <= maxOf(tripIdx, rateIdx)) return@mapNotNull null

            val tripId = cols[tripIdx].uppercase(Locale.US)
            val rate = ParseUtils.parseMoney(cols[rateIdx])
            if (rate <= 0) return@mapNotNull null
            val miles = milesIdx.takeIf { it != -1 && it < cols.size }
                ?.let { ParseUtils.parseMiles(cols[it]) } ?: 0.0
            val pointA = originIdx.takeIf { it != -1 && it < cols.size }?.let { cols[it] }.orEmpty()
            val pointB = destIdx.takeIf { it != -1 && it < cols.size }?.let { cols[it] }.orEmpty()

            if (tripId.isBlank() || rate <= 0) return@mapNotNull null

            val stops = buildList {
                if (pointA.isNotBlank()) {
                    add(
                        Stop(
                            id = 0, loadId = tripId, stopNumber = 1, type = StopType.PU,
                            puNumber = null, note = null, scheduledTime = "", timezone = "",
                            facilityCode = null, fullAddress = pointA,
                            city = pointA.substringBefore(",").trim(),
                            state = pointA.substringAfter(",", "").trim(), zip = "",
                        )
                    )
                }
                if (pointB.isNotBlank()) {
                    add(
                        Stop(
                            id = 0, loadId = tripId, stopNumber = 2, type = StopType.DEL,
                            puNumber = null, note = null, scheduledTime = "", timezone = "",
                            facilityCode = null, fullAddress = pointB,
                            city = pointB.substringBefore(",").trim(),
                            state = pointB.substringAfter(",", "").trim(), zip = "",
                        )
                    )
                }
            }

            val draft = Load(
                id = tripId,
                tripId = tripId,
                date = "",
                totalRate = rate,
                totalMiles = miles,
                pointA = pointA,
                pointB = pointB,
                puCount = stops.count { it.type == StopType.PU },
                delCount = stops.count { it.type == StopType.DEL },
                weekNumber = 0,
                year = 0,
                rawMessage = line,
                parsedAt = now,
                updatedAt = now,
                stops = stops,
            )
            val (week, year) = getLoadReportingWeek(draft)
            draft.copy(weekNumber = week, year = year).withRouteMetrics()
        }
    }
}
