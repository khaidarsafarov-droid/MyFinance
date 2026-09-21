package com.truckerload.domain.parser

import com.truckerload.domain.model.DieselParseResult

object DieselTextParser {

    private val totalPatterns = listOf(
        // Avoid bare "Total" — it matched Relay "Total Loaded Miles" as a diesel amount.
        Regex("""(?:Total\s*Amount|Amount\s*Due|Итого)\s*[:\s]*\$?\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE),
        Regex(
            """(?:Fuel\s*Total|Diesel\s*Total|Pump\s*Total|Fuel\s*(?:Sale|Amount)|Sale\s*(?:Amount|Total))\s*[:\s]*\$?\s*([\d,]+\.?\d*)""",
            RegexOption.IGNORE_CASE,
        ),
        Regex(
            """^\s*(?:SALE|AMT|AMOUNT)\s*[:\s]*\$?\s*([\d,]+\.?\d*)\s*$""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
        ),
    )
    private val gallonsPattern = Regex("""([\d.]+)[ \t]*(?:gal|gallons|gals|гл)\b""", RegexOption.IGNORE_CASE)
    private val ppgPattern = Regex(
        """(?:Price|PPG|PPU|@\s*)\$?\s*([\d.]+)\s*(?:/|\s*per\s*)?\s*g(?:al)?""",
        RegexOption.IGNORE_CASE,
    )
    private val datePattern = Regex("""(?:Date|Дата|Trans(?:action)?\s*Date)\s*[:\s]*([^\n]+)""", RegexOption.IGNORE_CASE)
    private val unlabeledDate = Regex("""\b(\d{1,2}[./-]\d{1,2}[./-]\d{2,4})\b""")
    private val locationPattern = Regex("""(?:Location|Store|Station|City|АЗС)\s*[:\s]*([^\n]+)""", RegexOption.IGNORE_CASE)
    private val vendorPattern = Regex("""(?:Merchant|Vendor)\s*[:\s]*([^\n]+)""", RegexOption.IGNORE_CASE)
    private val fuelCue = Regex(
        """diesel|fuel|gallons?|gal\b|топлив|дизел|\bdef\b|ad[\s\-]?blue|ulsd|#2\s*diesel|pump\s*total""",
        RegexOption.IGNORE_CASE,
    )

    fun looksLikeDiesel(text: String): Boolean = resolve(text) != null

    fun parse(text: String): DieselParseResult? = resolve(text)

    private fun resolve(text: String): DieselParseResult? {
        labeled(text)?.let { return it }
        return fromReceiptFields(text)
    }

    private fun labeled(text: String): DieselParseResult? {
        if (!fuelCue.containsMatchIn(text)) return null
        if (totalPatterns.none { it.containsMatchIn(text) }) return null
        val totalRaw = ParseUtils.firstMatch(text, totalPatterns) ?: return null
        val totalAmount = ParseUtils.parseMoney(totalRaw)
        if (totalAmount <= 0) return null

        val extra = DieselReceiptExtractor.extract(text)
        val gallons = gallonsPattern.find(text)?.groupValues?.get(1)?.toDoubleOrNull()
            ?: extra.gallons
        val ppg = ppgPattern.find(text)?.groupValues?.get(1)?.toDoubleOrNull()
            ?: extra.pricePerGallon
        return DieselParseResult(
            date = findDate(text),
            totalAmount = totalAmount,
            gallons = gallons,
            pricePerGallon = ppg,
            location = locationPattern.find(text)?.groupValues?.get(1)?.trim() ?: extra.location,
            vendor = vendorPattern.find(text)?.groupValues?.get(1)?.trim() ?: extra.vendor,
            confidence = "high",
        )
    }

    private fun fromReceiptFields(text: String): DieselParseResult? {
        val fields = DieselReceiptExtractor.extract(text)
        val total = fields.totalAmount
            ?: derivedTotal(fields.gallons, fields.pricePerGallon ?: fields.discountPricePerGallon)
            ?: return null
        if (total <= 0) return null
        val hasCue = fuelCue.containsMatchIn(text) ||
            fields.vendor != null ||
            fields.gallons != null
        if (!hasCue) return null
        return DieselParseResult(
            date = findDate(text),
            totalAmount = total,
            gallons = fields.gallons,
            pricePerGallon = fields.pricePerGallon ?: fields.discountPricePerGallon,
            location = fields.location,
            vendor = fields.vendor,
            confidence = if (fields.gallons != null && fields.pricePerGallon != null) "high" else "medium",
        )
    }

    private fun derivedTotal(gallons: Double?, ppg: Double?): Double? {
        if (gallons == null || ppg == null || gallons <= 0 || ppg <= 0) return null
        return gallons * ppg
    }

    private fun findDate(text: String): String? {
        datePattern.find(text)?.groupValues?.get(1)?.let { raw ->
            ParseUtils.normalizeDate(raw).takeIf { it.length >= 10 }?.let { return it }
        }
        unlabeledDate.find(text)?.value?.let { raw ->
            ParseUtils.normalizeDate(raw).takeIf { it.length >= 10 }?.let { return it }
        }
        return null
    }
}
