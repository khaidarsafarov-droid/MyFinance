package com.truckerload.domain.parser

import com.truckerload.domain.model.Load
import java.util.Locale

/**
 * Parses broker / carrier rate-confirmation PDFs and similar OCR dumps that are
 * not Amazon Relay blocks. Looks for PO / load ids, estimated rate, miles, and
 * pickup / delivery sections used by Integrity Express and other brokers.
 */
object RateConfirmationLoadParser {

    private val loadDocMarkers = Regex(
        """rate[\s\-]*confirmation|load[\s\-]*confirmation|carrier[\s\-]*rate|""" +
            """load\s*information|estimated\s*rate|IEL\s*PO|pick\s*ups\b|""" +
            """shipper.{0,80}consignee|bill\s*of\s*lading|rate\s*con\b""",
        RegexOption.IGNORE_CASE,
    )

    private val tripIdPatterns = listOf(
        Regex("""IEL\s*PO\s*#\s*:?\s*([A-Z0-9\-]{4,})""", RegexOption.IGNORE_CASE),
        Regex("""(?:\bPO|\bPRO|\bBOL|\bRef(?:erence)?)\s*#\s*:?\s*([A-Z0-9\-]{4,})""", RegexOption.IGNORE_CASE),
        Regex(
            """(?:Load|Confirmation|Trip)\s*(?:ID|#|Number|No\.?)\s*:?\s*([A-Z0-9\-]{4,})""",
            RegexOption.IGNORE_CASE,
        ),
    )

    private val labeledRatePatterns = listOf(
        Regex(
            """Estimated\s*Rate(?:[^$\n]{0,48})\$\s*([\d,]+(?:\.\d{2})?)""",
            RegexOption.IGNORE_CASE,
        ),
        Regex(
            """(?:Total|Carrier|Agreed|Line[\s\-]?Haul|All[\s\-]?in)\s*Rate""" +
                """(?:[^$\n]{0,40})\$?\s*([\d,]+(?:\.\d{2})?)""",
            RegexOption.IGNORE_CASE,
        ),
        Regex("""(?:^|\n)\s*Total\s*:\s*\$\s*([\d,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE),
    )

    private val dollarAmount = Regex("""\$\s*([\d,]+(?:\.\d{2})?)""")
    private val feeLine = Regex(
        """fee|fine|charge|pallet|detention|per\s*day|per\s*pallet|comcheck|unloading|layover""",
        RegexOption.IGNORE_CASE,
    )
    private val milesPattern = Regex("""(?:Total\s*Loaded\s*)?Miles\s*:\s*([\d.,]+)""", RegexOption.IGNORE_CASE)
    private val pickupDatePattern = Regex(
        """Pick[\s\-]*Up\s*:\s*(\d{1,2}[./-]\d{1,2}[./-]\d{2,4})""",
        RegexOption.IGNORE_CASE,
    )
    private val addressLabel = Regex("""Address\s*:\s*(.+)""", RegexOption.IGNORE_CASE)
    private val cityStateZip = Regex(
        """\b([A-Z][A-Za-z .'\-]{1,40},\s*[A-Z]{2}(?:\s+\d{5}(?:-\d{4})?)?)\b""",
    )
    private val pickupSection = Regex(
        """(?:Pick\s*Ups?|Pickup|Shipper|Origin)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val deliverySection = Regex(
        """(?:Deliveries|Delivery|Consignee|Destination|Receiver)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val sectionEnd = Regex(
        """(?:Special\s*Instructions|Page\s+\d|Carrier\s*Rate|By\s+signing|GENERAL\s+CARRIER)""",
        RegexOption.IGNORE_CASE,
    )

    fun looksLike(text: String, fileName: String? = null): Boolean {
        val src = listOfNotNull(fileName?.replace('-', ' '), text).joinToString("\n")
        return loadDocMarkers.containsMatchIn(src)
    }

    fun parseOne(
        rawMessage: String,
        nowMillis: Long = System.currentTimeMillis(),
        fileName: String? = null,
    ): Load? {
        val text = TelegramStyledTextNormalizer.normalize(
            rawMessage.replace("\r\n", "\n").trim(),
        )
        if (text.isBlank()) return null
        if (!looksLike(text, fileName)) return null

        val rate = bestRate(text) ?: return null
        if (rate <= 0.0) return null
        val (pointA, pointB) = extractStops(text)
        if (pointA.isBlank() && pointB.isBlank()) return null

        val miles = milesPattern.find(text)?.groupValues?.get(1)
            ?.let { ParseUtils.parseMiles(it) }
            ?.let { ParseUtils.sanitizeLoadedMiles(it, rate) }
            ?: 0.0
        val tripId = ParseUtils.firstMatch(text, tripIdPatterns)
            ?.uppercase(Locale.US)
            ?.let { token -> if (token.startsWith("PO-") || token.any { it.isLetter() }) token else "PO-$token" }
            .orEmpty()
        val date = pickupDatePattern.find(text)?.groupValues?.get(1)
            ?.let { ParseUtils.normalizeDate(it, referenceMillis = nowMillis) }
            .orEmpty()

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

    internal fun bestRate(text: String): Double? {
        val labeled = labeledRatePatterns.mapNotNull { pattern ->
            pattern.find(text)?.groupValues?.get(1)?.let { ParseUtils.parseMoney(it) }
                ?.takeIf { it > 0.0 }
        }
        labeled.maxOrNull()?.let { return it }

        val counts = linkedMapOf<Double, Int>()
        for (match in dollarAmount.findAll(text)) {
            val line = lineContaining(text, match.range.first) ?: continue
            if (feeLine.containsMatchIn(line)) continue
            val value = ParseUtils.parseMoney(match.groupValues[1]).takeIf { it >= MIN_LOAD_RATE } ?: continue
            counts[value] = (counts[value] ?: 0) + 1
        }
        return counts.maxWithOrNull(compareBy({ it.value }, { it.key }))?.key
    }

    internal fun extractStops(text: String): Pair<String, String> {
        val labeled = addressLabel.findAll(text)
            .map { it.groupValues[1].trim() }
            .filter { it.length >= 8 }
            .distinct()
            .toList()
        if (labeled.size >= 2) return labeled.first() to labeled.last()

        val pickup = addressFromSection(text, pickupSection, deliverySection)
            ?: labeled.firstOrNull()
            ?: firstAddressAfter(text, pickupSection)
        val delivery = addressFromSection(text, deliverySection, sectionEnd)
            ?: labeled.getOrNull(1)
            ?: firstAddressAfter(text, deliverySection)
        if (!pickup.isNullOrBlank() || !delivery.isNullOrBlank()) {
            return pickup.orEmpty() to delivery.orEmpty()
        }
        val cities = cityStateZip.findAll(text)
            .map { it.groupValues[1].trim() }
            .filterNot { isBoilerplateCity(it) }
            .distinct()
            .toList()
        return when {
            cities.size >= 2 -> cities.first() to cities.last()
            cities.size == 1 -> cities.first() to ""
            else -> "" to ""
        }
    }

    private fun addressFromSection(text: String, start: Regex, end: Regex): String? {
        val startMatch = start.find(text) ?: return null
        val rest = text.substring(startMatch.range.first)
        val endMatch = end.find(rest, startIndex = startMatch.value.length)
        val section = rest.substring(0, endMatch?.range?.first ?: rest.length.coerceAtMost(1_200))
        addressLabel.find(section)?.groupValues?.get(1)?.trim()
            ?.takeIf { it.length >= 8 }
            ?.let { return it.take(160) }
        return cityStateZip.find(section)?.groupValues?.get(1)?.trim()
    }

    private fun firstAddressAfter(text: String, start: Regex): String? {
        val match = start.find(text) ?: return null
        val rest = text.substring(match.range.last + 1).take(800)
        return addressLabel.find(rest)?.groupValues?.get(1)?.trim()?.takeIf { it.length >= 8 }?.take(160)
    }

    private fun isBoilerplateCity(city: String): Boolean {
        val lower = city.lowercase(Locale.US)
        return lower.contains("cincinnati") ||
            lower.contains("oasis") ||
            lower.contains("elk grove") ||
            lower.contains("lake forest")
    }

    private fun lineContaining(text: String, index: Int): String? {
        if (index !in text.indices) return null
        val start = text.lastIndexOf('\n', index - 1).let { if (it < 0) 0 else it + 1 }
        val end = text.indexOf('\n', index).let { if (it < 0) text.length else it }
        return text.substring(start, end).trim()
    }

    private const val MIN_LOAD_RATE = 100.0
}
