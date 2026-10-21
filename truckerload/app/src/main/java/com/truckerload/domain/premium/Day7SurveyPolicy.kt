package com.truckerload.domain.premium

import java.time.Instant
import java.time.ZoneId

/**
 * Weekly free-version prompt. First eligible at [Record.firstLaunchAt] + 7 days.
 * Banner and notification share [Record.lastShownAt], so a display on either
 * channel blocks the other until another 7 days have passed. The same local
 * day is never used twice. "It's enough" sets [Record.optedOut] forever.
 * Active Premium suppresses the prompt without changing billing.
 */
object Day7SurveyPolicy {
    const val CADENCE_MILLIS: Long = 7L * 24L * 60L * 60L * 1000L

    data class Record(
        val firstLaunchAt: Long?,
        val lastShownAt: Long?,
        val optedOut: Boolean,
    )

    /** Existing installs get [nowMillis] the first time this version runs. */
    fun withFirstLaunch(record: Record, nowMillis: Long): Record =
        if (record.firstLaunchAt == null) record.copy(firstLaunchAt = nowMillis) else record

    fun shouldShow(
        record: Record,
        nowMillis: Long,
        premiumActive: Boolean,
        zone: ZoneId,
    ): Boolean {
        val firstLaunchAt = record.firstLaunchAt ?: return false
        if (record.optedOut || premiumActive) return false
        if (nowMillis < firstLaunchAt + CADENCE_MILLIS) return false
        val lastShownAt = record.lastShownAt ?: return true
        if (sameLocalDay(lastShownAt, nowMillis, zone)) return false
        return nowMillis >= lastShownAt + CADENCE_MILLIS
    }

    /**
     * Stamp a display. A second call inside the same 7-day window keeps the
     * original [Record.lastShownAt] so a session recomposition does not re-count.
     */
    fun recordDisplay(record: Record, nowMillis: Long): Record {
        val lastShownAt = record.lastShownAt
        if (lastShownAt != null && nowMillis < lastShownAt + CADENCE_MILLIS) return record
        return record.copy(lastShownAt = nowMillis)
    }

    fun optOut(record: Record): Record = record.copy(optedOut = true)

    /**
     * Delay for the next one-shot check. Null means the user opted out and
     * the job should be cancelled. A due prompt that cannot be posted
     * (Premium, or notifications not allowed) is retried one cadence later
     * instead of spinning. Time that is not yet eligible still waits only
     * until the first 7-day mark.
     */
    fun nextCheckDelayMillis(
        record: Record,
        nowMillis: Long,
        premiumActive: Boolean,
        notificationsPermitted: Boolean,
    ): Long? {
        if (record.optedOut) return null
        val firstLaunchAt = record.firstLaunchAt ?: return CADENCE_MILLIS
        val notBefore = firstLaunchAt + CADENCE_MILLIS
        val afterLast = record.lastShownAt?.plus(CADENCE_MILLIS) ?: notBefore
        val wait = (maxOf(notBefore, afterLast) - nowMillis).coerceAtLeast(0L)
        if (wait == 0L && (premiumActive || !notificationsPermitted)) return CADENCE_MILLIS
        return wait
    }

    fun sameLocalDay(leftMillis: Long, rightMillis: Long, zone: ZoneId): Boolean {
        val left = Instant.ofEpochMilli(leftMillis).atZone(zone).toLocalDate()
        val right = Instant.ofEpochMilli(rightMillis).atZone(zone).toLocalDate()
        return left == right
    }
}
