package com.truckerload.utils.ocr

object LanguageDetector {

    fun detect(text: String): String {
        if (text.isBlank()) return "unknown"
        val cyrillic = text.count { it in '\u0400'..'\u04FF' }
        val latin = text.count { it in 'A'..'Z' || it in 'a'..'z' }
        val letters = cyrillic + latin
        if (letters == 0) return "unknown"
        return when {
            cyrillic.toDouble() / letters >= 0.3 -> "ru"
            latin.toDouble() / letters >= 0.5 -> "en"
            else -> "mixed"
        }
    }

    fun isRussianText(text: String): Boolean = detect(text) == "ru"

    fun isLatinText(text: String): Boolean {
        val detected = detect(text)
        return detected == "en" || (detected == "mixed" && !isRussianText(text))
    }
}
