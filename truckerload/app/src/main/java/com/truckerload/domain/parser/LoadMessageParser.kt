package com.truckerload.domain.parser

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import com.truckerload.domain.model.withRouteMetrics
import java.util.Locale

/**
 * Regex/FSM-based parser for Amazon Relay and similar load messages.
 * Supports multiple PU/DEL legs per trip (PU# blocks).
 */
object LoadMessageParser {

    private val tripIdPatterns = listOf(
        Regex("""Trip\s*ID\s*[:\|]?\s*([A-Z0-9\-]+)""", RegexOption.IGNORE_CASE),
        Regex("""Trip\s+([A-Z0-9\-]{6,})""", RegexOption.IGNORE_CASE),
        Regex("""PU#\s*([A-Z0-9]+)""", RegexOption.IGNORE_CASE)
    )
    private val totalRatePattern = Regex("""Total\s*Rate\s*[:\s]*\$?\s*([\d.,]+)""", RegexOption.IGNORE_CASE)
    private val totalMilesPattern = Regex("""Total\s*Loaded\s*Miles\s*[:\s]*([\d.,]+)""", RegexOption.IGNORE_CASE)
    private val inlineRatePattern = Regex("""Rate\s+([\d.,]+)""", RegexOption.IGNORE_CASE)
    private val inlineMilesPattern = Regex("""([\d.,]+)\s*mi\b""", RegexOption.IGNORE_CASE)

    private val puBlockPattern = Regex(
        """PU\s+([A-Z0-9\-]+)\s*\|\s*(.+?)\s*\|\s*([^\n|]+)\s*\|\s*(?:Note:\s*)?([^\n]*)""",
        RegexOption.IGNORE_CASE
    )
    private val delBlockPattern = Regex(
        """DEL\s*\|\s*([^,\n|]+)\s*,\s*([^\n|]+)\s*\|\s*([^\n|]+)""",
        RegexOption.IGNORE_CASE
    )

    private val puHeaderPattern = Regex("""^PU#\s*(\S+)""", RegexOption.MULTILINE)
    private val sectionStopPattern = Regex(
        """^(Pu|Del)-(address|time)\s*[:\s]*(.*)$""",
        RegexOption.IGNORE_CASE
    )

    fun parseAll(rawMessage: String): List<Load> {
        if (!MessageClassifier.isLoadLike(rawMessage)) return emptyList()
        return splitBlocks(rawMessage).mapNotNull { parseBlock(it, rawMessage) }
    }

    fun parseOne(rawMessage: String): Load? = parseAll(rawMessage).firstOrNull()

    private fun splitBlocks(text: String): List<String> {
        val normalized = text.replace("\r\n", "\n").trim()
        val byTrip = Regex("""(?=^Trip\s*ID)""", RegexOption.MULTILINE)
            .split(normalized)
            .map { it.trim() }
            .filter { it.isNotBlank() && MessageClassifier.isLoadLike(it) }
        if (byTrip.isNotEmpty()) return byTrip

        val byPu = Regex("""(?=^PU#\s)""", RegexOption.MULTILINE)
            .split(normalized)
            .map { it.trim() }
            .filter { it.isNotBlank() && MessageClassifier.isLoadLike(it) }
        if (byPu.size > 1) return byPu

        return listOf(normalized)
    }

    private fun parseBlock(block: String, rawMessage: String): Load? {
        val tripId = ParseUtils.firstMatch(block, tripIdPatterns)?.uppercase(Locale.US).orEmpty()
        if (tripId.isBlank()) return null

        val totalRate = totalRatePattern.find(block)?.groupValues?.get(1)?.let { ParseUtils.parseMoney(it) }
            ?: inlineRatePattern.find(block)?.groupValues?.get(1)?.let { ParseUtils.parseMoney(it) }
            ?: 0.0
        val totalMiles = totalMilesPattern.find(block)?.groupValues?.get(1)?.let { ParseUtils.parseMiles(it) }
            ?: inlineMilesPattern.find(block)?.groupValues?.get(1)?.let { ParseUtils.parseMiles(it) }
            ?: 0.0

        val stops = parseAllStops(block, tripId)
        val firstPuTime = stops.firstOrNull { it.type == StopType.PU }?.scheduledTime
        val date = ParseUtils.normalizeDate(firstPuTime)

        val puStops = stops.filter { it.type == StopType.PU }
        val delStops = stops.filter { it.type == StopType.DEL }
        val pointA = puStops.firstOrNull()?.let { formatRoutePoint(it) }.orEmpty()
        val pointB = delStops.lastOrNull()?.let { formatRoutePoint(it) }.orEmpty()

        val now = System.currentTimeMillis()
        if (totalRate <= 0.0 || (pointA.isBlank() && pointB.isBlank())) return null

        val draft = Load(
            id = tripId,
            tripId = tripId,
            date = date,
            totalRate = totalRate,
            totalMiles = totalMiles,
            pointA = pointA,
            pointB = pointB,
            puCount = puStops.size,
            delCount = delStops.size,
            weekNumber = 0,
            year = 0,
            rawMessage = rawMessage,
            parsedAt = now,
            updatedAt = now,
            stops = stops,
            penalties = emptyList()
        )
        val (weekNumber, year) = com.truckerload.utils.getLoadReportingWeek(draft)
        return draft.copy(weekNumber = weekNumber, year = year).withRouteMetrics()
    }

    private fun parseAllStops(block: String, tripId: String): List<Stop> {
        val relayLegs = parseRelayLegStops(block, tripId)
        if (relayLegs.isNotEmpty()) return relayLegs

        val labelStops = parseLabelPairStops(block, tripId)
        if (labelStops.isNotEmpty()) return labelStops

        val pipeStops = parsePipeStops(block, tripId)
        if (pipeStops.isNotEmpty()) return pipeStops

        return emptyList()
    }

    /** Parses multiple PU# / Pu-address / Del-address legs within one trip block. */
    private fun parseRelayLegStops(block: String, tripId: String): List<Stop> {
        val headers = puHeaderPattern.findAll(block).toList()
        if (headers.isEmpty()) return emptyList()

        val stops = mutableListOf<Stop>()
        headers.forEachIndexed { index, header ->
            val start = header.range.first
            val end = headers.getOrNull(index + 1)?.range?.first ?: block.length
            val segment = block.substring(start, end)
            val puCode = header.groupValues[1].trim()
            val sections = parseRelaySections(segment)

            sections["pu-address"]?.let { lines ->
                val addr = ParseUtils.parseMultiLineAddress(lines)
                val puTime = sections["pu-time"]?.firstOrNull().orEmpty()
                stops.add(
                    buildStop(
                        tripId = tripId,
                        stopNumber = stops.size + 1,
                        type = StopType.PU,
                        puNumber = puCode,
                        note = sections["note"]?.firstOrNull(),
                        scheduledTime = puTime,
                        addr = addr,
                    )
                )
            }

            sections["del-address"]?.let { lines ->
                val addr = ParseUtils.parseMultiLineAddress(lines)
                val delTime = sections["del-time"]?.firstOrNull().orEmpty()
                stops.add(
                    buildStop(
                        tripId = tripId,
                        stopNumber = stops.size + 1,
                        type = StopType.DEL,
                        puNumber = null,
                        note = null,
                        scheduledTime = delTime,
                        addr = addr,
                    )
                )
            }
        }
        return stops
    }

    private fun parseRelaySections(segment: String): Map<String, List<String>> {
        val result = mutableMapOf<String, MutableList<String>>()
        var currentKey: String? = null

        for (rawLine in segment.lines()) {
            val line = rawLine.trim()
            if (line.isBlank()) continue
            if (line.startsWith("PU#", ignoreCase = true)) continue

            val sectionMatch = sectionStopPattern.find(line)
            if (sectionMatch != null) {
                val kind = sectionMatch.groupValues[1].lowercase(Locale.US)
                val field = sectionMatch.groupValues[2].lowercase(Locale.US)
                currentKey = "$kind-$field"
                val remainder = sectionMatch.groupValues[3].trim()
                result.getOrPut(currentKey) { mutableListOf() }.apply {
                    if (remainder.isNotBlank()) add(remainder)
                }
                continue
            }

            if (line.startsWith("Note", ignoreCase = true)) {
                currentKey = "note"
                val noteText = line.substringAfter(":", line.substringAfter("Note", line)).trim()
                result.getOrPut("note") { mutableListOf() }.apply {
                    if (noteText.isNotBlank()) add(noteText)
                }
                continue
            }

            if (currentKey != null && !isTripLevelLine(line)) {
                result.getOrPut(currentKey) { mutableListOf() }.add(line)
            }
        }
        return result
    }

    private fun isTripLevelLine(line: String): Boolean =
        line.startsWith("Trip", ignoreCase = true) ||
            line.startsWith("Total", ignoreCase = true) ||
            line.startsWith("PU#", ignoreCase = true)

    private fun parseLabelPairStops(block: String, tripId: String): List<Stop> {
        val sections = parseRelaySections(block)
        if (!sections.containsKey("pu-address")) return emptyList()

        val stops = mutableListOf<Stop>()
        sections["pu-address"]?.let { lines ->
            val addr = ParseUtils.parseMultiLineAddress(lines)
            stops.add(
                buildStop(
                    tripId = tripId,
                    stopNumber = stops.size + 1,
                    type = StopType.PU,
                    puNumber = tripId.takeIf { block.contains("PU#", ignoreCase = true) },
                    note = sections["note"]?.firstOrNull(),
                    scheduledTime = sections["pu-time"]?.firstOrNull().orEmpty(),
                    addr = addr,
                )
            )
        }
        sections["del-address"]?.let { lines ->
            val addr = ParseUtils.parseMultiLineAddress(lines)
            stops.add(
                buildStop(
                    tripId = tripId,
                    stopNumber = stops.size + 1,
                    type = StopType.DEL,
                    puNumber = null,
                    note = null,
                    scheduledTime = sections["del-time"]?.firstOrNull().orEmpty(),
                    addr = addr,
                )
            )
        }
        return stops
    }

    private fun parsePipeStops(block: String, tripId: String): List<Stop> {
        val stops = mutableListOf<Stop>()

        puBlockPattern.findAll(block).forEach { m ->
            val code = m.groupValues[1].trim()
            val addr = ParseUtils.parseAddressLine(m.groupValues[2].trim() + ", " + m.groupValues[3].trim())
            stops.add(
                buildStop(
                    tripId = tripId,
                    stopNumber = stops.size + 1,
                    type = StopType.PU,
                    puNumber = code,
                    note = m.groupValues[4].trim().ifBlank { null },
                    scheduledTime = m.groupValues[3].trim(),
                    addr = addr,
                )
            )
        }

        delBlockPattern.findAll(block).forEach { m ->
            val addr = ParseUtils.parseAddressLine(m.groupValues[2].trim())
            stops.add(
                buildStop(
                    tripId = tripId,
                    stopNumber = stops.size + 1,
                    type = StopType.DEL,
                    puNumber = null,
                    note = null,
                    scheduledTime = m.groupValues[3].trim(),
                    addr = addr,
                    facilityOverride = m.groupValues[1].trim(),
                )
            )
        }
        return stops
    }

    private fun buildStop(
        tripId: String,
        stopNumber: Int,
        type: StopType,
        puNumber: String?,
        note: String?,
        scheduledTime: String,
        addr: ParseUtils.AddressParts,
        facilityOverride: String? = null,
    ): Stop = Stop(
        id = 0,
        loadId = tripId,
        stopNumber = stopNumber,
        type = type,
        puNumber = puNumber,
        note = note,
        scheduledTime = scheduledTime,
        timezone = extractTimezone(scheduledTime),
        facilityCode = facilityOverride ?: addr.facilityCode,
        fullAddress = addr.fullAddress,
        city = addr.city,
        state = addr.state,
        zip = addr.zip,
    )

    private fun formatRoutePoint(stop: Stop): String {
        val cityState = listOf(stop.city, stop.state).filter { it.isNotBlank() }.joinToString(", ")
        return cityState.ifBlank { stop.fullAddress }
    }

    private fun extractTimezone(time: String?): String {
        if (time.isNullOrBlank()) return ""
        val tz = Regex("""\b([A-Z]{2,4})\s*$""").find(time.trim())?.groupValues?.get(1)
        return tz.orEmpty()
    }
}
