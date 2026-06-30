package com.truckerload.utils

/**
 * Извлекает аббревиатуру штата из строки локации.
 * Поддерживает форматы: "City, ST", "City, ST USA", "City ST"
 */
fun extractStateFromLocation(location: String): String? {
    if (location.isBlank()) return null
    val t = location.trim()
    val afterComma = t.substringAfterLast(",", "").trim()
    if (afterComma.length == 2 && afterComma.all { it.isLetter() }) return afterComma.uppercase()
    Regex("\\b([A-Za-z]{2})\\s*$").find(t)?.groupValues?.get(1)?.let {
        if (it.all { c -> c.isLetter() }) return it.uppercase()
    }
    return null
}
