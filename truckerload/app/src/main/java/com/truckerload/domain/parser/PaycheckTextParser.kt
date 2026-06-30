package com.truckerload.domain.parser

import com.truckerload.domain.model.PaycheckParseResult

object PaycheckTextParser {

    private val netPatterns = listOf(
        Regex("""(?:Grand\s*Total|Зарплата|Net\s*Pay)\s*[:\s]*\$?\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE),
        Regex("""(?:Settlement\s*Total|Driver\s*Pay)\s*[:\s]*\$?\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
    )
    private val grossPattern = Regex("""Gross\s*Pay(?:\s*Total)?\s*[:\s]*\$?\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
    private val driverPattern = Regex("""Driver\s*[:\s]+([A-Za-z .'-]+)""", RegexOption.IGNORE_CASE)
    private val weekStartPattern = Regex("""(?:Week\s*Start|Settlement\s*Date|Cutoff\s*Date)\s*[:\s]*([^\n]+)""", RegexOption.IGNORE_CASE)
    private val weekEndPattern = Regex("""Week\s*End\s*[:\s]*([^\n]+)""", RegexOption.IGNORE_CASE)

    fun looksLikePaycheck(text: String): Boolean {
        return netPatterns.any { it.containsMatchIn(text) }
    }

    fun parse(text: String): PaycheckParseResult? {
        if (!looksLikePaycheck(text)) return null
        val netRaw = ParseUtils.firstMatch(text, netPatterns) ?: return null
        val netAmount = ParseUtils.parseMoney(netRaw)
        if (netAmount <= 0) return null

        val grossAmount = grossPattern.find(text)?.groupValues?.get(1)?.let { ParseUtils.parseMoney(it) }
        val weekStart = weekStartPattern.find(text)?.groupValues?.get(1)?.let { ParseUtils.normalizeDate(it) }
        val weekEnd = weekEndPattern.find(text)?.groupValues?.get(1)?.let { ParseUtils.normalizeDate(it) }
        val driverName = driverPattern.find(text)?.groupValues?.get(1)?.trim()

        return PaycheckParseResult(
            driverName = driverName,
            weekStartDate = weekStart,
            weekEndDate = weekEnd,
            grossAmount = grossAmount,
            netAmount = netAmount,
            confidence = if (netAmount > 0) "high" else "low"
        )
    }
}
