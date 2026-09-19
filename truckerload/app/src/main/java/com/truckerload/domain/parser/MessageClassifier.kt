package com.truckerload.domain.parser

/**
 * Classifies inbound bot text using keyword/regex rules (no external AI).
 */
object MessageClassifier {

    private val loadMarkers = Regex(
        """Trip\s*ID|Trip\nID|PU#|P/U\s*#|Total\s*Rate|rate[\s\-]*confirmation|""" +
            """load[\s\-]*confirmation|estimated\s*rate|IEL\s*PO|load\s*information""",
        RegexOption.IGNORE_CASE,
    )
    private val paycheckMarkers = Regex(
        """Grand\s*Total|Settlement\s*Date|Cutoff\s*Date|Driver\s*Settlement|Зарплата|""" +
            """Net\s*(?:Pay|Earnings|Settlement)|Gross\s*(?:Pay|Earnings)|Take[\s-]*Home|""" +
            """Pay\s*Period|Pay\s*Statement|Pay\s*Stub|Compensation\s*Summary""",
        RegexOption.IGNORE_CASE
    )
    private val dieselMarkers = Regex(
        """(?:diesel|fuel\s*receipt|fuel\s*sale|gallons?|price\s*per\s*gallon|gal\s*@|""" +
            """топлив|дизел|ulsd|#2\s*diesel|pump\s*total)""",
        RegexOption.IGNORE_CASE
    )

    fun classify(text: String): MessageType {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return MessageType.UNKNOWN
        if (paycheckMarkers.containsMatchIn(trimmed) && PaycheckTextParser.looksLikePaycheck(trimmed)) {
            return MessageType.PAYCHECK
        }
        if (loadMarkers.containsMatchIn(trimmed)) return MessageType.LOAD
        if (dieselMarkers.containsMatchIn(trimmed) && DieselTextParser.looksLikeDiesel(trimmed)) {
            return MessageType.DIESEL
        }
        return MessageType.UNKNOWN
    }

    fun isLoadLike(text: String): Boolean = loadMarkers.containsMatchIn(text)
}
