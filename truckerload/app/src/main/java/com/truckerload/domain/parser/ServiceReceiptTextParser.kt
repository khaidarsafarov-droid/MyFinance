package com.truckerload.domain.parser

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Smart heuristics for service / truck-stop receipt OCR text.
 * Prefers labeled totals over line-item prices; vendor from the header lines.
 */
object ServiceReceiptTextParser {

    private val knownServices = listOf(
        "Love's",
        "Loves",
        "TA Truck Service",
        "TA Petro",
        "TravelCenters of America",
        "TravelCenters",
        "Travel Centers",
        "Petro Stopping Centers",
        "Petro Stopping",
        "Pilot Flying J",
        "Flying J",
        "Pilot",
        "Speedco",
        "Jiffy Lube",
        "Firestone",
        "Goodyear",
        "Pep Boys",
        "NAPA",
        "FleetPride",
        "Fleet Pride",
        "TruckPro",
        "Kenworth",
        "Peterbilt",
        "Freightliner",
        "Volvo Trucks",
        "Interstate Batteries",
        "Truck Service",
    )

    /** Higher score = more likely to be the final amount due. */
    private val labeledTotalPatterns = listOf(
        100 to Regex(
            """(?:Grand\s*Total|Amount\s*Due|Balance\s*Due|Total\s*Due|Total\s*Amount|Amount\s*Paid|Итого\s*к\s*оплате|Всего\s*к\s*оплате)\s*[:\s]*\$?\s*([\d\s,]+\.?\d*)""",
            RegexOption.IGNORE_CASE,
        ),
        80 to Regex(
            """(?:^|\n)\s*(?:TOTAL|ИТОГО|ВСЕГО|СУММА)\s*[:\s]*\$?\s*([\d\s,]+\.?\d*)""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
        ),
        60 to Regex(
            """(?:Total|Итого|Всего|Сумма)\s*[:\s]*\$?\s*([\d\s,]+\.?\d*)""",
            RegexOption.IGNORE_CASE,
        ),
    )

    private val bareDollarPattern = Regex("""\$\s*([\d,]+\.\d{2})\b""")

    private val intermediateLinePattern = Regex(
        """\b(?:sub\s*-?\s*total|subtotal|tax|sales\s*tax|tip|gratuity|discount|deposit|change(?!\s+oil)|cash\s*tendered|qty|quantity|unit\s*price|each)\b""",
        RegexOption.IGNORE_CASE,
    )

    private val dateLabeledPattern = Regex(
        """(?:Date|Дата|Invoice\s*Date|Service\s*Date|Trans(?:action)?\s*Date)\s*[:\s]*([^\n]+)""",
        RegexOption.IGNORE_CASE,
    )

    private val dateIsoPattern = Regex("""\b(\d{4}-\d{2}-\d{2})\b""")
    private val dateNumericPattern = Regex("""\b(\d{1,2}[./-]\d{1,2}[./-]\d{2,4})\b""")
    private val dateTextMonthPattern = Regex(
        """\b((?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:t(?:ember)?)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\.?\s+\d{1,2}(?:,)?\s+\d{2,4})\b""",
        RegexOption.IGNORE_CASE,
    )

    private val skipVendorLinePattern = Regex(
        """total|итого|amount|date|дата|invoice|phone|tel|fax|www\.|http|receipt|thank|subtotal|tax|qty|item|visa|mastercard|cash|balance|miles?|mi\b|gal|gallon|street|road|ave|blvd|suite|ste\b""",
        RegexOption.IGNORE_CASE,
    )

    private val skipDescriptionPattern = Regex(
        """total|итого|amount|date|дата|invoice|phone|tel|fax|www\.|http|receipt|thank|subtotal|tax|qty|item|visa|mastercard|cash|change\s+due|balance\s+due|miles?|mi\b|gal|gallon""",
        RegexOption.IGNORE_CASE,
    )

    data class ParseResult(
        val amount: Double?,
        val date: String?,
        val serviceName: String?,
        val descriptionHint: String?,
    )

    /**
     * @param defaultDate ISO `yyyy-MM-dd` used when OCR has no date (defaults to today).
     */
    fun parse(
        text: String,
        defaultDate: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    ): ParseResult {
        if (text.isBlank()) {
            return ParseResult(null, defaultDate, null, null)
        }

        val lines = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()

        val amount = extractTotal(text, lines)
        val date = extractDate(text) ?: defaultDate
        val serviceName = extractServiceName(text, lines)
        val descriptionHint = extractDescription(lines, serviceName)

        return ParseResult(
            amount = amount,
            date = date,
            serviceName = serviceName,
            descriptionHint = descriptionHint,
        )
    }

    private fun extractTotal(text: String, lines: List<String>): Double? {
        var bestScore = -1
        var bestAmount: Double? = null

        for ((score, pattern) in labeledTotalPatterns) {
            for (match in pattern.findAll(text)) {
                val rawLine = lineContaining(text, match.range.first)
                if (rawLine != null && intermediateLinePattern.containsMatchIn(rawLine) &&
                    !Regex("""grand\s*total|amount\s*due|balance\s*due""", RegexOption.IGNORE_CASE)
                        .containsMatchIn(rawLine)
                ) {
                    continue
                }
                val value = ParseUtils.parseMoney(match.groupValues[1]).takeIf { it > 0 } ?: continue
                if (score > bestScore || (score == bestScore && value > (bestAmount ?: 0.0))) {
                    bestScore = score
                    bestAmount = value
                }
            }
        }
        if (bestAmount != null) return bestAmount

        // Fallback: last bare $x.xx that is not on an intermediate (tax/subtotal) line.
        val candidates = bareDollarPattern.findAll(text).mapNotNull { match ->
            val rawLine = lineContaining(text, match.range.first) ?: return@mapNotNull null
            if (intermediateLinePattern.containsMatchIn(rawLine)) return@mapNotNull null
            ParseUtils.parseMoney(match.groupValues[1]).takeIf { it > 0 }
        }.toList()

        if (candidates.isEmpty()) {
            // Last resort: largest money-like token on a non-intermediate line near the bottom.
            return lines.asReversed()
                .asSequence()
                .filterNot { intermediateLinePattern.containsMatchIn(it) }
                .mapNotNull { line ->
                    bareDollarPattern.find(line)?.groupValues?.get(1)
                        ?.let { ParseUtils.parseMoney(it).takeIf { v -> v > 0 } }
                        ?: Regex("""([\d,]+\.\d{2})\b""").find(line)?.groupValues?.get(1)
                            ?.let { ParseUtils.parseMoney(it).takeIf { v -> v > 0 } }
                }
                .firstOrNull()
        }

        // Prefer the last candidate (receipts print total at the bottom).
        return candidates.lastOrNull()
    }

    private fun extractDate(text: String): String? {
        dateLabeledPattern.find(text)?.groupValues?.get(1)?.let { raw ->
            ParseUtils.normalizeDate(raw).takeIf { it.isNotBlank() }?.let { return it }
            ParseUtils.normalizeTextMonthDate(raw).takeIf { it.isNotBlank() }?.let { return it }
        }
        dateIsoPattern.find(text)?.groupValues?.get(1)?.let { raw ->
            ParseUtils.normalizeDate(raw).takeIf { it.isNotBlank() }?.let { return it }
        }
        dateTextMonthPattern.find(text)?.groupValues?.get(1)?.let { raw ->
            ParseUtils.normalizeTextMonthDate(raw).takeIf { it.isNotBlank() }?.let { return it }
        }
        dateNumericPattern.find(text)?.groupValues?.get(1)?.let { raw ->
            ParseUtils.normalizeDate(raw).takeIf { it.isNotBlank() }?.let { return it }
        }
        return null
    }

    private fun extractServiceName(text: String, lines: List<String>): String? {
        val header = lines.take(5)
        val known = knownServices.firstOrNull { brand ->
            text.contains(brand, ignoreCase = true)
        }
        if (known != null) {
            val matchingLine = header.firstOrNull { line ->
                line.contains(known, ignoreCase = true) &&
                    line.length in known.length..80 &&
                    !line.contains('$')
            } ?: lines.firstOrNull { line ->
                line.contains(known, ignoreCase = true) &&
                    line.length in known.length..80 &&
                    !line.contains('$')
            }
            return matchingLine?.take(80) ?: known
        }

        return header.firstOrNull { line ->
            line.length in 3..60 &&
                !line.contains('$') &&
                !Regex("""^\d""").containsMatchIn(line) &&
                !skipVendorLinePattern.containsMatchIn(line) &&
                !Regex("""^\(?\d{3}\)?[.\-\s]?\d{3}[.\-\s]?\d{4}""").containsMatchIn(line) &&
                line.count { it.isLetter() } >= 3
        }
    }

    private fun extractDescription(lines: List<String>, serviceName: String?): String? =
        lines
            .asSequence()
            .filter { line ->
                line.length in 3..80 &&
                    !line.contains('$') &&
                    !Regex("""^\d""").containsMatchIn(line) &&
                    !skipDescriptionPattern.containsMatchIn(line) &&
                    !line.equals(serviceName, ignoreCase = true) &&
                    !(serviceName != null &&
                        line.contains(serviceName, ignoreCase = true) &&
                        line.length <= serviceName.length + 4)
            }
            .firstOrNull()

    private fun lineContaining(text: String, index: Int): String? {
        if (index !in text.indices) return null
        val start = text.lastIndexOf('\n', index - 1).let { if (it < 0) 0 else it + 1 }
        val end = text.indexOf('\n', index).let { if (it < 0) text.length else it }
        return text.substring(start, end).trim()
    }
}
