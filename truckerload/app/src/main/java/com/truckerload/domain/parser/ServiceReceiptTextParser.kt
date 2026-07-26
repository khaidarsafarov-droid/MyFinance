package com.truckerload.domain.parser

/**
 * Extracts total amount (and optional date / description hints) from service invoice OCR text.
 */
object ServiceReceiptTextParser {

    private val totalPatterns = listOf(
        Regex("""(?:Grand\s*Total|Amount\s*Due|Balance\s*Due|Total\s*Due|Итого|Всего|Сумма|Total)\s*[:\s]*\$?\s*([\d\s,]+\.?\d*)""", RegexOption.IGNORE_CASE),
        Regex("""\$\s*([\d,]+\.\d{2})\b"""),
    )

    private val datePatterns = listOf(
        Regex("""(?:Date|Дата|Invoice\s*Date)\s*[:\s]*([^\n]+)""", RegexOption.IGNORE_CASE),
        Regex("""\b(\d{1,2}[./-]\d{1,2}[./-]\d{2,4})\b"""),
        Regex("""\b(\d{4}-\d{2}-\d{2})\b"""),
    )

    data class ParseResult(
        val amount: Double?,
        val date: String?,
        val descriptionHint: String?,
    )

    fun parse(text: String): ParseResult {
        if (text.isBlank()) return ParseResult(null, null, null)
        val amount = totalPatterns
            .asSequence()
            .mapNotNull { pattern ->
                pattern.findAll(text)
                    .mapNotNull { match -> ParseUtils.parseMoney(match.groupValues[1]).takeIf { it > 0 } }
                    .maxOrNull()
            }
            .filterNotNull()
            .maxOrNull()

        val date = datePatterns
            .asSequence()
            .mapNotNull { it.find(text)?.groupValues?.get(1)?.let { raw -> ParseUtils.normalizeDate(raw) } }
            .firstOrNull { it.isNotBlank() }

        val descriptionHint = text.lineSequence()
            .map { it.trim() }
            .firstOrNull { line ->
                line.length in 3..80 &&
                    !line.contains('$') &&
                    !Regex("""^\d""").containsMatchIn(line) &&
                    !Regex("""total|итого|date|дата|invoice|phone|tel""", RegexOption.IGNORE_CASE)
                        .containsMatchIn(line)
            }

        return ParseResult(amount = amount, date = date, descriptionHint = descriptionHint)
    }
}
