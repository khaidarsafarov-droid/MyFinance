package com.truckerload.domain.parser

import com.truckerload.domain.money.Money
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import com.truckerload.domain.model.withRouteMetrics
import java.util.Locale

/**
 * Regex/FSM-based parser for Amazon Relay and similar load messages.
 * Supports multiple PU/DEL legs per trip (PU# blocks).
 */
internal object LoadMessageParser {

    private val tripIdPatterns = listOf(
        Regex("""Trip\s*(?:ID|#|Number|No\.?)\s*[:\|]?\s*(T-[A-Z0-9]+)""", RegexOption.IGNORE_CASE),
        Regex("""Trip\s*(?:ID|#|Number|No\.?)\s*[:\|]?\s*([A-Z0-9\-]+)""", RegexOption.IGNORE_CASE),
        Regex("""\b(T-[A-Z0-9]{6,})\b""", RegexOption.IGNORE_CASE),
    )
    private val totalRatePattern = Regex("""Total\s*Rate\s*[:\s]*\$?\s*([\d.,]+)""", RegexOption.IGNORE_CASE)
    private val totalMilesPattern = Regex("""Total\s*Loaded\s*Miles\s*[:\s]*([\d.,]+)""", RegexOption.IGNORE_CASE)
    /** `Rate: $1771.47`, `Payout - $900`, `Total Pay: 1,200.00` on its own line (never "Per mile"). */
    private val labeledRatePattern = Regex(
        """(?im)^\s*(?:Total\s*)?(?:Rate|Payout|Pay|Gross(?:\s*Pay)?|Total)\s*[:\-]\s*\$?\s*([\d][\d.,]*)""",
    )
    private val labeledMilesPattern = Regex(
        """(?im)^\s*(?:Total\s*(?:Loaded\s*)?Miles|Trip\s*Distance|Distance|Miles|Trip)""" +
            """\s*[:\-]\s*([\d][\d.,]*)""",
    )
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

    private val puHeaderPattern = Regex("""PU#\s*(\S+)""", RegexOption.IGNORE_CASE)
    private val sectionStopPattern = Regex(
        """^(Pu|Del)-(address|time)\s*[:\s]*(.*)$""",
        RegexOption.IGNORE_CASE
    )

    fun parseAll(
        rawMessage: String,
        referenceMillis: Long = System.currentTimeMillis(),
    ): List<Load> {
        val normalized = TelegramStyledTextNormalizer.normalize(
            rawMessage.replace("\r\n", "\n").trim(),
        )
        if (!MessageClassifier.isLoadLike(normalized)) return emptyList()
        val headers = TelegramMessageDate.scanChatHistory(normalized)
        return splitBlocksWithStarts(normalized).mapNotNull { (start, block) ->
            val ref = TelegramMessageDate.referenceMillisAt(headers, start, referenceMillis)
            parseBlock(block, rawMessage, ref)
        }
    }

    fun parseOne(
        rawMessage: String,
        referenceMillis: Long = System.currentTimeMillis(),
    ): Load? = parseAll(rawMessage, referenceMillis).firstOrNull()

    /**
     * Best-effort draft for the add-load form and the bot: same as [parseOne] but a missing
     * rate or route does not reject the text. Needs a Trip ID.
     */
    fun parseDraft(
        rawMessage: String,
        referenceMillis: Long = System.currentTimeMillis(),
    ): Load? {
        val normalized = TelegramStyledTextNormalizer.normalize(rawMessage.replace("\r\n", "\n").trim())
        val block = splitBlocksWithStarts(normalized).firstOrNull()?.second.takeIf {
            MessageClassifier.isLoadLike(normalized)
        }
        return block?.let { parseBlock(it, rawMessage, referenceMillis, allowPartial = true) }
    }

    private val tripBlockSplitPattern = Regex(
        """(?m)(?=^Trip\s*ID)""",
        RegexOption.IGNORE_CASE,
    )

    private fun splitBlocksWithStarts(text: String): List<Pair<Int, String>> {
        val matches = tripBlockSplitPattern.findAll(text).toList()
        if (matches.isEmpty()) return listOf(0 to text)

        val blocks = matches.mapIndexed { index, match ->
            val start = match.range.first
            val end = matches.getOrNull(index + 1)?.range?.first ?: text.length
            start to text.substring(start, end).trim()
        }.filter { it.second.isNotBlank() && MessageClassifier.isLoadLike(it.second) }
        return blocks.ifEmpty { listOf(0 to text) }
    }

    private fun parseRate(raw: String): Money =
        runCatching { Money.parse(raw) }.getOrElse { Money.fromDollars(ParseUtils.parseMoney(raw)) }

    private fun findRate(block: String): Money =
        totalRatePattern.find(block)?.groupValues?.get(1)?.let(::parseRate)
            ?: labeledRatePattern.find(block)?.groupValues?.get(1)?.let(::parseRate)
            ?: inlineRatePattern.find(block)?.groupValues?.get(1)?.let(::parseRate)
            ?: Money.ZERO

    private fun findMiles(block: String): Double =
        totalMilesPattern.find(block)?.groupValues?.get(1)?.let { ParseUtils.parseMiles(it) }
            ?: labeledMilesPattern.find(block)?.groupValues?.get(1)?.let { ParseUtils.parseMiles(it) }
            // FIX: fallback uses largest "X mi" — first match is often a leg, not Total Loaded Miles
            ?: inlineMilesPattern.findAll(block)
                .mapNotNull { match ->
                    ParseUtils.parseMiles(match.groupValues[1]).takeIf { it > 0.0 }
                }
                .maxOrNull()
            ?: 0.0

    private fun isIncomplete(rate: Money, pointA: String, pointB: String): Boolean =
        !rate.isPositive() || (pointA.isBlank() && pointB.isBlank())

    @Suppress("ReturnCount")
    private fun parseBlock(
        block: String,
        rawMessage: String,
        referenceMillis: Long,
        allowPartial: Boolean = false,
    ): Load? {
        val tripId = ParseUtils.firstMatch(block, tripIdPatterns)?.uppercase(Locale.US).orEmpty()
        if (tripId.isBlank()) return null

        val totalRate = findRate(block)
        val rawMiles = findMiles(block)
        val totalMiles = ParseUtils.sanitizeLoadedMiles(rawMiles, totalRate)

        val stops = parseAllStops(block, tripId)
        val firstPuTime = stops.firstOrNull { it.type == StopType.PU }?.scheduledTime
        // FIX: MM/DD year follows the Telegram/paste timestamp, not device "today"
        val date = com.truckerload.utils.parseDateFromScheduledTime(
            s = firstPuTime,
            referenceMillis = referenceMillis,
        ) ?: ParseUtils.normalizeDate(firstPuTime, referenceMillis = referenceMillis)

        val puStops = stops.filter { it.type == StopType.PU }
        val delStops = stops.filter { it.type == StopType.DEL }
        val pointA = puStops.firstOrNull()?.let { formatRoutePoint(it) }.orEmpty()
        val pointB = delStops.lastOrNull()?.let { formatRoutePoint(it) }.orEmpty()

        val now = referenceMillis.takeIf { it > 0L } ?: System.currentTimeMillis()
        if (!allowPartial && isIncomplete(totalRate, pointA, pointB)) return null

        val draft = Load(
            id = tripId,
            tripId = tripId,
            date = date.orEmpty(),
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
        // Blank Pu-time → date ""; anchor to message/paste time so week is not "today".
        val dated = com.truckerload.utils.LoadDateRepair.ensureDate(draft, referenceMillis = now)
        val (weekNumber, year) = com.truckerload.utils.getLoadReportingWeek(dated)
        return dated.copy(weekNumber = weekNumber, year = year).withRouteMetrics()
    }

    private fun parseAllStops(block: String, tripId: String): List<Stop> {
        val relayLegs = parseRelayLegStops(block, tripId)
        if (relayLegs.isNotEmpty()) return relayLegs

        val numbered = NumberedStopListParser.parse(block, tripId)
        if (numbered.isNotEmpty()) return numbered

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
        data class PendingStop(
            val type: StopType,
            val lines: MutableList<String>,
            val scheduledTime: String,
            val note: String?,
        )

        val pending = mutableListOf<PendingStop>()
        var current: PendingStop? = null
        var lastPuTime = ""
        var lastDelTime = ""
        var lastNote: String? = null

        fun flush() {
            val active = current ?: return
            if (active.lines.isNotEmpty()) pending.add(active)
            current = null
        }

        for (rawLine in block.lines()) {
            val line = rawLine.trim()
            if (line.isBlank()) continue
            if (line.startsWith("PU#", ignoreCase = true)) continue

            val sectionMatch = sectionStopPattern.find(line)
            if (sectionMatch != null) {
                val kind = sectionMatch.groupValues[1].lowercase(Locale.US)
                val field = sectionMatch.groupValues[2].lowercase(Locale.US)
                val remainder = sectionMatch.groupValues[3].trim()
                when (field) {
                    "time" -> {
                        if (kind == "pu") lastPuTime = remainder else lastDelTime = remainder
                    }
                    "address" -> {
                        flush()
                        current = PendingStop(
                            type = if (kind == "pu") StopType.PU else StopType.DEL,
                            lines = mutableListOf<String>().apply {
                                if (remainder.isNotBlank()) add(remainder)
                            },
                            scheduledTime = if (kind == "pu") lastPuTime else lastDelTime,
                            note = if (kind == "pu") lastNote else null,
                        )
                    }
                }
                continue
            }

            if (line.startsWith("Note", ignoreCase = true)) {
                val noteText = line.substringAfter(":", line.substringAfter("Note", line)).trim()
                lastNote = noteText.takeIf { it.isNotBlank() }
                continue
            }

            if (current != null && !isTripLevelLine(line)) {
                current!!.lines.add(line)
            }
        }
        flush()

        if (pending.isEmpty()) return emptyList()

        return pending.mapIndexed { index, item ->
            val addr = ParseUtils.parseMultiLineAddress(item.lines)
            buildStop(
                tripId = tripId,
                stopNumber = index + 1,
                type = item.type,
                puNumber = null,
                note = item.note,
                scheduledTime = item.scheduledTime,
                addr = addr,
            )
        }
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
