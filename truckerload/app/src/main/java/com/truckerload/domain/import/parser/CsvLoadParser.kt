package com.truckerload.domain.import.parser

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import com.truckerload.domain.model.withRouteMetrics
import com.truckerload.domain.parser.ParseUtils
import com.truckerload.utils.getLoadReportingWeek
import java.util.Locale

/** Minimal CSV: TripId,Rate,Miles,Origin,Destination,Date,... */
class CsvLoadParser : LoadParser {
    override fun parse(input: String): List<Load> {
        val lines = input.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
        if (lines.size < 2) return emptyList()

        // FIX: quote-aware split so "SWF2, Garner, NC" stays one column
        val headers = splitCsvLine(lines.first()).map { it.trim().lowercase(Locale.US) }
        val tripIdx = headers.indexOfFirst { it.contains("trip") }
        val rateIdx = headers.indexOfFirst { it.contains("rate") }
        val milesIdx = headers.indexOfFirst { it.contains("mile") }
        val originIdx = headers.indexOfFirst { it.contains("origin") || it.contains("pickup") || it == "from" }
        val destIdx = headers.indexOfFirst { it.contains("dest") || it.contains("delivery") || it == "to" }
        val dateIdx = headers.indexOfFirst {
            it == "date" ||
                it.contains("pickup_date") ||
                it == "pickup date" ||
                (it.contains("pu") && it.contains("date"))
        }

        if (tripIdx == -1 || rateIdx == -1) return emptyList()

        val now = System.currentTimeMillis()
        return lines.drop(1).mapNotNull { line ->
            val cols = splitCsvLine(line)
            if (cols.size <= maxOf(tripIdx, rateIdx)) return@mapNotNull null

            val tripId = cols[tripIdx].uppercase(Locale.US)
            val rate = ParseUtils.parseMoney(cols[rateIdx])
            if (rate <= 0) return@mapNotNull null
            val rawMiles = milesIdx.takeIf { it != -1 && it < cols.size }
                ?.let { ParseUtils.parseMiles(cols[it]) } ?: 0.0
            // FIX: apply Relay typo sanitization (same as LoadMessageParser / Room mapper)
            val miles = ParseUtils.sanitizeLoadedMiles(rawMiles, rate)
            val pointA = originIdx.takeIf { it != -1 && it < cols.size }?.let { cols[it] }.orEmpty()
            val pointB = destIdx.takeIf { it != -1 && it < cols.size }?.let { cols[it] }.orEmpty()
            val dateRaw = dateIdx.takeIf { it != -1 && it < cols.size }?.let { cols[it] }.orEmpty()
            val date = ParseUtils.normalizeDate(dateRaw).takeIf { it.length >= 10 }.orEmpty()
            // FIX: reject undated rows instead of silently assigning the current week
            if (date.isBlank()) return@mapNotNull null

            if (tripId.isBlank() || rate <= 0) return@mapNotNull null

            val originAddr = ParseUtils.parseAddressLine(pointA)
            val destAddr = ParseUtils.parseAddressLine(pointB)

            val stops = buildList {
                if (pointA.isNotBlank()) {
                    add(
                        Stop(
                            id = 0, loadId = tripId, stopNumber = 1, type = StopType.PU,
                            puNumber = null, note = null, scheduledTime = "", timezone = "",
                            // FIX: FACILITY, City, ST via ParseUtils — not first-comma split
                            facilityCode = originAddr.facilityCode,
                            fullAddress = originAddr.fullAddress.ifBlank { pointA },
                            city = originAddr.city,
                            state = originAddr.state,
                            zip = originAddr.zip,
                        )
                    )
                }
                if (pointB.isNotBlank()) {
                    add(
                        Stop(
                            id = 0, loadId = tripId, stopNumber = 2, type = StopType.DEL,
                            puNumber = null, note = null, scheduledTime = "", timezone = "",
                            facilityCode = destAddr.facilityCode,
                            fullAddress = destAddr.fullAddress.ifBlank { pointB },
                            city = destAddr.city,
                            state = destAddr.state,
                            zip = destAddr.zip,
                        )
                    )
                }
            }

            val draft = Load(
                id = tripId,
                tripId = tripId,
                date = date,
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

    companion object {
        /**
         * RFC 4180-ish field split: commas inside `"..."` do not delimit columns;
         * `""` inside quotes is an escaped quote.
         */
        fun splitCsvLine(line: String): List<String> {
            val result = ArrayList<String>()
            val current = StringBuilder()
            var inQuotes = false
            var i = 0
            while (i < line.length) {
                val c = line[i]
                when {
                    c == '"' -> {
                        if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                            current.append('"')
                            i++
                        } else {
                            inQuotes = !inQuotes
                        }
                    }
                    c == ',' && !inQuotes -> {
                        result.add(current.toString().trim())
                        current.setLength(0)
                    }
                    else -> current.append(c)
                }
                i++
            }
            result.add(current.toString().trim())
            return result
        }
    }
}
