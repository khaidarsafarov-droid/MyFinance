package com.truckerload.domain.share

import com.truckerload.domain.parser.ParseUtils
import java.util.Locale

/**
 * On-device score for "is this shared text a load?".
 * A chat, a store receipt, a fuel receipt, or a photo caption stays under the bar.
 */
data class ShareLoadReading(
    val points: Int,
    val keywordHits: Int,
    val hasRate: Boolean,
    val rate: Double?,
    val miles: Double?,
    val pickup: String,
    val delivery: String,
    val dateRaw: String,
    val loadNumber: String,
    val broker: String,
    val negative: Int,
) {
    val locationCount: Int
        get() = (if (pickup.isNotBlank()) 1 else 0) + (if (delivery.isNotBlank()) 1 else 0)

    val accept: Boolean
        get() = ShareLoadClassifier.accepts(this)
}

object ShareLoadClassifier {

    fun read(text: String, fileName: String? = null): ShareLoadReading {
        val source = text.replace("\r\n", "\n").trim()
        val keywordHits = keywordHits(source, fileName)
        val rate = rateOf(source)
        val pickupLabeled = labeledLocation(source, pickupLabel)
        val deliveryLabeled = labeledLocation(source, deliveryLabel)
        val (pickup, delivery) = fillCities(source, pickupLabeled, deliveryLabeled)
        val dateRaw = dateOf(source)
        val loadNumber = loadNumberOf(source)
        val miles = milesOf(source)
        val broker = brokerOf(source)
        val negative = negativeOf(source)
        var points = keywordHits * 2
        if (rate != null) points += 4
        points += listOf(pickup, delivery).count { it.isNotBlank() } * 3
        if (dateRaw.isNotBlank()) points += 2
        if (loadNumber.isNotBlank()) points += 2
        if (miles != null) points += 1
        return ShareLoadReading(
            points = points,
            keywordHits = keywordHits,
            hasRate = rate != null,
            rate = rate,
            miles = miles,
            pickup = pickup,
            delivery = delivery,
            dateRaw = dateRaw,
            loadNumber = loadNumber,
            broker = broker,
            negative = negative,
        )
    }

    fun accepts(reading: ShareLoadReading): Boolean {
        if (reading.keywordHits < 2) return false
        if (reading.points < ACCEPT_POINTS) return false
        if (reading.negative >= 4 && reading.keywordHits < 4) return false
        val concrete = reading.hasRate || reading.locationCount > 0 || reading.loadNumber.isNotBlank()
        if (!concrete) return false
        val groups = listOf(
            reading.hasRate,
            reading.locationCount > 0,
            reading.dateRaw.isNotBlank(),
            reading.loadNumber.isNotBlank(),
            reading.keywordHits >= 2,
        ).count { it }
        if (groups >= 3) return true
        if (reading.hasRate && reading.locationCount >= 2 && reading.keywordHits >= 2) return true
        return reading.locationCount >= 2 && reading.keywordHits >= 3
    }

    private fun keywordHits(text: String, fileName: String?): Int {
        var hits = KEYWORD_GROUPS.count { it.containsMatchIn(text) }
        val name = fileName?.replace('-', ' ')?.replace('_', ' ').orEmpty()
        if (hits < KEYWORD_GROUPS.size && name.isNotBlank() && FILE_NAME_LOAD.containsMatchIn(name)) {
            if (!KEYWORD_GROUPS[0].containsMatchIn(text)) hits += 1
        }
        return hits
    }

    private fun rateOf(text: String): Double? {
        val labeled = labeledRate.mapNotNull { pattern ->
            pattern.find(text)?.groupValues?.get(1)?.let { money(it) }
        }
        labeled.maxOrNull()?.let { return it }
        var best = 0.0
        for (match in dollarAmount.findAll(text)) {
            val line = lineAt(text, match.range.first)
            if (feeLine.containsMatchIn(line)) continue
            val value = money(match.groupValues[1]) ?: continue
            if (value > best) best = value
        }
        return best.takeIf { it >= MIN_RATE }
    }

    private fun money(raw: String): Double? =
        ParseUtils.parseMoney(raw).takeIf { it >= MIN_RATE }

    private fun labeledLocation(text: String, label: Regex): String {
        for (match in label.findAll(text)) {
            val location = locationFrom(match.groupValues[1].trim())
            if (location.isNotBlank()) return location
        }
        return ""
    }

    private fun locationFrom(value: String): String {
        cityState.find(value)?.groupValues?.get(1)?.trim()?.takeIf { !boilerplate(it) }?.let {
            return it.take(160)
        }
        val street = value.contains(Regex("""\d""")) && value.length >= 8
        val zip = Regex("""\b\d{5}(?:-\d{4})?\b""").containsMatchIn(value)
        if ((street || zip) && value.length >= 8 && !boilerplate(value)) {
            return value.take(160)
        }
        return ""
    }

    private fun fillCities(text: String, pickup: String, delivery: String): Pair<String, String> {
        if (pickup.isNotBlank() && delivery.isNotBlank()) return pickup to delivery
        val cities = cityState.findAll(text)
            .map { it.groupValues[1].trim() }
            .filterNot { boilerplate(it) }
            .distinct()
            .toList()
        val origin = pickup.ifBlank { cities.firstOrNull().orEmpty() }
        val dest = when {
            delivery.isNotBlank() -> delivery
            cities.size >= 2 && !cities.last().equals(origin, ignoreCase = true) -> cities.last()
            else -> ""
        }
        if (origin.equals(dest, ignoreCase = true)) return origin to ""
        return origin to dest
    }

    private fun boilerplate(city: String): Boolean {
        val lower = city.lowercase(Locale.US)
        return BOILERPLATE.any { lower.contains(it) }
    }

    private fun dateOf(text: String): String {
        val labeled = labeledDate.find(text)?.groupValues?.get(1).orEmpty()
        if (labeled.isNotBlank()) return labeled.trim()
        return isoDate.find(text)?.value.orEmpty()
    }

    private fun loadNumberOf(text: String): String {
        for (pattern in loadNumberPatterns) {
            val token = pattern.find(text)?.groupValues?.get(1)?.trim().orEmpty()
            if (plausibleLoadNumber(token)) return token.uppercase(Locale.US)
        }
        return ""
    }

    private fun plausibleLoadNumber(token: String): Boolean {
        if (token.length !in 4..24) return false
        if (!token.any { it.isDigit() }) return false
        return !Regex("""^\d{1,2}[./-]\d{1,2}[./-]\d{2,4}$""").matches(token)
    }

    private fun milesOf(text: String): Double? {
        val match = milesPattern.find(text) ?: return null
        val raw = match.groupValues.drop(1).firstOrNull { it.isNotBlank() } ?: return null
        val miles = ParseUtils.parseMiles(raw)
        return miles.takeIf { it in 1.0..6_000.0 }
    }

    private fun brokerOf(text: String): String {
        val raw = brokerPattern.find(text)?.groupValues?.get(1)?.trim().orEmpty()
        if (raw.isBlank()) return ""
        val cut = raw
            .substringBefore("\n")
            .substringBefore(" MC")
            .substringBefore("MC#")
            .trim()
            .trimEnd(',', ';', '.', ' ')
        val collapsed = Regex("""\s+""").replace(cut, " ")
        if (collapsed.length !in 2..60) return ""
        if (collapsed.count { it == ' ' } > 8) return ""
        return collapsed
    }

    private fun negativeOf(text: String): Int =
        NEGATIVE_GROUPS.sumOf { pattern -> if (pattern.containsMatchIn(text)) 4 else 0 }

    private fun lineAt(text: String, index: Int): String {
        if (index !in text.indices) return ""
        val start = text.lastIndexOf('\n', index - 1).let { if (it < 0) 0 else it + 1 }
        val end = text.indexOf('\n', index).let { if (it < 0) text.length else it }
        return text.substring(start, end)
    }

    private val KEYWORD_GROUPS = listOf(
        Regex(
            """rate[\s\-]*confirmation|load[\s\-]*confirmation|load[\s\-]*tender|""" +
                """\brate\s*con\b|dispatch(?:\s+sheet)?|подтверждени[ея]\s+ставк""",
            RegexOption.IGNORE_CASE,
        ),
        Regex(
            """\bload\s*(?:#|number|no\b|id\b)|trip\s*id|\b(?:PO|PRO|BOL)\s*#|""" +
                """номер\s+груза|номер\s+рейса""",
            RegexOption.IGNORE_CASE,
        ),
        Regex("""\bshippers?\b|\bгрузоотправител""", RegexOption.IGNORE_CASE),
        Regex("""\bconsignees?\b|\bгрузополучател""", RegexOption.IGNORE_CASE),
        Regex(
            """\bpick[\s\-]*ups?\b|pu[\s\-]*address|\borigin\b|погрузк""",
            RegexOption.IGNORE_CASE,
        ),
        Regex(
            """\bdeliver(?:y|ies)\b|del[\s\-]*address|\bdestination\b|разгрузк|выгрузк""",
            RegexOption.IGNORE_CASE,
        ),
        Regex("""\bbrokers?\b|\bброкер""", RegexOption.IGNORE_CASE),
        Regex("""\bMC\s*#|\bMC\s*number\b""", RegexOption.IGNORE_CASE),
        Regex("""\bcarriers?\b|\bперевозчик""", RegexOption.IGNORE_CASE),
    )

    private val FILE_NAME_LOAD = Regex(
        """rate\s*confirmation|ratecon|load\s*tender|load\s*confirmation|dispatch""",
        RegexOption.IGNORE_CASE,
    )

    private val labeledRate = listOf(
        Regex(
            """(?:Estimated\s*Rate|Total\s*Rate|Line[\s\-]*Haul|All[\s\-]?in(?:\s*Rate)?|""" +
                """Carrier\s*Rate|Agreed\s*Rate|Ставка)\s*:?\s*\$?\s*([\d][\d,]*(?:\.\d{2})?)""",
            RegexOption.IGNORE_CASE,
        ),
        Regex(
            """(?:^|\n)\s*Rate\s*:?\s*\$?\s*([\d][\d,]*(?:\.\d{2})?)""",
            RegexOption.IGNORE_CASE,
        ),
    )

    private val dollarAmount = Regex("""\$\s*([\d][\d,]*\.\d{2})""")
    private val feeLine = Regex(
        """fee|fine|pallet|detention|surcharge|per\s*day|layover|comcheck""",
        RegexOption.IGNORE_CASE,
    )

    private val pickupLabel = Regex(
        """(?:Pu[\s\-]*address|Pickup(?:\s*(?:Location|City))?|Origin|Shipper|Погрузка)\s*:\s*(.+)""",
        RegexOption.IGNORE_CASE,
    )
    private val deliveryLabel = Regex(
        """(?:Del[\s\-]*address|Delivery(?:\s*(?:Location|City))?|Destination|Consignee|""" +
            """Разгрузка|Выгрузка)\s*:\s*(.+)""",
        RegexOption.IGNORE_CASE,
    )
    private val cityState = Regex(
        """\b([A-Z][A-Za-z.'\-]+(?:\s+[A-Z][A-Za-z.'\-]+){0,3},\s*[A-Z]{2}""" +
            """(?:\s+\d{5}(?:-\d{4})?)?)\b""",
    )
    private val labeledDate = Regex(
        """(?:Pickup\s*Date|Delivery\s*Date|Pick[\s\-]*Up|Ship\s*Date|Date|Дата)\s*:\s*""" +
            """(\d{4}-\d{2}-\d{2}|\d{1,2}[./-]\d{1,2}[./-]\d{2,4})""",
        RegexOption.IGNORE_CASE,
    )
    private val isoDate = Regex("""\b\d{4}-\d{2}-\d{2}\b""")
    private val loadNumberPatterns = listOf(
        Regex(
            """\b(?:Load|Trip|Confirmation)\s*(?:ID|#|Number|No\.?)\s*[:#]?\s*([A-Z0-9][A-Z0-9\-]{3,})""",
            RegexOption.IGNORE_CASE,
        ),
        Regex("""\b(?:PO|PRO|BOL)\s*#\s*[:#]?\s*([A-Z0-9][A-Z0-9\-]{3,})""", RegexOption.IGNORE_CASE),
        Regex("""\b(T-[A-Z0-9]{6,})\b""", RegexOption.IGNORE_CASE),
        Regex("""Номер\s+груза\s*:\s*([A-Za-z0-9][A-Za-z0-9\-]{2,})""", RegexOption.IGNORE_CASE),
    )
    private val milesPattern = Regex(
        """(?:Total\s*Loaded\s*)?(?:Miles|Мил[иь])\s*:?\s*([\d,]+(?:\.\d+)?)|""" +
            """([\d,]+(?:\.\d+)?)\s*mi\b""",
        RegexOption.IGNORE_CASE,
    )
    private val brokerPattern = Regex(
        """(?:Broker(?:age)?|Брокер)\s*:\s*([^\n]{2,80})""",
        RegexOption.IGNORE_CASE,
    )
    private val NEGATIVE_GROUPS = listOf(
        Regex(
            """fuel\s*receipt|pump\s*total|\bgallons?\b|\bdiesel\b|\bulsd\b|#2\s*diesel|дизел|топлив""",
            RegexOption.IGNORE_CASE,
        ),
        Regex(
            """thank you for shopping|\bsubtotal\b|\bcashier\b|\bvisa\b|\bmastercard\b|change due""",
            RegexOption.IGNORE_CASE,
        ),
        Regex(
            """pay\s*stub|net\s*pay|driver\s*settlement|\bgrand\s*total\b|pay\s*period""",
            RegexOption.IGNORE_CASE,
        ),
    )
    private val BOILERPLATE = listOf("cincinnati", "elk grove", "lake forest")

    private const val ACCEPT_POINTS = 12
    private const val MIN_RATE = 100.0
}
