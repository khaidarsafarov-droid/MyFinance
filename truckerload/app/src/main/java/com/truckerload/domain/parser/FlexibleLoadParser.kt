package com.truckerload.domain.parser

import com.truckerload.domain.model.Load
import java.util.Locale

/**
 * Fallback parser for pasted / OCR text that is not a full Amazon Relay block.
 * Requires a positive rate and at least one pickup or delivery address.
 * Generates a trip id when the message has none (unlike [LoadMessageParser]).
 */
object FlexibleLoadParser {

    private val tripIdPatterns = listOf(
        Regex("""Trip\s*ID\s*[:\|]?\s*(T-[A-Z0-9]+)""", RegexOption.IGNORE_CASE),
        Regex("""Trip\s*ID\s*[:\|]?\s*([A-Z0-9\-]{4,})""", RegexOption.IGNORE_CASE),
        Regex("""\b(T-[A-Z0-9]{6,})\b""", RegexOption.IGNORE_CASE),
    )
    private val ratePatterns = listOf(
        Regex("""Total\s*Rate\s*[:\s]*\$?\s*([\d.,]+)""", RegexOption.IGNORE_CASE),
        Regex("""(?:Rate|Pay|Оплата|Ставка)\s*[:\s]*\$?\s*([\d.,]+)""", RegexOption.IGNORE_CASE),
        Regex("""\$\s*([\d]{2,}[.,]?\d*)"""),
    )
    private val milesPatterns = listOf(
        Regex("""Total\s*Loaded\s*Miles\s*[:\s]*([\d.,]+)""", RegexOption.IGNORE_CASE),
        Regex("""(?:Miles|Мил[иь])\s*[:\s]*([\d.,]+)""", RegexOption.IGNORE_CASE),
        Regex("""([\d.,]+)\s*mi\b""", RegexOption.IGNORE_CASE),
    )
    private val pickupPatterns = listOf(
        Regex("""(?:Pu-address|Pickup|From|Origin|Погрузка)\s*[:]\s*(.+)""", RegexOption.IGNORE_CASE),
    )
    private val deliveryPatterns = listOf(
        Regex("""(?:Del-address|Delivery|To|Destination|Разгрузка)\s*[:]\s*(.+)""", RegexOption.IGNORE_CASE),
    )
    private val cityStateLine = Regex(
        """^[A-Za-z0-9 .#'\-/]{2,40},\s*[A-Z]{2}(?:\s+\d{5})?$""",
    )

    fun parseOne(rawMessage: String, nowMillis: Long = System.currentTimeMillis()): Load? {
        val text = TelegramStyledTextNormalizer.normalize(
            rawMessage.replace("\r\n", "\n").trim(),
        )
        if (text.isBlank()) return null
        when (MessageClassifier.classify(text)) {
            MessageType.PAYCHECK, MessageType.DIESEL -> return null
            else -> Unit
        }

        val rate = firstMoney(text) ?: return null
        if (rate <= 0.0) return null
        val miles = firstMiles(text)?.let { ParseUtils.sanitizeLoadedMiles(it, rate) } ?: 0.0
        val labeledPickup = firstLabeled(text, pickupPatterns)
        val labeledDelivery = firstLabeled(text, deliveryPatterns)
        val (pointA, pointB) = resolvePoints(text, labeledPickup, labeledDelivery)
        if (pointA.isBlank() && pointB.isBlank()) return null

        val tripId = ParseUtils.firstMatch(text, tripIdPatterns)?.uppercase(Locale.US)
            ?: ManualLoadFactory.generateTripId(nowMillis, pointA, pointB, rate)
        val date = ParseUtils.normalizeDate(text).takeIf { it.length >= 10 }.orEmpty()

        return ManualLoadFactory.build(
            tripId = tripId,
            date = date,
            rate = rate,
            miles = miles,
            pointA = pointA,
            pointB = pointB,
            rawMessage = rawMessage,
            nowMillis = nowMillis,
        )
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

    private fun firstLabeled(text: String, patterns: List<Regex>): String? {
        for (pattern in patterns) {
            val match = pattern.find(text) ?: continue
            val first = match.groupValues[1].trim()
            if (first.isNotBlank()) {
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
        val cities = text.lineSequence()
            .map { it.trim() }
            .filter { cityStateLine.matches(it) }
            .toList()
        return when {
            cities.size >= 2 -> cities.first() to cities.last()
            cities.size == 1 -> cities.first() to ""
            else -> "" to ""
        }
    }
}
