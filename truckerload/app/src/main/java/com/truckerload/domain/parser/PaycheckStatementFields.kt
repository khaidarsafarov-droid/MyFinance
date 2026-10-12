package com.truckerload.domain.parser

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Cross-checks a driver settlement: work dates, unlabeled payee name,
 * miles, and whether loads-net minus deductions equals the printed take-home.
 */
internal object PaycheckStatementFields {

    private val moneyNumber =
        """([\d]{1,3}(?:,\d{3})+(?:\.\d{2})?|[\d]+\.\d{2})"""
    private val moneyToken = Regex("""[${'$'}]?\s*$moneyNumber""")
    private val settlementDatePattern = Regex(
        """Settlement\s*Date\s*[:\s]*([^\n]+)""",
        RegexOption.IGNORE_CASE,
    )
    private val deductionsHeader = Regex("""(?i)\bDeductions\b""")
    private val summaryStart = Regex(
        """(?i)Grand\s*Tota[l1I]|Settlement\s*Summary""",
    )
    private val dateToken = Regex("""\b(\d{1,2}[./-]\d{1,2}[./-]\d{2,4})\b""")
    private val milesTotal = Regex(
        """Miles\s*Total\s*:?\s*([\d,]+(?:\.\d+)?)""",
        RegexOption.IGNORE_CASE,
    )
    private val plainTotal = Regex(
        """(?im)^[^\n]{0,24}\bTotal\b\s*:?\s*[${'$'}]?\s*$moneyNumber""",
    )
    private val skipTotal = Regex(
        """grand|gross|miles|deduct|load|order""",
        RegexOption.IGNORE_CASE,
    )
    private val tripLine = Regex(
        """(?im)^[^\n]*\b(?:T-[A-Z0-9]{6,}|[0-9]{2,}[A-Z][A-Z0-9]{4,})[^\n]*""",
    )
    private val percentOnLine = Regex("""%|percent""", RegexOption.IGNORE_CASE)
    private val personLine = Regex(
        """^[A-Za-z][A-Za-z'.-]*(?:\s+[A-Za-z][A-Za-z'.-]*){1,3}$""",
    )
    private val notAPerson = Regex(
        """\b(LLC|INC|CORP|LTD|GROUP|LOGISTICS|COMPANY|TRUCKING|TRANSPORT|""" +
            """SETTLEMENT|STATEMENT|DRIVER|PAYEE|TOTAL|SUMMARY|DEDUCTION|PAY)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val totalDeductionsLabel = Regex(
        """Total\s*Deductions[^\d${'$'}]{0,40}[${'$'}]?\s*$moneyNumber""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    fun settlementDate(text: String): String? =
        settlementDatePattern.find(text)?.groupValues?.getOrNull(1)
            ?.let { ParseUtils.normalizeDate(it) }
            ?.takeIf { it.length >= 10 }

    /** Earliest and latest load dates. Settlement and deduction dates stay out. */
    fun loadDateSpan(text: String): Pair<String, String>? {
        val cut = deductionsHeader.find(text)?.range?.first ?: text.length
        val head = text.substring(0, cut).replace(settlementDatePattern, "")
        val dates = dateToken.findAll(head)
            .map { ParseUtils.normalizeDate(it.value) }
            .filter { it.length >= 10 }
            .distinct()
            .sorted()
            .toList()
        val start = dates.firstOrNull() ?: return null
        return start to dates.last()
    }

    fun unlabeledDriver(text: String): String? {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val anchor = lines.indexOfLast { line ->
            line.contains("Settlement Date", ignoreCase = true) ||
                line.contains("Payee ID", ignoreCase = true)
        }
        if (anchor < 0) return null
        return lines.drop(anchor + 1).take(HEADER_NAME_WINDOW).firstOrNull(::isPersonLine)
    }

    fun miles(text: String): Double? =
        milesTotal.find(text)?.groupValues?.getOrNull(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()
            ?.takeIf { it > 0.0 }

    fun loadsNetCandidates(text: String): List<Double> {
        val section = loadsSection(text)
        val found = ArrayList<Double>()
        fun add(value: Double?) {
            if (value == null || value <= MIN_TOTAL) return
            if (found.any { near(it, value) }) return
            found += value
        }
        val orders = labeledAmounts(section, """Order\s*Totals?""")
        if (orders.size >= 2) add(roundCents(orders.sum()))
        add(firstPlainTotal(section))
        val methodNets = labeledAmounts(section, """\d{1,3}\s*%""")
        if (methodNets.size >= 2) add(roundCents(methodNets.sum()))
        return found
    }

    fun closingLoadsNet(
        takeHome: Double?,
        candidates: List<Double>,
        deductions: Double?,
    ): Double? {
        if (takeHome != null && deductions != null) {
            candidates.firstOrNull { near(it - deductions, takeHome) }?.let { return it }
        }
        return candidates.firstOrNull()
    }

    fun deductions(text: String): Double? {
        val labeled = totalDeductionsLabel.find(text)?.groupValues?.getOrNull(1)
            ?.let(::parseMoney)
            ?.takeIf { it > 0.0 }
        if (labeled != null) return labeled
        val block = deductionBlock(text) ?: return null
        val items = deductionItemSum(block)
        val total = plainTotals(block).lastOrNull()
        return when {
            items != null && total != null && near(items, total) -> total
            total != null -> total
            else -> items
        }
    }

    /** Sum of gross amounts on trip lines, when every trip line has one. */
    fun grossFromLines(text: String): Double? {
        val lines = tripLine.findAll(text).map { it.value.trim() }.toList()
        if (lines.size < 2) return null
        val grosses = lines.map(::grossOnTripLine)
        if (grosses.any { it == null }) return null
        return roundCents(grosses.sumOf { it ?: 0.0 })
    }

    fun confidence(
        takeHome: Double,
        loadsNets: List<Double>,
        deductions: Double?,
        gross: Double?,
        lineGross: Double?,
    ): String {
        val totalsDisagree = deductions != null &&
            loadsNets.isNotEmpty() &&
            loadsNets.none { near(it - deductions, takeHome) }
        val grossDisagrees = gross != null && lineGross != null && !near(gross, lineGross)
        return if (totalsDisagree || grossDisagrees) "low" else "high"
    }

    private fun isPersonLine(line: String): Boolean =
        personLine.matches(line) && !notAPerson.containsMatchIn(line)

    private fun loadsSection(text: String): String {
        val cut = deductionsHeader.find(text)?.range?.first ?: return text
        return text.substring(0, cut)
    }

    private fun deductionBlock(text: String): String? {
        val start = deductionsHeader.find(text)?.range?.first ?: return null
        val rest = text.substring(start)
        val end = summaryStart.find(rest)?.range?.first ?: rest.length
        return rest.substring(0, end)
    }

    private fun deductionItemSum(block: String): Double? {
        val amounts = block.lines().map { it.trim() }.filter { line ->
            line.isNotEmpty() &&
                !line.equals("Deductions", ignoreCase = true) &&
                !line.startsWith("Description", ignoreCase = true) &&
                !line.startsWith("Total", ignoreCase = true)
        }.mapNotNull { line -> moneys(line).lastOrNull() }
        if (amounts.size < 2) return null
        return roundCents(amounts.sum())
    }

    private fun firstPlainTotal(section: String): Double? = plainTotals(section).firstOrNull()

    private fun plainTotals(section: String): List<Double> =
        plainTotal.findAll(section).mapNotNull { match ->
            if (skipTotal.containsMatchIn(match.value)) return@mapNotNull null
            parseMoney(match.groupValues.getOrNull(1)).takeIf { it > MIN_TOTAL }
        }.toList()

    private fun grossOnTripLine(line: String): Double? {
        val amounts = moneys(line)
        return when {
            amounts.size >= 2 -> amounts.first()
            amounts.size == 1 && percentOnLine.containsMatchIn(line) -> null
            amounts.size == 1 -> amounts.first()
            else -> null
        }
    }

    private fun labeledAmounts(text: String, label: String): List<Double> {
        val rx = Regex(
            """$label[^\d${'$'}]{0,40}[${'$'}]?\s*$moneyNumber""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        return rx.findAll(text).mapNotNull { match ->
            parseMoney(match.groupValues.getOrNull(1)).takeIf { it > 0.0 }
        }.toList()
    }

    private fun moneys(line: String): List<Double> =
        moneyToken.findAll(line).mapNotNull { match ->
            parseMoney(match.groupValues.getOrNull(1)).takeIf { it > 0.0 }
        }.toList()

    private fun parseMoney(raw: String?): Double {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return 0.0
        return ParseUtils.parseMoney(trimmed)
    }

    private fun roundCents(value: Double): Double = (value * 100.0).roundToInt() / 100.0

    private fun near(a: Double, b: Double): Boolean = abs(a - b) < 0.02

    private const val MIN_TOTAL = 50.0
    private const val HEADER_NAME_WINDOW = 5
}
