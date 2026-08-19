package com.truckerload.data.community

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

internal object CommunityTime {
    private val SPACE_THEN_T = Regex("(\\d{4}-\\d{2}-\\d{2}) (\\d)")
    private val OFFSET_BARE = Regex("([+-]\\d{2})$")
    private val FLEXIBLE_OFFSET: DateTimeFormatter = DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        .optionalStart()
        .appendOffset("+HH:MM", "Z")
        .optionalEnd()
        .optionalStart()
        .appendOffset("+HHMM", "Z")
        .optionalEnd()
        .optionalStart()
        .appendOffset("+HH", "Z")
        .optionalEnd()
        .parseDefaulting(ChronoField.OFFSET_SECONDS, 0)
        .toFormatter()

    fun parseMillis(raw: String): Long {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed.equals("null", ignoreCase = true)) return 0L
        parseEpochNumber(trimmed)?.let { return it }
        val normalized = normalizeTimestamp(trimmed)
        runCatching { Instant.parse(normalized) }.getOrNull()?.toEpochMilli()?.let { return it }
        runCatching { OffsetDateTime.parse(normalized) }.getOrNull()?.toInstant()?.toEpochMilli()
            ?.let { return it }
        runCatching { OffsetDateTime.parse(normalized, FLEXIBLE_OFFSET) }.getOrNull()
            ?.toInstant()?.toEpochMilli()?.let { return it }
        runCatching {
            LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli()
        }.getOrNull()?.let { return it }
        return 0L
    }

    fun toIso(millis: Long): String = Instant.ofEpochMilli(millis).toString()

    private fun parseEpochNumber(raw: String): Long? {
        val asLong = raw.toLongOrNull() ?: raw.toDoubleOrNull()?.toLong() ?: return null
        return when {
            asLong <= 0L -> 0L
            asLong < 10_000_000_000L -> asLong * 1_000L
            else -> asLong
        }
    }

    private fun normalizeTimestamp(raw: String): String {
        var value = SPACE_THEN_T.replace(raw) { "${it.groupValues[1]}T${it.groupValues[2]}" }
        value = OFFSET_BARE.replace(value) { "${it.groupValues[1]}:00" }
        if (value.endsWith("+00:00")) value = value.dropLast(6) + "Z"
        return value
    }
}
