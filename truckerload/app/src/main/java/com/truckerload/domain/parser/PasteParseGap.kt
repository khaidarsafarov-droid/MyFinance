package com.truckerload.domain.parser

/**
 * What a pasted Relay/Telegram block is still missing — shown before Save,
 * in the driver's words, not as a generic parse failure.
 */
enum class PasteParseGap {
    MISSING_RATE,
    MISSING_ADDRESS,
    MISSING_BOTH,
    INCOMPLETE,
}

object PasteParseHint {
    private val rateLabel = Regex("(?i)total\\s*rate|gross\\s*pay|line\\s*haul")
    private val money = Regex("""\$\s*\d|\d+[.,]\d{2}""")
    private val addressLabel = Regex("(?i)pu-?address|del-?address|pick\\s*up|delivery")
    private val cityState = Regex("""[A-Za-z .'-]+,\s*[A-Z]{2}\b""")

    fun of(text: String): PasteParseGap {
        val hasRate = rateLabel.containsMatchIn(text) || money.containsMatchIn(text)
        val hasAddress = addressLabel.containsMatchIn(text) || cityState.containsMatchIn(text)
        return when {
            !hasRate && !hasAddress -> PasteParseGap.MISSING_BOTH
            !hasRate -> PasteParseGap.MISSING_RATE
            !hasAddress -> PasteParseGap.MISSING_ADDRESS
            else -> PasteParseGap.INCOMPLETE
        }
    }
}
