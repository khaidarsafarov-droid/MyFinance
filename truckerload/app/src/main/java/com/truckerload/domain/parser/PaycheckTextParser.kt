package com.truckerload.domain.parser

import com.truckerload.domain.model.PaycheckParseResult

object PaycheckTextParser {

    private val moneyCapture = """\$?\s*([\d]{1,3}(?:,\d{3})+(?:\.\d{2})?|[\d]+\.\d{2})"""
    private val netPayLabel = """(?:Net\s*Pay|Зарплата)"""
    private val grandLabel = """G[ra]{0,2}and\s*Tota[l1I]"""
    private val settlementTotalLabel = """(?:Settlement\s*Total|Driver\s*Pay)"""
    private val statementMarker = Regex(
        """Driver\s*Settlement|Settlement\s*Summary|Settlement\s*Date|Payee\s*ID|""" +
            """Total\s*Deductions|Sett+e?ment""",
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

    fun looksLikePaycheck(text: String, fileName: String? = null): Boolean {
        val hay = haystack(text, fileName)
        return amountAfter(hay, netPayLabel) != null ||
            amountAfter(hay, grandLabel) != null ||
            amountAfter(hay, settlementTotalLabel) != null
    }

    fun parse(text: String, fileName: String? = null): PaycheckParseResult? {
        val hay = haystack(text, fileName)
        val grand = amountAfter(hay, grandLabel)
        val netPays = amountsAfter(hay, netPayLabel)
        val settlementTotal = amountAfter(hay, settlementTotalLabel)
        val statement = isDriverStatement(hay)
        val netAmount = when {
            statement && grand != null -> grand
            !statement && netPays.isNotEmpty() -> netPays.first()
            grand != null -> grand
            settlementTotal != null -> settlementTotal
            netPays.isNotEmpty() -> netPays.first()
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

    private fun amountAfter(text: String, label: String): Double? =
        amountsAfter(text, label).firstOrNull()

    private fun amountsAfter(text: String, label: String): List<Double> {
        val rx = Regex(
            """$label[^\d$]{0,80}$moneyCapture""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        return rx.findAll(text).mapNotNull { match ->
            ParseUtils.parseMoney(match.groupValues.getOrNull(1)).takeIf { it > 0 }
        }.toList()
    }

    private fun labeledMoney(text: String, pattern: Regex): Double? =
        pattern.find(text)?.groupValues?.getOrNull(1)
            ?.let { ParseUtils.parseMoney(it) }
            ?.takeIf { it > 0 }

    private fun weekFromFileName(fileName: String?): Pair<String, String>? {
        val match = fileWeekRange.find(fileName.orEmpty()) ?: return null
        val start = ParseUtils.normalizeDate(match.groupValues[1].replace('.', '/'))
        val end = ParseUtils.normalizeDate(match.groupValues[2].replace('.', '/'))
        if (start.isBlank() || end.isBlank()) return null
        return start to end
    }
}
