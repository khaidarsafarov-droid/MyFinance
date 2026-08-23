package com.truckerload.domain.parser

import com.truckerload.domain.model.Load
import java.util.Locale

/**
 * Fallback parser for pasted / OCR text that is not a full Amazon Relay block.
 * [parseOne] requires a positive rate and at least one pickup or delivery address.
 * [extractFields] fills whatever it can so the add-load form can show editable boxes.
 */
object FlexibleLoadParser {

    private val tripIdPatterns = listOf(
        Regex("""Trip\s*ID\s*[:\|]?\s*(T-[A-Z0-9]+)""", RegexOption.IGNORE_CASE),
        Regex("""(?:Trip\s*ID|Load\s*(?:ID|#|Number)|Confirmation(?:\s*(?:#|No\.?|Number))?)\s*[:\|]?\s*([A-Z0-9\-]{4,})""", RegexOption.IGNORE_CASE),
        Regex("""\b(T-[A-Z0-9]{6,})\b""", RegexOption.IGNORE_CASE),
    )
    private val ratePatterns = listOf(
        Regex("""Total\s*Rate\s*[:\s]*\$?\s*([\d.,]+)""", RegexOption.IGNORE_CASE),
        Regex("""Line\s*-?\s*Haul(?:\s*Rate)?\s*[:\s]*\$?\s*([\d.,]+)""", RegexOption.IGNORE_CASE),
        Regex("""All[\s-]?in(?:\s*rate)?\s*[:\s]*\$?\s*([\d.,]+)""", RegexOption.IGNORE_CASE),
        Regex("""(?:Rate|Pay|Оплата|Ставка)\s*[:\s]*\$?\s*([\d.,]+)""", RegexOption.IGNORE_CASE),
        Regex("""\$\s*([\d]{2,}[.,]?\d*)"""),
    )
    private val milesPatterns = listOf(
        Regex("""Total\s*Loaded\s*Miles\s*[:\s]*([\d.,]+)""", RegexOption.IGNORE_CASE),
        Regex("""(?:Loaded\s*Miles|Miles|Мил[иь]|Distance)\s*[:\s]*([\d.,]+)""", RegexOption.IGNORE_CASE),
        Regex("""([\d.,]+)\s*mi\b""", RegexOption.IGNORE_CASE),
    )
    private val pickupPatterns = listOf(
        // FIX: \b on From — otherwise "Platform:" / "Freeform:" can steal the pickup
        Regex(
            """(?:Pu[\s\-]*address|Pickup(?:\s*Location)?|\bFrom\b|Origin|Погрузка)\s*[:]\s*(.+)""",
            RegexOption.IGNORE_CASE,
        ),
    )
    private val deliveryPatterns = listOf(
        // FIX: \b on To — otherwise "Total Rate:" / "Auto:" can steal the delivery
        Regex(
            """(?:Del[\s\-]*address|Delivery(?:\s*Location)?|\bTo\b|Destination|Разгрузка)\s*[:]\s*(.+)""",
            RegexOption.IGNORE_CASE,
        ),
    )
    private val cityStateLine = Regex(
        """^[A-Za-z0-9 .#'\-/]{2,40},\s*[A-Za-z]{2}(?:\s+\d{5}(?:-\d{4})?)?$""",
    )
    private val cityStateAnywhere = Regex(
        """\b([A-Z][A-Za-z .'\-]{1,32},\s*[A-Za-z]{2}(?:\s+\d{5}(?:-\d{4})?)?)\b""",
    )
    private val standaloneLabel = Regex(
        """^(?:Trip\s*ID|Load\s*(?:ID|#|Number)|Confirmation(?:\s*(?:#|No\.?|Number))?|Total\s*Rate|Line\s*-?\s*Haul(?:\s*Rate)?|All[\s-]?in(?:\s*rate)?|Rate|Pay|Оплата|Ставка|Total\s*Loaded\s*Miles|Loaded\s*Miles|Miles|Мил[иь]|Distance|Pu[\s\-]*address|Pickup(?:\s*Location)?|Origin|Погрузка|Del[\s\-]*address|Delivery(?:\s*Location)?|Destination|Разгрузка|Date|Load\s*Date)\s*:?\s*$""",
        RegexOption.IGNORE_CASE,
    )

    /**
     * Parses a single load from pasted or OCR text.
     *
     * @return a [Load] when rate > 0 and at least one address is present; otherwise null.
     */
    fun parseOne(rawMessage: String, nowMillis: Long = System.currentTimeMillis()): Load? {
        val draft = extractFields(rawMessage)
        val rate = draft.parsedRate() ?: return null
        if (rate <= 0.0) return null
        if (draft.pointA.isBlank() && draft.pointB.isBlank()) return null

        val miles = ParseUtils.sanitizeLoadedMiles(draft.parsedMiles(), rate)
        val tripId = draft.tripId.ifBlank {
            ManualLoadFactory.generateTripId(nowMillis, draft.pointA, draft.pointB, rate)
        }
        return ManualLoadFactory.build(
            tripId = tripId,
            date = draft.date,
            rate = rate,
            miles = miles,
            pointA = draft.pointA,
            pointB = draft.pointB,
            rawMessage = rawMessage,
            nowMillis = nowMillis,
        )
    }

    /**
     * Pulls whatever load fields OCR / paste text contains, without requiring a complete load.
     * Does not invent a trip id — empty trip id means the form should leave it optional.
     */
    fun extractFields(rawMessage: String): LoadDraftFields {
        val normalized = TelegramStyledTextNormalizer.normalize(
            rawMessage.replace("\r\n", "\n").trim(),
        )
        if (normalized.isBlank()) return LoadDraftFields()
        val text = joinLabelValueLines(normalized)
        when (MessageClassifier.classify(text)) {
            MessageType.PAYCHECK, MessageType.DIESEL -> return LoadDraftFields()
            else -> Unit
        }

        val rate = firstMoney(text)
        val miles = firstMiles(text)?.let { rawMiles ->
            ParseUtils.sanitizeLoadedMiles(rawMiles, rate ?: 0.0)
        }
        val labeledPickup = firstLabeled(text, pickupPatterns)
        val labeledDelivery = firstLabeled(text, deliveryPatterns)
        val (pointA, pointB) = resolvePoints(text, labeledPickup, labeledDelivery)
        val tripId = ParseUtils.firstMatch(text, tripIdPatterns)?.uppercase(Locale.US).orEmpty()
        val date = ParseUtils.normalizeDate(text).takeIf { it.length >= 10 }.orEmpty()

        return LoadDraftFields(
            tripId = tripId,
            date = date,
            rate = LoadDraftFields.formatAmount(rate ?: 0.0),
            miles = LoadDraftFields.formatAmount(miles ?: 0.0),
            pointA = pointA,
            pointB = pointB,
        )
    }

    /** Turns OCR "Total Rate\n$2500" into "Total Rate: $2500" so labeled regexes match. */
    internal fun joinLabelValueLines(text: String): String {
        val lines = text.split('\n')
        if (lines.size < 2) return text
        val out = ArrayList<String>(lines.size)
        var index = 0
        while (index < lines.size) {
            val current = lines[index]
            val next = lines.getOrNull(index + 1)?.trim().orEmpty()
            if (standaloneLabel.matches(current.trim()) && isJoinableValue(next)) {
                val label = current.trim().trimEnd(':').trim()
                out += "$label: $next"
                index += 2
            } else {
                out += current
                index += 1
            }
        }
        return out.joinToString("\n")
    }

    private fun isJoinableValue(next: String): Boolean {
        if (next.isBlank() || next.length > 80) return false
        if (standaloneLabel.matches(next)) return false
        return true
    }

    private fun firstMoney(text: String): Double? =
        ratePatterns.firstNotNullOfOrNull { pattern ->
            pattern.find(text)?.groupValues?.get(1)?.let { ParseUtils.parseMoney(it) }
                ?.takeIf { it > 0.0 }
        }

    private fun firstMiles(text: String): Double? =
        milesPatterns.mapNotNull { pattern ->
            pattern.findAll(text).mapNotNull { match ->
                ParseUtils.parseMiles(match.groupValues[1]).takeIf { it > 0.0 }
            }.maxOrNull()
        }.maxOrNull()

    private val dateOnlyValue = Regex("""^\d{1,2}[./-]\d{1,2}[./-]\d{2,4}$""")

    private fun looksLikeDateOnly(value: String): Boolean = dateOnlyValue.matches(value.trim())

    private fun firstLabeled(text: String, patterns: List<Regex>): String? {
        for (pattern in patterns) {
            val match = pattern.find(text) ?: continue
            val first = match.groupValues[1].trim()
            if (first.isNotBlank() && !looksLikeDateOnly(first)) {
                val extra = followingAddressLines(text, match.range.last)
                return listOf(first, extra).filter { it.isNotBlank() }.joinToString(", ")
            }
        }
        return null
    }

    private fun followingAddressLines(text: String, afterIndex: Int): String {
        val rest = text.substring(afterIndex.coerceAtMost(text.length)).trimStart('\n', '\r')
        val next = rest.lineSequence().firstOrNull()?.trim().orEmpty()
        if (next.isBlank() || next.contains(':')) return ""
        return if (cityStateLine.matches(next)) next else ""
    }

    private fun resolvePoints(
        text: String,
        labeledPickup: String?,
        labeledDelivery: String?,
    ): Pair<String, String> {
        if (!labeledPickup.isNullOrBlank() || !labeledDelivery.isNullOrBlank()) {
            return labeledPickup.orEmpty() to labeledDelivery.orEmpty()
        }
        val fromLines = text.lineSequence()
            .map { it.trim() }
            .filter { cityStateLine.matches(it) }
            .toList()
        val cities = fromLines.ifEmpty {
            cityStateAnywhere.findAll(text).map { it.groupValues[1].trim() }.distinct().toList()
        }
        return when {
            cities.size >= 2 -> cities.first() to cities.last()
            cities.size == 1 -> cities.first() to ""
            else -> "" to ""
        }
    }
}
