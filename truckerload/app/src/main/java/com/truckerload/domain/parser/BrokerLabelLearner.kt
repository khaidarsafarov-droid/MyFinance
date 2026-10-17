package com.truckerload.domain.parser

/**
 * The words on the same line, before a load number or a rate the driver pointed at.
 */
object BrokerLabelLearner {

    fun labelBefore(text: String, token: String): String? {
        val needle = token.trim()
        if (needle.length < 2) return null
        val digits = needle.filter { it.isDigit() }
        for (line in text.lines()) {
            val at = indexOnLine(line, needle, digits) ?: continue
            val before = line.substring(0, at)
                .trim()
                .trimEnd(':', '-', '$', ' ')
                .trim()
            val words = before.split(Regex("\\s+")).filter { it.isNotBlank() }.takeLast(4)
            val label = words.joinToString(" ")
            if (label.length < 2 || label.none { it.isLetter() }) continue
            if (GENERIC.contains(label.lowercase())) continue
            return label
        }
        return null
    }

    private fun indexOnLine(line: String, needle: String, digits: String): Int? {
        val direct = line.indexOf(needle, ignoreCase = true)
        if (direct >= 0) return direct
        if (digits.length < 3) return null
        if (!line.filter { it.isDigit() }.contains(digits)) return null
        val firstDigit = line.indexOfFirst { it.isDigit() }
        return firstDigit.takeIf { it >= 0 }
    }

    private val GENERIC = setOf(
        "total",
        "rate",
        "amount",
        "date",
        "miles",
        "price",
        "stop",
        "pickup",
        "delivery",
    )
}
