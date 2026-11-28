@file:Suppress("MagicNumber", "ReturnCount", "MaxLineLength")

package com.truckerload.domain.parser

import java.time.LocalDate
import java.util.Locale

/**
 * Parser for short dispatcher chat messages (EN / RU / ES / UZ): "PU: Laredo TX ... DEL: Memphis TN ...
 * 2400 all in (~990 mi)". City and state only, no ZIP, labels and amounts in one line.
 * Penalty/fine/lumper amounts and deadhead miles are removed before anything is read.
 * Every field is nullable; null when the text does not look like a chat load at all.
 */
internal object ChatLoadParser {
    private const val MAX_CHAT_LENGTH = 1500
    private const val MIN_RATE = 100.0
    private const val MAX_RATE = 35_000.0
    private const val MIN_MILES = 20.0
    private const val MAX_MILES = 5_000.0
    private const val DEADHEAD_LOOKBACK = 24
    private val flags = setOf(RegexOption.IGNORE_CASE)

    private fun words(list: List<String>) = list.joinToString("|")
    private fun keyword(list: List<String>) = Regex("(?<![\\p{L}\\p{N}])(?:${words(list)})(?![\\p{L}])", RegexOption.IGNORE_CASE)

    private val pickupKey = "(?:${words(LoadSynonyms.pickup)}|de(?=\\s*:))"
    private val deliveryKey = "(?:${words(LoadSynonyms.delivery)}|para(?=\\s*:))"
    private val anyKey = Regex("(?<![\\p{L}\\p{N}])(?:($pickupKey)|($deliveryKey))(?![\\p{L}])", RegexOption.IGNORE_CASE)
    private val penaltyWords = words(LoadSynonyms.penalty)
    private const val MONEY = "(?:\\$\\s*)?\\d[\\d,]*(?:\\.\\d{1,2})?(?:\\s*\\$)?"
    private val penaltyClause = Regex(
        "(?:(?:$penaltyWords)\\W{0,3}$MONEY|$MONEY\\s*(?:$penaltyWords))",
        RegexOption.IGNORE_CASE,
    )
    private val deadlineClause = Regex(
        "(?:(?<![\\p{L}])(?:${words(LoadSynonyms.deadline)})\\s*\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?|\\d{1,2}:\\d{2}\\s*gacha)",
        RegexOption.IGNORE_CASE,
    )

    private val documentMarkers = Regex(
        "confirmation|\\bcharges\\b|line\\s*haul|surcharge|special instructions|\\bcarrier\\b|dispatch sheet|consignee|shipper|invoice|terms",
        RegexOption.IGNORE_CASE,
    )

    /** Short and free of rate-confirmation vocabulary: a dispatcher message, not a document. */
    fun looksLikeChat(raw: String): Boolean = raw.length <= MAX_CHAT_LENGTH && !documentMarkers.containsMatchIn(raw)

    fun extract(raw: String, today: LocalDate? = null): RateConTextExtractor.Fields? {
        if (!looksLikeChat(raw)) return null
        val text = deadlineClause.replace(penaltyClause.replace(raw, " "), " ")
        val (pickup, delivery) = stops(text)
        val date = findDate(text, today)
        val fields = RateConTextExtractor.Fields(findRate(text), findMiles(text), findLoadId(text), pickup, delivery, date)
        val found = listOf(fields.rate, fields.miles, fields.loadId, fields.pickup, fields.delivery).count { it != null }
        return fields.takeIf { found >= 2 }
    }

    // ---- money -------------------------------------------------------------------------------

    private val labelThenAmount = Regex(
        "(?<![\\p{L}])(?:${words(LoadSynonyms.rate)})[\\s:=\\-]{0,3}(?:(?:\\$\\s*)(\\d[\\d,]*(?:\\.\\d{1,2})?)|(\\d[\\d,]*(?:\\.\\d{1,2})?)\\s*\\$)",
        flags,
    )
    private val amountThenLabel = Regex(
        "(?:\\$\\s*)?(\\d[\\d,]*(?:\\.\\d{1,2})?)\\s*\\$?\\s*(?:all[\\s-]?in|алл\\s*ин|flat|флэт|флет|баксов|total|todo incluido|rate|usd)(?![\\p{L}])",
        flags,
    )

    private fun amountOrNull(value: String): Double? =
        value.replace(",", "").toDoubleOrNull()?.takeIf { it in MIN_RATE..MAX_RATE }

    private fun findRate(text: String): Double? {
        val labeled = labelThenAmount.findAll(text).mapNotNull { m ->
            amountOrNull(m.groupValues[1].ifEmpty { m.groupValues[2] })
        }.firstOrNull()
        return labeled ?: amountThenLabel.findAll(text).mapNotNull { amountOrNull(it.groupValues[1]) }.firstOrNull()
    }

    // ---- miles -------------------------------------------------------------------------------

    private val milesRegex = Regex(
        "(\\d[\\d,]*(?:\\.\\d+)?)\\s*(?:mill?as|miles?|mil|mi|миль|мили|мил)(?![\\p{L}])",
        flags,
    )
    private val deadheadRegex = keyword(LoadSynonyms.deadhead)

    private fun findMiles(text: String): Double? = milesRegex.findAll(text).firstNotNullOfOrNull { m ->
        val before = text.substring(maxOf(0, m.range.first - DEADHEAD_LOOKBACK), m.range.first)
        val miles = m.groupValues[1].replace(",", "").toDoubleOrNull()
        miles?.takeIf { it in MIN_MILES..MAX_MILES && !deadheadRegex.containsMatchIn(before) }
    }

    // ---- load id -----------------------------------------------------------------------------

    private const val ID = "([A-Z0-9][A-Z0-9\\-]{3,19})"
    private val idPatterns = listOf(
        Regex("VRID\\s*[:#]?\\s*$ID", flags),
        Regex("(?<![\\p{L}])(?:load|груз|carga|yuk|trip)\\s*#\\s*$ID", flags),
        Regex("(?<![\\p{L}])PU\\s*#\\s*$ID", flags),
    )

    private fun findLoadId(text: String): String? = idPatterns.firstNotNullOfOrNull { regex ->
        regex.findAll(text).map { it.groupValues[1].uppercase(Locale.US) }.firstOrNull { id -> id.any(Char::isDigit) }
    }

    /** Texts right after each pickup label, up to the next label or line end: where the pickup date sits. */
    private fun pickupSegments(text: String): List<String> {
        val keys = anyKey.findAll(text).toList()
        return keys.indices.filter { keys[it].groups[1] != null }.map { index ->
            val lineEnd = text.indexOf('\n', keys[index].range.last).let { if (it < 0) text.length else it }
            val end = minOf(lineEnd, keys.getOrNull(index + 1)?.range?.first ?: lineEnd)
            text.substring(keys[index].range.first, maxOf(keys[index].range.first, end))
        }
    }

    private fun findDate(text: String, today: LocalDate?): LocalDate? = today?.let { day ->
        pickupSegments(text).firstNotNullOfOrNull { MultilingualDate.extract(it, day, text) }
            ?: MultilingualDate.extract(text, day)
    }

    // ---- stops -------------------------------------------------------------------------------

    private val place = Regex("(?<![A-Za-z0-9])((?:[A-Z][A-Za-z.'\\-]+ +){0,3}?[A-Z][A-Za-z.'\\-]+)(?:, ?| +)([A-Z]{2})(?![A-Za-z])")
    private val arrow = Regex("\\s*(?:->|→|—>|=>|➡️?)\\s*")

    private fun placesIn(segment: String): List<String> = place.findAll(segment)
        .filter { it.groupValues[2] in StateCodes.all }
        .map { "${it.groupValues[1].trim()}, ${it.groupValues[2]}" }.toList()

    private fun stops(text: String): Pair<String?, String?> {
        val keys = anyKey.findAll(text).toList()
        val pickups = mutableListOf<String>()
        val deliveries = mutableListOf<String>()
        keys.forEachIndexed { index, key ->
            val lineEnd = text.indexOf('\n', key.range.last).let { if (it < 0) text.length else it }
            val end = minOf(lineEnd, keys.getOrNull(index + 1)?.range?.first ?: lineEnd)
            val found = placesIn(text.substring(key.range.last + 1, maxOf(key.range.last + 1, end)))
            if (key.groups[1] != null) pickups += found else deliveries += found
        }
        var pickup = pickups.firstOrNull()
        var delivery = deliveries.lastOrNull()
        if (pickup == null || delivery == null) {
            val all = placesIn(text).distinct()
            val arrowPair = text.lines().firstNotNullOfOrNull { line ->
                val sides = arrow.split(line)
                val from = sides.firstOrNull()?.let(::placesIn)?.lastOrNull()
                val to = sides.drop(1).firstOrNull()?.let(::placesIn)?.firstOrNull()
                if (sides.size >= 2 && from != null && to != null) from to to else null
            }
            pickup = pickup ?: arrowPair?.first ?: all.firstOrNull()
            delivery = delivery ?: arrowPair?.second ?: all.lastOrNull()?.takeIf { it != pickup }
        }
        return pickup to delivery
    }
}
