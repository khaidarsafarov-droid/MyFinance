package com.truckerload.data.community

import java.time.Instant

internal object CommunityTime {
    fun parseMillis(raw: String): Long {
        if (raw.isBlank()) return 0L
        raw.toLongOrNull()?.let { return it }
        return runCatching { Instant.parse(raw).toEpochMilli() }.getOrDefault(0L)
    }

    fun toIso(millis: Long): String = Instant.ofEpochMilli(millis).toString()
}
