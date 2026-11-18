package com.truckerload.domain.parser

/**
 * The one parser for amounts, miles and gallons typed by the driver, pasted, or read by OCR.
 *
 * Accepts US `2,500.50`, EU `2.500,50`, decimal comma `25,5`, plain `2500`, an optional
 * `$` / `USD` and spaces. Separator rules:
 * - both `,` and `.` present: the last one is the decimal mark, the other groups thousands;
 * - repeated `,` or `.`: thousands, unless the last group has 1–2 digits (`1,234,56` = 1234.56);
 * - a single `,`: thousands when exactly three digits follow (`2,500` = 2500), else decimal;
 * - a single `.`: decimal.
 */
object DecimalInput {

    fun parse(raw: String?): Double? = canonical(raw)?.toDoubleOrNull()

    /**
     * Same cleaning as [parse], kept as text so money can be read in cents
     * without a [Double] round trip.
     */
    fun canonical(raw: String?): String? {
        if (raw == null) return null
        val compact = raw.trim()
            .replace("USD", "", ignoreCase = true)
            .filterNot { it == '$' || it.isWhitespace() || it == '\u00A0' }
        if (compact.isEmpty()) return null
        val negative = compact.startsWith('-')
        val body = compact.removePrefix("-")
        if (body.isEmpty() || body.any { !it.isDigit() && it != ',' && it != '.' }) return null
        val normalized = normalizeSeparators(body) ?: return null
        if (normalized.toDoubleOrNull() == null) return null
        return if (negative) "-$normalized" else normalized
    }

    private fun normalizeSeparators(body: String): String? {
        val lastComma = body.lastIndexOf(',')
        val lastDot = body.lastIndexOf('.')
        return when {
            lastComma >= 0 && lastDot >= 0 -> {
                val decimal = if (lastComma > lastDot) ',' else '.'
                val grouping = if (decimal == ',') '.' else ','
                if (body.indexOf(decimal) != body.lastIndexOf(decimal)) return null
                body.replace(grouping.toString(), "").replace(decimal, '.')
            }
            lastComma >= 0 -> singleSeparator(body, ',', singleIsDecimal = digitsAfter(body, lastComma) != 3)
            lastDot >= 0 -> singleSeparator(body, '.', singleIsDecimal = true)
            else -> body
        }
    }

    private fun singleSeparator(body: String, separator: Char, singleIsDecimal: Boolean): String {
        val count = body.count { it == separator }
        val last = body.lastIndexOf(separator)
        val lastIsDecimal = if (count == 1) singleIsDecimal else digitsAfter(body, last) in 1..2
        if (!lastIsDecimal) return body.replace(separator.toString(), "")
        val integerPart = body.substring(0, last).replace(separator.toString(), "")
        return integerPart + "." + body.substring(last + 1)
    }

    private fun digitsAfter(body: String, index: Int): Int = body.length - index - 1
}
