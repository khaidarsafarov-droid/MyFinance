package com.truckerload.voice

import com.truckerload.domain.assistant.AssistantToolCall

/**
 * On-device intent parser for the in-app voice assistant.
 * No cloud LLM — maps RU/EN transcripts onto [AssistantToolCall].
 */
object LocalSpokenAssistantParser {
    private val NUMBER = Regex("""(\d+(?:[.,]\d+)?)""")
    private val CURRENCY_AMOUNT = Regex(
        """(?:\$\s*)?(\d+(?:[.,]\d+)?)\s*(?:доллар(?:ов|а)?|бакс(?:ов|а)?|dollar(?:s)?|buck(?:s)?|usd|\$)?""",
        RegexOption.IGNORE_CASE,
    )
    private val GALLONS = Regex(
        """(\d+(?:[.,]\d+)?)\s*(?:галлон(?:ов|а)?|gallon(?:s)?|gal\.?)""" +
                """|(?:галлон(?:ов|а)?|gallon(?:s)?|gal\.?)\s*(\d+(?:[.,]\d+)?)""",
        RegexOption.IGNORE_CASE,
    )
    private val FILLER = Regex(
        """\b(?:привет|здравствуй(?:те)?|пожалуйста|please|hello|hi|hey|can you|could you|""" +
                """добавь(?:те)?\s+мне|add\s+me|мне)\b""",
        RegexOption.IGNORE_CASE,
    )

    fun parse(transcript: String): AssistantToolCall? {
        val key = AppVoiceActions.normalize(FILLER.replace(transcript, " "))
            .replace(Regex("""\s+"""), " ")
            .trim()
        if (key.isBlank()) return null

        if (AppVoiceJournal.isWeeklyGrossQuery(key) ||
            key == AppVoiceJournal.PATH_WEEKLY_GROSS ||
            key == "gross"
        ) {
            return AssistantToolCall.QueryWeeklyGross(
                weekNumber = AppVoiceJournal.parseSpokenWeekNumber(key),
                year = AppVoiceJournal.parseSpokenYear(key),
            )
        }

        if (isDiesel(key)) {
            val gallons = extractGallons(key)
            val amount = extractAmount(key, exclude = gallons) ?: return null
            return AssistantToolCall.AddDiesel(amount = amount, gallons = gallons, date = null)
        }

        if (isPaycheck(key)) {
            val amount = extractAmount(key, exclude = null) ?: return null
            return AssistantToolCall.AddPaycheck(
                amount = amount,
                weekNumber = AppVoiceJournal.parseSpokenWeekNumber(key),
                year = AppVoiceJournal.parseSpokenYear(key),
            )
        }

        return null
    }

    private fun isDiesel(key: String): Boolean {
        return listOf(
            "дизель",
            "diesel",
            "топлив",
            "fuel",
            "солярр",
            "gas"
        ).any { key.contains(it) }
    }

    private fun isPaycheck(key: String): Boolean {
        return listOf(
            "зарплат",
            "paycheck",
            "settlement",
            "оклад",
            "выплат",
            "payroll",
        ).any { key.contains(it) }
    }

    private fun extractGallons(key: String): Double? {
        val match = GALLONS.find(key) ?: return null
        val raw = match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }
            ?: match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }
        return AppVoiceJournal.parseAmount(raw)
    }

    private fun extractAmount(key: String, exclude: Double?): Double? {
        val withoutGallons = GALLONS.replace(key, " ")
        CURRENCY_AMOUNT.findAll(withoutGallons).forEach { match ->
            val value =
                AppVoiceJournal.parseAmount(match.groupValues.getOrNull(1)) ?: return@forEach
            if (exclude == null || value != exclude) return value
        }
        NUMBER.findAll(withoutGallons).forEach { match ->
            val value =
                AppVoiceJournal.parseAmount(match.groupValues.getOrNull(1)) ?: return@forEach
            if (exclude == null || value != exclude) return value
        }
        return null
    }
}
