package com.truckerload.domain.parser

/**
 * Splits OCR receipt text into service rows (name + amount).
 * Invoice tables print Unit Price / Qty / Amount as trailing numbers;
 * the last number is the line total. Summary rows (tax, total, paid) are skipped.
 */
object ServiceReceiptLineItems {

    data class LineItem(
        val description: String,
        val amount: Double,
    )

    private val skipLinePattern = Regex(
        """\b(?:grand\s*total|\btotals?\b|amount\s*(?:due|paid)|balance\s*due|total\s*(?:due|amount)|""" +
            """sub\s*-?\s*total|subtotal|sales\s*tax|\btax\b|tip|gratuity|discount|""" +
            """change\s*due|итого|всего|сумма|qty|quantity|unit\s*price|\bitem\b|""" +
            """description|amount|date|дата|invoice|due\s*date|notes?|thank|visa|""" +
            """mastercard|cash|phone|tel|fax|www\.|http)\b""",
        RegexOption.IGNORE_CASE,
    )

    private val onlyDatePattern = Regex(
        """^(?:\d{1,2}[./-]\d{1,2}[./-]\d{2,4}|\d{4}-\d{2}-\d{2})$""",
    )

    private val numericTokenPattern = Regex(
        """^\$?\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?$|^\$?\d+\.\d{2}$|^\$?\d+$""",
    )

    fun extract(lines: List<String>): List<LineItem> {
        val items = mutableListOf<LineItem>()
        var pendingDescription: String? = null
        for (raw in lines) {
            val line = raw.trim()
            if (line.isBlank() || isSkip(line)) {
                pendingDescription = null
                continue
            }
            val parsed = splitDescriptionAndAmount(line)
            when {
                parsed != null -> {
                    items += parsed
                    pendingDescription = null
                }
                looksLikeDescription(line) -> pendingDescription = line
                pendingDescription != null -> {
                    val amount = trailingAmount(line)
                    if (amount != null && amount > 0) {
                        items += LineItem(pendingDescription.trim(), amount)
                    }
                    pendingDescription = null
                }
            }
        }
        return items
    }

    private fun isSkip(line: String): Boolean =
        skipLinePattern.containsMatchIn(line) || onlyDatePattern.matches(line)

    private fun splitDescriptionAndAmount(line: String): LineItem? {
        val tokens = line.split(Regex("""\s+""")).filter { it.isNotBlank() }
        if (tokens.size < 2) return null
        var trailing = 0
        for (i in tokens.indices.reversed()) {
            if (!isNumericToken(tokens[i])) break
            trailing++
        }
        if (trailing == 0) return null
        val strip = if (trailing >= 3) 3 else trailing
        val description = tokens.dropLast(strip).joinToString(" ").trim()
        val amount = ParseUtils.parseMoney(tokens.last()).takeIf { it > 0 } ?: return null
        if (!looksLikeDescription(description)) return null
        return LineItem(description, amount)
    }

    private fun trailingAmount(line: String): Double? {
        val tokens = line.split(Regex("""\s+""")).filter { it.isNotBlank() }
        if (tokens.isEmpty() || tokens.any { !isNumericToken(it) }) return null
        return ParseUtils.parseMoney(tokens.last()).takeIf { it > 0 }
    }

    private fun isNumericToken(token: String): Boolean =
        numericTokenPattern.matches(token.trim())

    private fun looksLikeDescription(text: String): Boolean {
        if (text.length !in 2..80) return false
        if (isSkip(text)) return false
        return text.count { it.isLetter() } >= 2
    }
}
