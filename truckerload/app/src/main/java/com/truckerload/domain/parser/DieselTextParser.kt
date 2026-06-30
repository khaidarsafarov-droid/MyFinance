package com.truckerload.domain.parser

import com.truckerload.domain.model.DieselParseResult

object DieselTextParser {

    private val totalPatterns = listOf(
        Regex("""(?:Total\s*Amount|Amount\s*Due|Итого|Total)\s*[:\s]*\$?\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE),
        Regex("""(?:Fuel\s*Total|Diesel\s*Total)\s*[:\s]*\$?\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
    )
    private val gallonsPattern = Regex("""([\d.]+)\s*(?:gal|gallons|гл)\b""", RegexOption.IGNORE_CASE)
    private val ppgPattern = Regex("""(?:Price|PPG|@\s*)\$?\s*([\d.]+)\s*(?:/|\s*per\s*)?\s*gal""", RegexOption.IGNORE_CASE)
    private val datePattern = Regex("""(?:Date|Дата)\s*[:\s]*([^\n]+)""", RegexOption.IGNORE_CASE)
    private val locationPattern = Regex("""(?:Location|Store|Station|АЗС)\s*[:\s]*([^\n]+)""", RegexOption.IGNORE_CASE)
    private val vendorPattern = Regex("""(?:Merchant|Vendor)\s*[:\s]*([^\n]+)""", RegexOption.IGNORE_CASE)

    fun looksLikeDiesel(text: String): Boolean {
        val hasFuelKeyword = Regex("""diesel|fuel|gallons?|gal\b|топлив|дизел""", RegexOption.IGNORE_CASE).containsMatchIn(text)
        val hasAmount = totalPatterns.any { it.containsMatchIn(text) }
        return hasFuelKeyword && hasAmount
    }

    fun parse(text: String): DieselParseResult? {
        if (!looksLikeDiesel(text)) return null
        val totalRaw = ParseUtils.firstMatch(text, totalPatterns) ?: return null
        val totalAmount = ParseUtils.parseMoney(totalRaw)
        if (totalAmount <= 0) return null

        val gallons = gallonsPattern.find(text)?.groupValues?.get(1)?.toDoubleOrNull()
        val ppg = ppgPattern.find(text)?.groupValues?.get(1)?.toDoubleOrNull()
        val date = datePattern.find(text)?.groupValues?.get(1)?.let { ParseUtils.normalizeDate(it) }
        val location = locationPattern.find(text)?.groupValues?.get(1)?.trim()
        val vendor = vendorPattern.find(text)?.groupValues?.get(1)?.trim()

        return DieselParseResult(
            date = date,
            totalAmount = totalAmount,
            gallons = gallons,
            pricePerGallon = ppg,
            location = location,
            vendor = vendor,
            confidence = "high"
        )
    }
}
