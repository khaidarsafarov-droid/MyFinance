package com.truckerload.domain.parser

/**
 * Extracts service name, total amount, date, and description hints from service invoice OCR text.
 */
object ServiceReceiptTextParser {

    private val knownServices = listOf(
        "Love's",
        "Loves",
        "TA Truck Service",
        "TA Petro",
        "TravelCenters",
        "Travel Centers",
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
    )

    private val totalPatterns = listOf(
        Regex(
            """(?:Grand\s*Total|Amount\s*Due|Balance\s*Due|Total\s*Due|Total\s*Amount|Amount|Итого|Всего|Сумма|Total)\s*[:\s]*\$?\s*([\d\s,]+\.?\d*)""",
            RegexOption.IGNORE_CASE,
        ),
        Regex("""\$\s*([\d,]+\.\d{2})\b"""),
    )

    private val datePatterns = listOf(
        Regex("""(?:Date|Дата|Invoice\s*Date)\s*[:\s]*([^\n]+)""", RegexOption.IGNORE_CASE),
        Regex("""\b(\d{4}-\d{2}-\d{2})\b"""),
        Regex("""\b(\d{1,2}[./-]\d{1,2}[./-]\d{2,4})\b"""),
    )

    private val skipLinePattern = Regex(
        """total|итого|amount|date|дата|invoice|phone|tel|fax|www\.|http|receipt|thank|subtotal|tax|qty|item|visa|mastercard|cash|change\s+due|balance\s+due|miles?|mi\b|gal|gallon""",
        RegexOption.IGNORE_CASE,
    )

    data class ParseResult(
        val amount: Double?,
        val date: String?,
        val serviceName: String?,
        val descriptionHint: String?,
    )

    fun parse(text: String): ParseResult {
        if (text.isBlank()) {
            return ParseResult(null, null, null, null)
        }

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

        val lines = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()

        val serviceName = extractServiceName(text, lines)

        val descriptionHint = lines
            .asSequence()
            .filter { line ->
                line.length in 3..80 &&
                    !line.contains('$') &&
                    !Regex("""^\d""").containsMatchIn(line) &&
                    !skipLinePattern.containsMatchIn(line) &&
                    !line.equals(serviceName, ignoreCase = true) &&
                    !(serviceName != null && line.contains(serviceName, ignoreCase = true) &&
                        line.length <= serviceName.length + 4)
            }
            .firstOrNull()

        return ParseResult(
            amount = amount,
            date = date,
            serviceName = serviceName,
            descriptionHint = descriptionHint,
        )
    }

    private fun extractServiceName(text: String, lines: List<String>): String? {
        val known = knownServices.firstOrNull { brand ->
            text.contains(brand, ignoreCase = true)
        }
        if (known != null) {
            // Prefer the OCR line that contains the brand (often full store name).
            val matchingLine = lines.firstOrNull { line ->
                line.contains(known, ignoreCase = true) &&
                    line.length in known.length..80 &&
                    !line.contains('$')
            }
            return matchingLine?.take(80) ?: known
        }

        return lines
            .asSequence()
            .take(6)
            .firstOrNull { line ->
                line.length in 3..60 &&
                    !line.contains('$') &&
                    !Regex("""^\d""").containsMatchIn(line) &&
                    !skipLinePattern.containsMatchIn(line) &&
                    !Regex("""^\(?\d{3}\)?[.\-\s]?\d{3}[.\-\s]?\d{4}""").containsMatchIn(line) &&
                    line.count { it.isLetter() } >= 3
            }
    }
}
