@file:Suppress("MaxLineLength", "ReturnCount", "MagicNumber")

package com.truckerload.domain.parser

import java.util.Locale

/**
 * Layout-tolerant extraction for rate confirmations and load-board text (pdftotext -layout,
 * OCR, chat). Works line by line: money is picked by label tier and never from penalty,
 * insurance, per-mile or fee lines; miles ignore deadhead and per-mile rates; the load id must
 * contain a digit; stops are found under pickup/delivery section headers.
 * Every field is nullable so the caller can leave unknown ones empty.
 */
internal object RateConTextExtractor {

    data class Fields(
        val rate: Double?,
        val miles: Double?,
        val loadId: String?,
        val pickup: String?,
        val delivery: String?,
        val date: java.time.LocalDate? = null,
    )

    /** `$3.63/mi`, `(1.51/mi loaded)`, `$2.05 per mile` are rates per mile, never the load rate. */
    private val perMile = Regex(
        "\\(?\\$?\\s*\\d+(?:\\.\\d+)?\\s*(?:/\\s*mi(?:les?)?|per\\s*mile)(?:\\s*loaded)?\\)?",
        RegexOption.IGNORE_CASE,
    )

    fun extract(raw: String): Fields {
        val lines = repairOcrMoney(raw).lines().map { it.trimEnd() }
        return Fields(
            rate = findRate(lines.map { perMile.replace(it, " ") }),
            miles = findMiles(lines),
            loadId = findLoadId(raw),
            pickup = findStop(lines, pickupHeader, last = false) ?: firstAndLastPlace(raw).first,
            delivery = findStop(lines, deliveryHeader, last = true) ?: firstAndLastPlace(raw).second,
        )
    }

    // ---- money -------------------------------------------------------------------------------

    private val amount = Regex("""\$?\s*(\d{1,3}(?:,\d{3})+(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?)""")
    private val badMoneyLine = Regex(
        "no\\s+update|photo|chargeback|cancel|lumper|штраф|multa|shtraf|jarima|insurance|liabil|cargo|\\bvalue\\b|\\bfine[ds]?\\b|penalt|\\blate\\b|deduct|reduc|\\bfee\\b|advance|" +
            "quick\\s*pay|detention|tonu|\\bper\\s*(?:hour|hr|day|occurrence|every|stop)|/\\s*(?:hr|day)|deadhead|incentive|bonus|reserve|" +
            "balance|discount|factor|\\bmax|\\bcap\\b|limit|up to|\\beach\\b|\\bnet\\s*\\d|subtotal|weight|wgt|" +
            "phone|\\bfax\\b|\\bmc\\b|\\bdot\\b|zip|\\bpo\\b|\\bbol\\b|\\bpage\\b|pallet|pieces|count",
        RegexOption.IGNORE_CASE,
    )
    private val totalLabel = Regex(
        "(?<![a-z])total(?!\\s*(?:weight|wt|wgt|mi\\b|miles|stops|pieces|pallets|count|distance))|" +
            "carrier\\s*(?:freight\\s*)?(?:pay|total)|agreed|all[\\s-]?in|payout|net\\s*pay|estimated\\s*rate|" +
            "gross\\s*pay|\\bstavka|ставка|рейт|цена",
        RegexOption.IGNORE_CASE,
    )
    private val rateLabel = Regex(
        "\\brates?\\b|line\\s*-?\\s*haul|flat\\s*rate|base\\s*rate|freight\\s*(?:charges|pay)|\\bpay\\b|" +
            "\\bamount\\b|\\$\\s*\\d[\\d,]*\\.?\\d*\\s*(?:usd|dollars)",
        RegexOption.IGNORE_CASE,
    )
    private const val MIN_RATE = 100.0
    private const val MAX_RATE = 35_000.0

    private fun amountsOn(line: String, itemSum: Double = 0.0): List<Double> = amount.findAll(line).mapNotNull { m ->
        val hasDollar = m.value.contains('$')
        val parsed = m.groupValues[1].replace(",", "").toDoubleOrNull() ?: return@mapNotNull null
        // OCR reads "$" as "5" ("51,850" for "$1,850"): checked against the itemised lines BEFORE the cap drops it.
        val v = leadingFiveFix(parsed, itemSum)
        val frac = m.groupValues[1].contains('.')
        v.takeIf { it in MIN_RATE..MAX_RATE && (hasDollar || frac || line.contains(Regex("rate|total|pay|amount", RegexOption.IGNORE_CASE))) }
    }.toList()

    private const val FIVE_THOUSANDS = 50_000.0
    private const val SUM_TOLERANCE = 0.5

    private fun leadingFiveFix(value: Double, itemSum: Double): Double {
        val without = value - FIVE_THOUSANDS
        val looksFive = value in FIVE_THOUSANDS..(FIVE_THOUSANDS + MAX_RATE) && without >= MIN_RATE
        return if (looksFive && itemSum > 0.0 && kotlin.math.abs(without - itemSum) <= SUM_TOLERANCE) without else value
    }

    private val itemLabel = Regex("line\\s*-?\\s*haul|flat\\s*rate|base\\s*rate|fuel|\\bfsc\\b|surcharge|accessorial", RegexOption.IGNORE_CASE)

    /** Sum of the itemised charge lines (line haul, fuel, accessorials): what a printed total should equal. */
    private fun itemisedSum(lines: List<String>): Double = lines
        .filter { itemLabel.containsMatchIn(it) && !totalLabel.containsMatchIn(it) && !badMoneyLine.containsMatchIn(it) }
        .sumOf { line -> amount.findAll(line).mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }.lastOrNull() ?: 0.0 }

    private val ocrMoneyToken = Regex("(?<![A-Za-z0-9])[S§$]\\s?[0-9OolIBZ][0-9OolIBZ,.]*")
    private val ocrDigits = mapOf('O' to '0', 'o' to '0', 'l' to '1', 'I' to '1', 'B' to '8', 'Z' to '2')

    /** `S1,75O.OO` -> `$1,750.00`: a leading S/§ and look-alike letters inside an amount token. */
    internal fun repairOcrMoney(raw: String): String = ocrMoneyToken.replace(raw) { m ->
        val body = m.value.drop(1).trim()
        val fixed = body.map { ocrDigits[it] ?: it }.joinToString("")
        if (fixed.any(Char::isDigit) && fixed.count { it.isDigit() } >= body.count { it.isDigit() }) "$" + fixed else m.value
    }

    private fun findRate(lines: List<String>): Double? {
        val good = lines.filterNot { badMoneyLine.containsMatchIn(it) }
        val itemSum = itemisedSum(lines)
        val totals = good.filter { totalLabel.containsMatchIn(it) }.mapNotNull { amountsOn(it, itemSum).lastOrNull() }
        if (totals.isNotEmpty()) return mostCommonThenMax(totals)
        val labeled = good.filter { rateLabel.containsMatchIn(it) }.mapNotNull { amountsOn(it).lastOrNull() }
        if (labeled.isNotEmpty()) return labeled.first()
        return good.flatMap { line -> amount.findAll(line).filter { it.value.contains('$') }.map { it.groupValues[1] } }
            .mapNotNull { it.replace(",", "").toDoubleOrNull() }
            .filter { it in MIN_RATE..MAX_RATE }
            .maxOrNull()
    }

    private fun mostCommonThenMax(values: List<Double>): Double =
        values.groupBy { it }.entries.sortedWith(compareByDescending<Map.Entry<Double, List<Double>>> { it.value.size }.thenByDescending { it.key }).first().key

    // ---- miles -------------------------------------------------------------------------------

    private val badMilesLine = Regex("deadhead|total\\s*mi\\b|empty|per\\s*mile|/\\s*mi\\b|\\bx\\s*\\.?\\d", RegexOption.IGNORE_CASE)
    private val milesLabeled = Regex(
        "(?:loaded\\s*mi(?:les)?|distance|mileage|miles?)\\s*[:\\-]?\\s*(\\d[\\d,]*\\.?\\d*)",
        RegexOption.IGNORE_CASE,
    )
    private val milesSuffix = Regex("(?<![\\d$.,/])(\\d[\\d,]*\\.?\\d*)\\s*(?:loaded\\s*)?mi(?:les)?\\b", RegexOption.IGNORE_CASE)
    private const val MIN_MILES = 20.0
    private const val MAX_MILES = 5_000.0

    private fun findMiles(lines: List<String>): Double? {
        val good = lines.filterNot { badMilesLine.containsMatchIn(it) }
        for (regex in listOf(milesLabeled, milesSuffix)) {
            val hit = good.firstNotNullOfOrNull { line ->
                regex.findAll(line).mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
                    .firstOrNull { it in MIN_MILES..MAX_MILES }
            }
            if (hit != null) return hit
        }
        return null
    }

    // ---- load id -----------------------------------------------------------------------------

    private const val ID = """([A-Z0-9][A-Z0-9\-]{3,19})"""
    private val idPatterns = listOf(
        Regex("""Trip\s*ID\s*[:\s]*(T-[A-Z0-9]+)""", RegexOption.IGNORE_CASE),
        Regex("""load\s*(?:confirmation\s*)?(?:number|no\.?|#|id)?[\s:#\-]*$ID""", RegexOption.IGNORE_CASE),
        Regex("""(?:trip|tour|order|confirmation|shipment|pro|movement)\s*(?:id|number|no\.?|#)?[\s:#\-]*$ID""", RegexOption.IGNORE_CASE),
        Regex("""(?:VRID|PO|BOL|ref(?:erence)?)\s*(?:number|no\.?|#)?[\s:#\-]*$ID""", RegexOption.IGNORE_CASE),
    )

    private val idAbove = Regex("""(?m)^\s*([A-Z0-9][A-Z0-9\-]{3,19})\s*\n\s*(?:trip|load|order)\s*(?:number|no\.?|#|id)\b""", RegexOption.IGNORE_CASE)

    private fun findLoadId(text: String): String? {
        idAbove.find(text)?.groupValues?.get(1)?.takeIf { v -> v.any { it.isDigit() } }?.let { return it.uppercase(Locale.US) }
        for (pattern in idPatterns) {
            val hit = pattern.findAll(text).map { it.groupValues[1].uppercase(Locale.US) }
                .firstOrNull { id -> id.any { it.isDigit() } && id.count { it.isDigit() } >= 3 }
            if (hit != null) return hit
        }
        return null
    }

    // ---- stops -------------------------------------------------------------------------------

    private val pickupHeader = Regex(
        "^\\W*(?:\\d+\\W+)?(?:shipper|pick[\\s-]*ups?\\b|pu\\b|p/u|pick\\b|origin|loading|stop\\s*\\d+\\s*[:\\-]?\\s*pick)",
        RegexOption.IGNORE_CASE,
    )
    private val deliveryHeader = Regex(
        "^\\W*(?:\\d+\\W+)?(?:consignee|receiver|deliver(?:y|ies)|drop\\b|del\\b|destination|unloading|final\\s*deliv|" +
            "stop\\s*\\d+\\s*[:\\-]?\\s*deliv)",
        RegexOption.IGNORE_CASE,
    )
    private val stateCodes = StateCodes.all
    private val cityStateZip = Regex(
        "([A-Za-z][A-Za-z.'\\-]*(?: [A-Za-z][A-Za-z.'\\-]*){0,2}),? ([A-Z]{2})\\s+(\\d{5}(?:-\\d{4})?|[A-Z]\\d[A-Z]\\s?\\d[A-Z]\\d)\\b",
    )
    private val cityComma = Regex("([A-Za-z][A-Za-z.'\\-]*(?: [A-Za-z][A-Za-z.'\\-]*){0,2}), ?([A-Z]{2})\\b")
    private const val WINDOW = 14

    private fun cityStateOn(line: String): String? {
        for (regex in listOf(cityStateZip, cityComma)) {
            val m = regex.findAll(line).firstOrNull { it.groupValues[2] in stateCodes } ?: continue
            return "${m.groupValues[1].trim()}, ${m.groupValues[2]}"
        }
        return null
    }

    private fun findStop(lines: List<String>, header: Regex, last: Boolean): String? {
        val other = if (header === pickupHeader) deliveryHeader else pickupHeader
        val starts = lines.indices.filter { header.containsMatchIn(lines[it]) || other.containsMatchIn(lines[it]) }
        val rows = starts.filter { header.containsMatchIn(lines[it]) }.map { i ->
            val end = minOf(lines.size, i + WINDOW, starts.firstOrNull { it > i } ?: lines.size)
            (i until end).mapNotNull { cityStateOn(lines[it]) }
        }.filter { it.isNotEmpty() }
        return if (last) rows.lastOrNull()?.last() else rows.firstOrNull()?.first()
    }

    /** First and last `City, ST ZIP` in reading order: the fallback when no section header exists. */
    fun firstAndLastPlace(raw: String): Pair<String?, String?> {
        val places = raw.lines().flatMap { line ->
            cityStateZip.findAll(line).filter { it.groupValues[2] in stateCodes }
                .map { "${it.groupValues[1].trim()}, ${it.groupValues[2]}" }.toList()
        }.distinct()
        return places.firstOrNull() to places.lastOrNull().takeIf { places.size > 1 }
    }
}
