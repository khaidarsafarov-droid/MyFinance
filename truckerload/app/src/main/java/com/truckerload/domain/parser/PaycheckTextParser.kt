package com.truckerload.domain.parser

import com.truckerload.domain.model.PaycheckParseResult
import kotlin.math.roundToInt

object PaycheckTextParser {

    /**
     * Dollars after a label: `$4,040.30`, `4 040.30`, OCR `4040 30`, `Grand Tota1`.
     */
    private val moneyNumber =
        """([\d]{1,3}(?:,\d{3})+(?:\.\d{2}|\s+\d{2})?|""" +
            """[\d]{1,3}(?:\s\d{3})+(?:\.\d{2}|\s+\d{2})|""" +
            """[\d]+\.\d{2}|""" +
            """[\d]{3,6}\s+\d{2})"""
    private val moneyCapture = """[${'$'}S]?\s*$moneyNumber"""
    private val netPayLabel = """(?:Net\s*Pay|Зарплата)"""
    private val statementTakeHomeLabel =
        """(?:G[ra]{0,2}and\s*Tota[l1I]|Settlement\s*(?:Total|Amount)|""" +
            """Take[\s-]*Home|Check\s*(?:Amount|Total)|Net\s*Settlement)"""
    private val simpleTakeHomeLabel =
        """(?:$statementTakeHomeLabel|Driver\s*Pay|Net\s*(?:Check|Earnings)|""" +
            """Amount\s*(?:to\s*)?Driver|Total\s*(?:Due\s*)?Driver)"""
    private val statementMarker = Regex(
        """Driver\s*Sett+e?ment|Settlement\s*Summary|Settlement\s*Date|Payee\s*ID|""" +
            """Total\s*Deductions|Owner\s*Operator\s*Sett+e?ment|""" +
            """Weekly\s*Sett+e?ment|Pay\s*Statement|Driver\s*Statement|Sett+e?ment""",
        RegexOption.IGNORE_CASE,
    )
    private val grossPattern = Regex(
        """Gross\s*Pay(?:\s*Total)?[^\d$]{0,40}$moneyCapture""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val driverPattern = Regex("""Driver\s*[:]\s*([A-Za-z .'-]+)""", RegexOption.IGNORE_CASE)
    private val weekStartPattern = Regex(
        """(?:Week\s*Start|Settlement\s*Date|Cutoff\s*Date)\s*[:\s]*([^\n]+)""",
        RegexOption.IGNORE_CASE,
    )
    private val weekEndPattern = Regex("""Week\s*End\s*[:\s]*([^\n]+)""", RegexOption.IGNORE_CASE)
    private val fileWeekRange = Regex(
        """(\d{1,2}[./]\d{1,2})[-–](\d{1,2}[./]\d{1,2})""",
    )
    private val methodNet = Regex(
        """\d{1,3}\s*%[^\d$]{0,12}$moneyCapture""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val totalLine = Regex(
        """(?m)^[^\n]{0,48}\bTotal\b[^\n]{0,48}$moneyCapture""",
        setOf(RegexOption.IGNORE_CASE),
    )

    fun looksLikePaycheck(text: String, fileName: String? = null): Boolean {
        val hay = haystack(text, fileName)
        return amountAfter(hay, simpleTakeHomeLabel) != null ||
            amountAfter(hay, netPayLabel) != null ||
            (isDriverStatement(hay) && computedTakeHome(hay) != null)
    }

    fun parse(text: String, fileName: String? = null): PaycheckParseResult? {
        val hay = haystack(text, fileName)
        val statement = isDriverStatement(hay)
        val takeHome = amountAfter(
            hay,
            if (statement) statementTakeHomeLabel else simpleTakeHomeLabel,
        )
        val netPays = amountsAfter(hay, netPayLabel)
        val computed = if (statement) computedTakeHome(hay) else null
        val netAmount = when {
            statement && takeHome != null -> takeHome
            !statement && netPays.isNotEmpty() -> netPays.first()
            takeHome != null -> takeHome
            statement -> computed ?: return null
            else -> return null
        }
        if (netAmount <= 0) return null

        val grossAmount = labeledMoney(hay, grossPattern)
        val weekStart = weekStartPattern.find(hay)?.groupValues?.get(1)
            ?.let { ParseUtils.normalizeDate(it) }
            ?.takeIf { it.isNotBlank() }
            ?: weekFromFileName(fileName)?.first
        val weekEnd = weekEndPattern.find(hay)?.groupValues?.get(1)
            ?.let { ParseUtils.normalizeDate(it) }
            ?.takeIf { it.isNotBlank() }
            ?: weekFromFileName(fileName)?.second
        val driverName = driverPattern.find(hay)?.groupValues?.get(1)?.trim()

        return PaycheckParseResult(
            driverName = driverName,
            weekStartDate = weekStart,
            weekEndDate = weekEnd,
            grossAmount = grossAmount,
            netAmount = netAmount,
            confidence = "high",
        )
    }

    internal fun isDriverStatement(text: String): Boolean {
        if (statementMarker.containsMatchIn(text)) return true
        return amountsAfter(text, netPayLabel).size >= 2
    }

    private fun haystack(text: String, fileName: String?): String =
        listOfNotNull(fileName?.replace('-', ' '), text).joinToString("\n")

    private fun computedTakeHome(hay: String): Double? {
        val deductions = amountAfter(hay, """Total\s*Deductions""") ?: return null
        val loadsNet = statementLoadsNet(hay) ?: lineNetSum(hay) ?: return null
        val net = ((loadsNet - deductions) * 100.0).roundToInt() / 100.0
        return net.takeIf { it > 0 }
    }

    private fun statementLoadsNet(hay: String): Double? {
        val amounts = totalLine.findAll(hay).mapNotNull { match ->
            val line = match.value
            if (DEDUCTION_OR_GROSS.containsMatchIn(line)) return@mapNotNull null
            parsePayMoney(match.groupValues.getOrNull(1)).takeIf { it > 50 }
        }.toList()
        return amounts.firstOrNull()
    }

    private fun lineNetSum(hay: String): Double? {
        val nets = methodNet.findAll(hay).mapNotNull { match ->
            parsePayMoney(match.groupValues.getOrNull(1)).takeIf { it > 0 }
        }.toList()
        if (nets.size < 2) return null
        return (nets.sum() * 100.0).roundToInt() / 100.0
    }

    private fun amountAfter(text: String, label: String): Double? =
        amountsAfter(text, label).firstOrNull()

    private fun amountsAfter(text: String, label: String): List<Double> {
        val rx = Regex(
            """$label[^\d$]{0,80}$moneyCapture""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        return rx.findAll(text).mapNotNull { match ->
            parsePayMoney(match.groupValues.getOrNull(1)).takeIf { it > 0 }
        }.toList()
    }

    private fun labeledMoney(text: String, pattern: Regex): Double? =
        pattern.find(text)?.groupValues?.getOrNull(1)
            ?.let { parsePayMoney(it) }
            ?.takeIf { it > 0 }

    /**
     * `4040 30` / `4 040 30` → 4040.30. Otherwise [ParseUtils.parseMoney]
     * (which already strips spaces around a real decimal).
     */
    private fun parsePayMoney(raw: String?): Double {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return 0.0
        val parts = trimmed.split(Regex("""\s+"""))
        val cents = parts.last()
        if (parts.size >= 2 &&
            cents.length == 2 &&
            cents.all { it.isDigit() } &&
            !trimmed.contains('.')
        ) {
            val dollars = parts.dropLast(1).joinToString("").replace(",", "").toDoubleOrNull()
            if (dollars != null) return dollars + cents.toInt() / 100.0
        }
        return ParseUtils.parseMoney(trimmed)
    }

    private fun weekFromFileName(fileName: String?): Pair<String, String>? {
        val match = fileWeekRange.find(fileName.orEmpty()) ?: return null
        val start = ParseUtils.normalizeDate(match.groupValues[1].replace('.', '/'))
        val end = ParseUtils.normalizeDate(match.groupValues[2].replace('.', '/'))
        if (start.isBlank() || end.isBlank()) return null
        return start to end
    }

    private val DEDUCTION_OR_GROSS = Regex(
        """deduct|gross|miles|load""",
        RegexOption.IGNORE_CASE,
    )
}
