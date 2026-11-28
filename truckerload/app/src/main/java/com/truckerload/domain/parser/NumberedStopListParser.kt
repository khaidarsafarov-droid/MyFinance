package com.truckerload.domain.parser

import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType

/**
 * Amazon Relay "stop list" layout (text is already normalized, see [TelegramStyledTextNormalizer]):
 *
 * ```
 * 1#: 115DZR7G2          (also `Stop 1: ...`, `Stop 1`)
 * Loaded - Preloaded     (type hint)
 * Fri, Nov 27, 10:45 AM EST
 * CMH4                   (facility code)
 * 1550 W Main St
 * West Jefferson, OH 43162-9747
 * ------------------
 * ```
 *
 * Stop 1 is the pickup (origin), the last stop is the delivery (destination).
 * Middle stops follow their type hint (see [typeFromHint]).
 */
internal object NumberedStopListParser {

    private val headerPattern = Regex(
        """^(?:Stop\s*)?(\d{1,2})\s*(?:#\s*:?|:|\.)\s*([A-Z0-9][A-Z0-9\-]{3,})?$""" +
            """|^Stop\s*(\d{1,2})\s*[:\-]?\s*([A-Z0-9][A-Z0-9\-]{3,})?$""",
        RegexOption.IGNORE_CASE,
    )
    private val separatorPattern = Regex("""^[-=_*~.\s]{3,}$""")
    private val timePattern = Regex("""\d{1,2}:\d{2}""")
    private val tzPattern = Regex("""\b([A-Z]{2,4})\s*$""")
    private val hintPattern = Regex(
        """^(?:loaded|empty|preloaded|pre-loaded|drop|live|hook|pick\s*up|pickup|delivery|unload|deliver)\b.*""",
        RegexOption.IGNORE_CASE,
    )
    private val tripLevelPattern = Regex(
        """^(?:(?:Total\s*)?(?:Rate|Payout)|Per\s*mile|Trip\b|Duration|Late\b|No\s+(?:Update|PU|DEL)""" +
            """|Penalt|Distance|Miles)""",
        RegexOption.IGNORE_CASE,
    )
    private val deliveryWords = Regex("""drop|unload|deliver""")
    private val pickupWords = Regex("""pick\s*up|pickup|live|load|hook""")

    private data class RawStop(val id: String?, val hint: String?, val time: String, val lines: List<String>)

    fun parse(block: String, tripId: String): List<Stop> {
        val raw = collect(block)
        if (raw.isEmpty()) return emptyList()
        return raw.mapIndexed { index, item ->
            val addr = ParseUtils.parseMultiLineAddress(item.lines)
            val type = typeFor(index, raw.size, item.hint)
            Stop(
                id = 0,
                loadId = tripId,
                stopNumber = index + 1,
                type = type,
                puNumber = item.id.takeIf { type == StopType.PU },
                note = listOfNotNull(item.hint, item.id.takeIf { type == StopType.DEL })
                    .joinToString(" · ")
                    .ifBlank { null },
                scheduledTime = item.time,
                timezone = tzPattern.find(item.time)?.groupValues?.get(1).orEmpty(),
                facilityCode = addr.facilityCode,
                fullAddress = addr.fullAddress,
                city = addr.city,
                state = addr.state,
                zip = addr.zip,
            )
        }.filter { it.fullAddress.isNotBlank() || it.city.isNotBlank() }
    }

    private class Builder(val id: String?) {
        var hint: String? = null
        var time: String = ""
        val lines = mutableListOf<String>()

        fun add(line: String) {
            when {
                hint == null && hintPattern.matches(line) -> hint = line
                time.isEmpty() && timePattern.containsMatchIn(line) -> time = line
                else -> lines += line
            }
        }

        fun build() = RawStop(id, hint, time, lines)
    }

    private fun idOf(header: MatchResult): String? =
        (header.groups[ID_GROUP]?.value ?: header.groups[STOP_ID_GROUP]?.value)?.ifBlank { null }

    private const val ID_GROUP = 2
    private const val STOP_ID_GROUP = 4

    private fun endsStop(line: String): Boolean {
        val separator = separatorPattern.matches(line)
        return separator || tripLevelPattern.containsMatchIn(line)
    }

    private fun collect(block: String): List<RawStop> {
        val out = mutableListOf<RawStop>()
        var current: Builder? = null
        for (line in block.lines().map { it.trim() }.filter { it.isNotBlank() }) {
            val header = headerPattern.find(line)
            if (header != null || endsStop(line)) {
                current?.let { out += it.build() }
                current = header?.let { Builder(idOf(it)) }
            } else {
                current?.add(line)
            }
        }
        current?.let { out += it.build() }
        return out.filter { it.lines.isNotEmpty() }
    }

    private fun typeFor(index: Int, count: Int, hint: String?): StopType = when {
        index == 0 -> StopType.PU
        index == count - 1 -> StopType.DEL
        else -> typeFromHint(hint) ?: StopType.PU
    }

    /**
     * Drop / unload / delivery words mean delivery; loaded / preloaded / live load / pickup / hook
     * mean pickup; a bare "Empty" means delivery. Null when there is no hint.
     */
    fun typeFromHint(hint: String?): StopType? {
        val h = hint?.lowercase().orEmpty()
        return when {
            h.isBlank() -> null
            deliveryWords.containsMatchIn(h) -> StopType.DEL
            pickupWords.containsMatchIn(h) -> StopType.PU
            h.contains("empty") -> StopType.DEL
            else -> null
        }
    }
}
