package com.truckerload.domain.parser

import java.util.Calendar
import java.util.Locale

/**
 * Dates from Telegram chat history: Desktop HTML `title`, JSON `date` / `date_unixtime`,
 * and copied/exported text headers (`Name, [21.08.2025 02:09]`).
 *
 * Relay `Pu-time: MM/DD` has no year — the message timestamp is the year anchor.
 */
object TelegramMessageDate {

    data class Header(val index: Int, val millis: Long)

    private const val MIN_UNIX_SECONDS = 946_684_800L // 2000-01-01
    private const val MAX_UNIX_SECONDS = 4_102_444_800L // 2100-01-01

    private val DOT_DATETIME = Regex(
        """(\d{1,2})\.(\d{1,2})\.(\d{2,4})\s+(\d{1,2}):(\d{2})(?::(\d{2}))?""",
    )
    private val ISO_DATETIME = Regex(
        """(\d{4})-(\d{2})-(\d{2})[T ](\d{1,2}):(\d{2})(?::(\d{2}))?""",
    )
    private val US_DATETIME = Regex(
        """(\d{1,2})/(\d{1,2})/(\d{2,4})\s+(\d{1,2}):(\d{2})(?::(\d{2}))?(?:\s*([AaPp][Mm]))?""",
    )
    private val COPY_HEADER = Regex(
        """(?m)^[^\n]{0,80},\s*\[(\d{1,2})\.(\d{1,2})\.(\d{2,4})\s+(\d{1,2}):(\d{2})(?::(\d{2}))?\]""",
    )
    private val IOS_HEADER = Regex(
        """(?m)^\[(\d{1,2})\.(\d{1,2})\.(\d{2,4}),\s*(\d{1,2}):(\d{2})(?::(\d{2}))?\]""",
    )
    private val DESKTOP_TXT_HEADER = Regex(
        """(?m)^[^\n]{1,80}\s+(\d{1,2})\.(\d{1,2})\.(\d{4})\s+(\d{1,2}):(\d{2}):(\d{2})(?:\s+UTC[+-]\d{2}:?\d{2})?\s*$""",
    )
    private val COPY_TIME_ONLY = Regex(
        """(?m)^[^\n]{0,80},\s*\[(\d{1,2}):(\d{2})(?::(\d{2}))?\]""",
    )
    private val DAY_SEPARATOR = Regex(
        """(?im)^(?:\d{1,2}\s+(?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?|январ[яь]|феврал[яь]|марта?|апрел[яь]|ма[йя]|июн[яь]|июл[яь]|августа?|сентябр[яь]|октябр[яь]|ноябр[яь]|декабр[яь])\.?\s+\d{2,4}|\d{1,2}\.\d{1,2}\.\d{2,4})\s*$""",
    )

    fun parseToMillis(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        DOT_DATETIME.find(trimmed)?.let { return calendarFromDot(it) }
        ISO_DATETIME.find(trimmed)?.let { return calendarFromIso(it) }
        US_DATETIME.find(trimmed)?.let { return calendarFromUs(it) }
        ParseUtils.normalizeTextMonthDate(trimmed).takeIf { it.length >= 10 }?.let { iso ->
            return isoDateToMillis(iso)
        }
        ParseUtils.normalizeDate(trimmed).takeIf { it.length >= 10 }?.let { iso ->
            return isoDateToMillis(iso)
        }
        return null
    }

    fun parseUnixSeconds(value: Any?): Long? {
        if (value == null) return null
        return when (value) {
            is Number -> value.toLong().takeIf { it in MIN_UNIX_SECONDS..MAX_UNIX_SECONDS }
            is String -> {
                val trimmed = value.trim()
                trimmed.toLongOrNull()?.takeIf { it in MIN_UNIX_SECONDS..MAX_UNIX_SECONDS }
                    ?: parseToMillis(trimmed)?.div(1000L)
            }
            else -> null
        }
    }

    fun scanChatHistory(text: String): List<Header> {
        if (text.isBlank()) return emptyList()
        val headers = mutableListOf<Header>()

        fun addDot(match: MatchResult) {
            calendarFromDot(match)?.let { headers.add(Header(match.range.first, it)) }
        }
        COPY_HEADER.findAll(text).forEach(::addDot)
        IOS_HEADER.findAll(text).forEach(::addDot)
        DESKTOP_TXT_HEADER.findAll(text).forEach { match ->
            parseToMillis(match.value)?.let { headers.add(Header(match.range.first, it)) }
        }
        DAY_SEPARATOR.findAll(text).forEach { match ->
            parseToMillis(match.value)?.let { headers.add(Header(match.range.first, it)) }
        }
        headers.sortBy { it.index }
        if (headers.isEmpty()) return headers

        COPY_TIME_ONLY.findAll(text).forEach { match ->
            if (COPY_HEADER.containsMatchIn(match.value)) return@forEach
            val hour = match.groupValues[1].toIntOrNull() ?: return@forEach
            val minute = match.groupValues[2].toIntOrNull() ?: return@forEach
            val second = match.groupValues[3].toIntOrNull() ?: 0
            val preceding = headers.lastOrNull { it.index <= match.range.first } ?: headers.first()
            headers.add(Header(match.range.first, applyTime(startOfDay(preceding.millis), hour, minute, second)))
        }
        headers.sortBy { it.index }
        return headers
    }

    fun referenceMillisAt(headers: List<Header>, offset: Int, fallback: Long): Long =
        headers.lastOrNull { it.index <= offset }?.millis ?: fallback

    private fun calendarFromDot(match: MatchResult): Long? {
        val day = match.groupValues[1].toIntOrNull() ?: return null
        val month = match.groupValues[2].toIntOrNull() ?: return null
        val year = expandYear(match.groupValues[3].toIntOrNull() ?: return null)
        val hour = match.groupValues[4].toIntOrNull() ?: 12
        val minute = match.groupValues[5].toIntOrNull() ?: 0
        val second = match.groupValues.getOrNull(6)?.toIntOrNull() ?: 0
        return calendarAt(year, month, day, hour, minute, second)
    }

    private fun calendarFromIso(match: MatchResult): Long? {
        val year = match.groupValues[1].toIntOrNull() ?: return null
        val month = match.groupValues[2].toIntOrNull() ?: return null
        val day = match.groupValues[3].toIntOrNull() ?: return null
        val hour = match.groupValues[4].toIntOrNull() ?: 0
        val minute = match.groupValues[5].toIntOrNull() ?: 0
        val second = match.groupValues.getOrNull(6)?.toIntOrNull() ?: 0
        return calendarAt(year, month, day, hour, minute, second)
    }

    private fun calendarFromUs(match: MatchResult): Long? {
        val a = match.groupValues[1].toIntOrNull() ?: return null
        val b = match.groupValues[2].toIntOrNull() ?: return null
        val year = expandYear(match.groupValues[3].toIntOrNull() ?: return null)
        var hour = match.groupValues[4].toIntOrNull() ?: return null
        val minute = match.groupValues[5].toIntOrNull() ?: 0
        val second = match.groupValues.getOrNull(6)?.toIntOrNull() ?: 0
        val ampm = match.groupValues.getOrNull(7).orEmpty()
        if (ampm.isNotBlank()) {
            val pm = ampm.equals("pm", ignoreCase = true)
            hour = when {
                pm && hour < 12 -> hour + 12
                !pm && hour == 12 -> 0
                else -> hour
            }
        }
        val (month, day) = when {
            a > 12 && b in 1..12 -> b to a
            else -> a to b
        }
        return calendarAt(year, month, day, hour, minute, second)
    }

    private fun expandYear(year: Int): Int = if (year in 0..99) 2000 + year else year

    private fun calendarAt(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Long? {
        if (month !in 1..12 || day !in 1..31 || year !in 1970..2100) return null
        if (hour !in 0..23 || minute !in 0..59 || second !in 0..59) return null
        return Calendar.getInstance(Locale.US).apply {
            set(year, month - 1, day, hour, minute, second)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun isoDateToMillis(iso: String): Long? {
        val parts = iso.split("-")
        if (parts.size < 3) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val d = parts[2].take(2).toIntOrNull() ?: return null
        return calendarAt(y, m, d, 12, 0, 0)
    }

    private fun startOfDay(millis: Long): Long =
        Calendar.getInstance(Locale.US).apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun applyTime(dayMillis: Long, hour: Int, minute: Int, second: Int): Long =
        Calendar.getInstance(Locale.US).apply {
            timeInMillis = dayMillis
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, second.coerceIn(0, 59))
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
