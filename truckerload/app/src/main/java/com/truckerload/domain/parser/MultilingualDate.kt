@file:Suppress("MagicNumber", "ReturnCount", "MaxLineLength")

package com.truckerload.domain.parser

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Dates in dispatcher chat (EN / RU / ES / UZ): today/tomorrow words, weekday names and numeric
 * `MM/DD`, `DD/MM`, `DD.MM` dates without a year. The year is inferred from [today].
 *
 * Slash order is decided for the whole document: a second part above 12 (`11/30`) forces month-first
 * for every slash date in it (so `12/01` is Dec 1), a first part above 12 (`29/11`) forces day-first;
 * otherwise US month-first. Dots are always day-month.
 */
internal object MultilingualDate {
    private const val STALE_DAYS = 45L
    private const val MAX_MONTH = 12
    private const val MAX_DAY = 31
    private const val TWO_DIGIT_YEAR_BASE = 2000
    private const val LETTERS = "(?<![\\p{L}\\p{N}])"
    private const val END = "(?![\\p{L}\\p{N}])"

    private val relative = listOf(
        "послезавтра|pasado mañana|pasado manana|indinga" to 2L,
        "сегодня|today|hoy|bugun|bugün" to 0L,
        "завтра|tomorrow|mañana|manana|ertaga" to 1L,
    )

    private val weekdays: List<Pair<String, DayOfWeek>> = listOf(
        "понедельник|monday|mon|lunes|dushanba" to DayOfWeek.MONDAY,
        "вторник|tuesday|tues?|martes|seshanba" to DayOfWeek.TUESDAY,
        "среда|среду|wednesday|wed|mi[eé]rcoles|chorshanba" to DayOfWeek.WEDNESDAY,
        "четверг|thursday|thu(?:rs?)?|jueves|payshanba" to DayOfWeek.THURSDAY,
        "пятниц[аyу]|friday|fri|viernes|juma" to DayOfWeek.FRIDAY,
        "суббот[аyу]|saturday|sat|s[aá]bado|shanba" to DayOfWeek.SATURDAY,
        "воскресенье|sunday|sun|domingo|yakshanba" to DayOfWeek.SUNDAY,
    )

    private val slash = Regex("(?<![\\d/$.])(\\d{1,2})/(\\d{1,2})(?:/(\\d{4}|\\d{2}))?(?![\\d/])")
    private val dot = Regex("(?<![\\d$.,])(\\d{2})\\.(\\d{2})(?:\\.(\\d{4}|\\d{2}))?(?![\\d.])")

    private class Hit(val position: Int, val date: LocalDate)

    /** Day-first when the document says so (a first part above 12 and no second part above 12). */
    private fun dayFirst(doc: String): Boolean {
        val parts = slash.findAll(doc).map { it.groupValues[1].toInt() to it.groupValues[2].toInt() }.toList()
        if (parts.any { it.second > MAX_MONTH && it.first <= MAX_MONTH }) return false
        return parts.any { it.first > MAX_MONTH && it.second <= MAX_MONTH }
    }

    /** First date in [text]; [doc] (the whole message) decides the slash order. Null when none. */
    fun extract(text: String, today: LocalDate, doc: String = text): LocalDate? {
        val dayFirst = dayFirst(doc)
        val hits = mutableListOf<Hit>()
        relative.forEach { (words, offset) ->
            Regex("$LETTERS(?:$words)$END", RegexOption.IGNORE_CASE).findAll(text)
                .forEach { hits += Hit(it.range.first, today.plusDays(offset)) }
        }
        weekdays.forEach { (words, day) ->
            Regex("$LETTERS(?:$words)$END", RegexOption.IGNORE_CASE).findAll(text)
                .forEach { hits += Hit(it.range.first, nextWeekday(today, day)) }
        }
        slash.findAll(text).forEach { m ->
            if (m.value == "24/7") return@forEach
            val (a, b) = m.groupValues[1].toInt() to m.groupValues[2].toInt()
            val (month, day) = if (dayFirst) b to a else a to b
            dateOf(today, month, day, m.groupValues[3])?.let { hits += Hit(m.range.first, it) }
        }
        dot.findAll(text).forEach { m ->
            dateOf(today, m.groupValues[2].toInt(), m.groupValues[1].toInt(), m.groupValues[3])?.let { hits += Hit(m.range.first, it) }
        }
        return hits.minByOrNull { it.position }?.date
    }

    private fun nextWeekday(today: LocalDate, day: DayOfWeek): LocalDate =
        today.plusDays(((day.value - today.dayOfWeek.value + 7) % 7).toLong())

    private fun dateOf(today: LocalDate, month: Int, day: Int, yearText: String): LocalDate? {
        if (month !in 1..MAX_MONTH || day !in 1..MAX_DAY) return null
        if (yearText.isNotEmpty()) {
            val year = yearText.toInt().let { if (it < 100) it + TWO_DIGIT_YEAR_BASE else it }
            return runCatching { LocalDate.of(year, month, day) }.getOrNull()
        }
        val thisYear = runCatching { LocalDate.of(today.year, month, day) }.getOrNull() ?: return null
        return if (thisYear.isBefore(today.minusDays(STALE_DAYS))) thisYear.plusYears(1) else thisYear
    }
}
