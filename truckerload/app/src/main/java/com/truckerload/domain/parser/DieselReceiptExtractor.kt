package com.truckerload.domain.parser

import java.util.Locale

/** Optional fields read from a pump display or paper fuel receipt. Missing values stay null. */
data class DieselReceiptFields(
    val gallons: Double? = null,
    val pricePerGallon: Double? = null,
    val discountPricePerGallon: Double? = null,
    val totalAmount: Double? = null,
    val location: String? = null,
    val vendor: String? = null,
) {
    val hasAnyField: Boolean
        get() = gallons != null ||
            pricePerGallon != null ||
            discountPricePerGallon != null ||
            totalAmount != null ||
            !location.isNullOrBlank() ||
            !vendor.isNullOrBlank()
}

/**
 * Best-effort OCR harvest for diesel gallons / PPG. Unlike [DieselTextParser], this does **not**
 * require a fuel keyword plus a labeled total — pump screens often show only gallons and price.
 */
object DieselReceiptExtractor {

    private val gallonsInline = listOf(
        Regex("""([\d]{1,3}(?:[.,]\d{1,4})?)\s*(?:gal(?:lons?)?|gals?|гл)\b""", RegexOption.IGNORE_CASE),
        Regex(
            """(?:^|[\n\s])(?:gallons?|gals?|sale\s*gals?|volume|объем|галлоны?)\s*[:\-]?\s*\$?\s*([\d]{1,3}(?:[.,]\d{1,4})?)""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
        ),
    )
    private val ppgInline = listOf(
        Regex(
            """(?:price\s*/?\s*gal(?:lon)?s?|price\s*per\s*gal(?:lon)?s?|ppg|\$\s*/\s*gal|цена\s*за\s*гал(?:лон)?)\s*[:\-]?\s*\$?\s*([\d]{1,2}(?:[.,]\d{1,4})?)""",
            RegexOption.IGNORE_CASE,
        ),
        Regex("""(?:Price|PPG|@\s*)\$?\s*([\d.]+)\s*(?:/|\s*per\s*)?\s*gal""", RegexOption.IGNORE_CASE),
        Regex("""\$\s*([\d]{1,2}\.\d{2,3})\s*/\s*gal""", RegexOption.IGNORE_CASE),
    )
    private val discountInline = listOf(
        Regex(
            """(?:disc(?:ount)?(?:ed)?\s*(?:price|ppg)|fleet\s*price|cash\s*price|цена\s*со\s*скидкой)\s*[:\-]?\s*\$?\s*([\d]{1,2}(?:[.,]\d{1,4})?)""",
            RegexOption.IGNORE_CASE,
        ),
    )
    private val totalInline = listOf(
        Regex(
            """(?:Total\s*Amount|Amount\s*Due|Fuel\s*Total|Diesel\s*Total|Sale\s*Amount|Итого)\s*[:\s]*\$?\s*([\d,]+\.?\d*)""",
            RegexOption.IGNORE_CASE,
        ),
        Regex("""^\s*Total\s*[:\s]*\$?\s*([\d,]+\.?\d*)\s*$""", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)),
    )
    private val locationInline = listOf(
        Regex("""(?:Location|Store|Station|АЗС)\s*[:\s]*([^\n]+)""", RegexOption.IGNORE_CASE),
        Regex("""(?:Merchant|Vendor)\s*[:\s]*([^\n]+)""", RegexOption.IGNORE_CASE),
    )
    private val cityState = Regex(
        """\b([A-Z][A-Za-z]+(?:[\s\-'][A-Z][A-Za-z]+)*)\,\s*([A-Z]{2})(?:\s+\d{5}(?:-\d{4})?)?\b""",
    )
    private val gallonsCue = Regex(
        """gallons?|gals?|sale\s*gals?|volume|объем|галлон""",
        RegexOption.IGNORE_CASE,
    )
    private val ppgCue = Regex(
        """price\s*/?\s*gal|price\s*per\s*gal|ppg|\$\s*/\s*g|цена\s*за\s*гал""",
        RegexOption.IGNORE_CASE,
    )
    private val discountCue = Regex(
        """disc(?:ount)?|fleet\s*price|cash\s*price|скидк""",
        RegexOption.IGNORE_CASE,
    )
    private val totalCue = Regex("""total|amount\s*due|итого""", RegexOption.IGNORE_CASE)
    private val numberToken = Regex("""\$?\s*([\d]{1,4}(?:[.,]\d{1,4})?)""")

    private val knownStops = listOf(
        Regex("""love'?s""", RegexOption.IGNORE_CASE) to "Love's Travel Stop",
        Regex("""flying\s*j""", RegexOption.IGNORE_CASE) to "Flying J",
        Regex("""\bpilot\b""", RegexOption.IGNORE_CASE) to "Pilot",
        Regex("""travelcenters|\bta\s*express\b|\bta\b""", RegexOption.IGNORE_CASE) to "TA",
        Regex("""\bpetro\b""", RegexOption.IGNORE_CASE) to "Petro",
        Regex("""speedway""", RegexOption.IGNORE_CASE) to "Speedway",
        Regex("""maverik""", RegexOption.IGNORE_CASE) to "Maverik",
        Regex("""quik\s*trip|\bqt\b""", RegexOption.IGNORE_CASE) to "QuikTrip",
        Regex("""race\s*trac""", RegexOption.IGNORE_CASE) to "RaceTrac",
        Regex("""circle\s*k""", RegexOption.IGNORE_CASE) to "Circle K",
        Regex("""sheetz""", RegexOption.IGNORE_CASE) to "Sheetz",
        Regex("""wawa""", RegexOption.IGNORE_CASE) to "Wawa",
        Regex("""thornton""", RegexOption.IGNORE_CASE) to "Thorntons",
        Regex("""kum\s*[&n]\s*go""", RegexOption.IGNORE_CASE) to "Kum & Go",
        Regex("""sapp\s*bros""", RegexOption.IGNORE_CASE) to "Sapp Bros",
        Regex("""ambest""", RegexOption.IGNORE_CASE) to "Ambest",
    )

    fun extract(text: String): DieselReceiptFields {
        val raw = text.trim()
        if (raw.isBlank()) return DieselReceiptFields()

        var gallons = firstPositive(raw, gallonsInline, ::asGallons)
        var ppg = firstPositive(raw, ppgInline, ::asPpg)
        var discount = firstPositive(raw, discountInline, ::asPpg)
        var total = firstPositive(raw, totalInline) { ParseUtils.parseMoney(it).takeIf(::isTotal) }

        val lines = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }
        harvestAdjacent(lines) { g, p, d, t ->
            if (gallons == null) gallons = g
            if (ppg == null) ppg = p
            if (discount == null) discount = d
            if (total == null) total = t
        }

        val gallonsSoFar = gallons
        val ppgSoFar = ppg
        val totalSoFar = total
        if (gallonsSoFar == null && totalSoFar != null && ppgSoFar != null && ppgSoFar > 0.0) {
            gallons = asGallons(totalSoFar / ppgSoFar)
        }
        val gallonsAfter = gallons
        if (ppgSoFar == null && totalSoFar != null && gallonsAfter != null && gallonsAfter > 0.0) {
            ppg = asPpg(totalSoFar / gallonsAfter)
        }

        val vendor = findVendor(raw)
        val labeledLocation = ParseUtils.firstMatch(raw, locationInline)?.trim()?.takeIf { it.isNotBlank() }
        val cityStateLine = cityState.find(raw)?.value?.trim()
        val location = buildLocation(labeledLocation, vendor, cityStateLine)

        return DieselReceiptFields(
            gallons = gallons,
            pricePerGallon = ppg,
            discountPricePerGallon = discount,
            totalAmount = total,
            location = location,
            vendor = vendor,
        )
    }

    private fun harvestAdjacent(
        lines: List<String>,
        sink: (Double?, Double?, Double?, Double?) -> Unit,
    ) {
        var gallons: Double? = null
        var ppg: Double? = null
        var discount: Double? = null
        var total: Double? = null
        for (i in lines.indices) {
            val line = lines[i]
            val next = lines.getOrNull(i + 1).orEmpty()
            val here = firstNumber(line)
            val below = firstNumber(next)
            when {
                isGallonsLine(line) && below != null -> gallons = gallons ?: asGallons(below)
                isGallonsLine(next) && here != null -> gallons = gallons ?: asGallons(here)
                isPpgLine(line) && below != null -> ppg = ppg ?: asPpg(below)
                isPpgLine(next) && here != null -> ppg = ppg ?: asPpg(here)
                isDiscountLine(line) && below != null -> discount = discount ?: asPpg(below)
                isTotalLine(line) && below != null -> total = total ?: asTotal(below)
            }
        }
        sink(gallons, ppg, discount, total)
    }

    private fun findVendor(text: String): String? {
        for ((pattern, label) in knownStops) {
            if (pattern.containsMatchIn(text)) return label
        }
        return ParseUtils.firstMatch(text, locationInline)?.trim()?.takeIf { it.isNotBlank() }
            ?.takeIf { !it.contains("location", ignoreCase = true) }
    }

    private fun buildLocation(labeled: String?, vendor: String?, cityStateLine: String?): String? {
        val parts = listOfNotNull(
            vendor,
            labeled?.takeIf { vendor == null || !it.contains(vendor, ignoreCase = true) },
            cityStateLine,
        ).map { it.trim() }.filter { it.isNotBlank() }.distinct()
        return parts.joinToString(", ").takeIf { it.isNotBlank() }
    }

    private fun firstPositive(
        text: String,
        patterns: List<Regex>,
        convert: (String) -> Double?,
    ): Double? {
        val raw = ParseUtils.firstMatch(text, patterns) ?: return null
        return convert(raw)
    }

    private fun firstNumber(line: String): Double? {
        val raw = numberToken.find(line)?.groupValues?.getOrNull(1) ?: return null
        return parseNumber(raw)
    }

    private fun isGallonsLine(line: String): Boolean =
        gallonsCue.containsMatchIn(line) && !ppgCue.containsMatchIn(line)

    private fun isPpgLine(line: String): Boolean = ppgCue.containsMatchIn(line)

    private fun isDiscountLine(line: String): Boolean =
        discountCue.containsMatchIn(line) && !isPpgLine(line) && !isGallonsLine(line)

    private fun isTotalLine(line: String): Boolean =
        totalCue.containsMatchIn(line) && !isGallonsLine(line) && !isPpgLine(line)

    private fun parseNumber(raw: String): Double? {
        val cleaned = raw.trim().replace("$", "").replace(" ", "").replace(",", "")
        return cleaned.toDoubleOrNull()?.takeIf { it > 0.0 }
    }

    private fun asGallons(value: Double): Double? = value.takeIf { it in GALLONS_MIN..GALLONS_MAX }
    private fun asGallons(raw: String): Double? = parseNumber(raw)?.let(::asGallons)
    private fun asPpg(value: Double): Double? = value.takeIf { it in PPG_MIN..PPG_MAX }
    private fun asPpg(raw: String): Double? = parseNumber(raw)?.let(::asPpg)
    private fun asTotal(value: Double): Double? = value.takeIf(::isTotal)
    private fun isTotal(value: Double): Boolean = value in TOTAL_MIN..TOTAL_MAX

    fun formatField(value: Double, maxDecimals: Int = 3): String {
        val text = String.format(Locale.US, "%.${maxDecimals}f", value)
        return text.trimEnd('0').trimEnd('.')
    }

    private const val GALLONS_MIN = 0.1
    private const val GALLONS_MAX = 400.0
    private const val PPG_MIN = 0.5
    private const val PPG_MAX = 12.0
    private const val TOTAL_MIN = 1.0
    private const val TOTAL_MAX = 8_000.0
}
