package com.truckerload.domain.alarm

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.StopType
import com.truckerload.utils.parseScheduledTimeToMillis

/**
 * Pure decisions for pickup alarms offered right after the driver adds a load.
 */
object LoadAlarmPlanner {

    const val HOUR_MS = 60L * 60L * 1000L
    const val TWO_HOURS_MS = 2L * HOUR_MS

    enum class Preset {
        TWO_HOURS_BEFORE,
        ONE_HOUR_BEFORE,
        CUSTOM,
    }

    data class Offer(
        val pickupMillis: Long,
        val availablePresets: List<Preset>,
    )

    /**
     * Offer an alarm only when the first PU stop has a clock time and that time is still in the future.
     */
    fun offerForLoad(load: Load, nowMillis: Long = System.currentTimeMillis()): Offer? {
        val pickupMillis = firstPickUpClockMillis(load) ?: return null
        if (pickupMillis <= nowMillis) return null
        val presets = buildList {
            if (pickupMillis - TWO_HOURS_MS > nowMillis) add(Preset.TWO_HOURS_BEFORE)
            if (pickupMillis - HOUR_MS > nowMillis) add(Preset.ONE_HOUR_BEFORE)
            add(Preset.CUSTOM)
        }
        return Offer(pickupMillis = pickupMillis, availablePresets = presets)
    }

    fun triggerMillis(preset: Preset, pickupMillis: Long, customMillis: Long? = null): Long? =
        when (preset) {
            Preset.TWO_HOURS_BEFORE -> pickupMillis - TWO_HOURS_MS
            Preset.ONE_HOUR_BEFORE -> pickupMillis - HOUR_MS
            Preset.CUSTOM -> customMillis
        }

    fun isValidTrigger(triggerMillis: Long, pickupMillis: Long, nowMillis: Long): Boolean =
        triggerMillis > nowMillis && triggerMillis <= pickupMillis

    fun defaultCustomMillis(pickupMillis: Long, nowMillis: Long): Long {
        val twoHoursBefore = pickupMillis - TWO_HOURS_MS
        if (twoHoursBefore > nowMillis) return twoHoursBefore
        val oneHourBefore = pickupMillis - HOUR_MS
        if (oneHourBefore > nowMillis) return oneHourBefore
        // Slightly in the future so the TimePicker opens on a schedulable instant.
        return (nowMillis + 15L * 60L * 1000L).coerceAtMost(pickupMillis)
    }

    private fun firstPickUpClockMillis(load: Load): Long? =
        load.stops
            .filter { it.type == StopType.PU && hasClockTime(it.scheduledTime) }
            .mapNotNull { parseScheduledTimeToMillis(it.scheduledTime) }
            .minOrNull()

    private fun hasClockTime(scheduledTime: String): Boolean =
        CLOCK_TIME.containsMatchIn(scheduledTime)

    private val CLOCK_TIME = Regex("""\d{1,2}:\d{2}""")
}
