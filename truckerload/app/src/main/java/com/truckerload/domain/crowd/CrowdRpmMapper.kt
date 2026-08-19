package com.truckerload.domain.crowd

import com.truckerload.domain.model.Load
import com.truckerload.utils.extractStateFromLocation
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Maps a [Load] to [AnonymizedRpmSample].
 *
 * Reads only money, miles, PU/DEL **text for 2-letter state extraction**, and a
 * local timestamp used to compute ISO [AnonymizedRpmSample.weekNumber] / window
 * filters. Addresses, facility codes, trip ids, raw messages, and user identity
 * are discarded and never copied onto the sample.
 *
 * Do not log mapper output.
 */
object CrowdRpmMapper {

    val WEEK_MS: Long = TimeUnit.DAYS.toMillis(7)

    /**
     * Shareable sample, or null when rate/miles are not positive.
     * Region is `ST` or `ST-ST` (US state codes only) — never a city or address.
     */
    fun fromLoad(load: Load): AnonymizedRpmSample? {
        if (load.totalMiles <= 0.0 || load.totalRate <= 0.0) return null
        val rpm = load.totalRate / load.totalMiles
        val from = originState(load)
        val to = destState(load)
        val at = eventTimeMillis(load)
        return AnonymizedRpmSample(
            rpm = rpm,
            miles = load.totalMiles,
            region = regionOf(from, to),
            weekNumber = isoWeekNumber(at),
        )
    }

    fun fromLoads(loads: List<Load>): List<AnonymizedRpmSample> =
        loads.mapNotNull { fromLoad(it) }

    fun samplesInWindow(
        loads: List<Load>,
        nowMillis: Long = System.currentTimeMillis(),
        windowMs: Long = WEEK_MS,
    ): List<AnonymizedRpmSample> {
        val cutoff = nowMillis - windowMs
        val skew = nowMillis + TimeUnit.HOURS.toMillis(1)
        return loads.mapNotNull { load ->
            val at = eventTimeMillis(load)
            if (at < cutoff || at > skew) return@mapNotNull null
            fromLoad(load)
        }
    }

    fun weekSummary(
        loads: List<Load>,
        nowMillis: Long = System.currentTimeMillis(),
    ): CrowdRpmWeekSummary {
        val week = isoWeekNumber(nowMillis)
        val samples = samplesInWindow(loads, nowMillis, WEEK_MS)
        val miles = samples.sumOf { it.miles }
        val avgRpm = if (miles > 0.0) samples.sumOf { it.rpm * it.miles } / miles else 0.0
        return CrowdRpmWeekSummary(
            sampleCount = samples.size,
            avgRpm = avgRpm,
            totalMiles = miles,
            weekNumber = week,
        )
    }

    /** 2-letter origin state; input address is not retained. */
    fun originState(load: Load): String? = twoLetterState(load.pointA)

    /** 2-letter destination state; input address is not retained. */
    fun destState(load: Load): String? = twoLetterState(load.pointB)

    /**
     * Local event time for windowing only — never copied onto [AnonymizedRpmSample].
     */
    fun eventTimeMillis(load: Load): Long {
        if (load.parsedAt > 0L) return load.parsedAt
        if (load.updatedAt > 0L) return load.updatedAt
        return try {
            LocalDate.parse(load.date)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli()
        } catch (_: DateTimeParseException) {
            0L
        }
    }

    fun isoWeekNumber(epochMillis: Long): Int {
        val instant = Instant.ofEpochMilli(epochMillis.coerceAtLeast(0L))
        return instant.atZone(ZoneOffset.UTC).get(WeekFields.ISO.weekOfWeekBasedYear())
    }

    fun regionOf(fromState: String?, toState: String?): String? {
        val from = fromState?.takeIf { it.length == 2 }
        val to = toState?.takeIf { it.length == 2 }
        return when {
            from != null && to != null && from == to -> from
            from != null && to != null -> "$from-$to"
            from != null -> from
            to != null -> to
            else -> null
        }
    }

    private fun twoLetterState(location: String): String? {
        val code = extractStateFromLocation(location) ?: return null
        return code.takeIf { it.length == 2 && it.all { ch -> ch.isLetter() } }
            ?.uppercase(Locale.US)
    }
}
