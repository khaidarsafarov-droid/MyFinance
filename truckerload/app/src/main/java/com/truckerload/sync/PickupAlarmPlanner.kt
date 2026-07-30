package com.truckerload.sync

/** Pure logic for pickup reminder timing (unit-testable). */
object PickupAlarmPlanner {

    const val OFFSET_TWO_HOURS_MS = 2L * 60L * 60L * 1000L
    const val OFFSET_ONE_HOUR_MS = 60L * 60L * 1000L

    fun shouldPromptForAlarm(pickupMillis: Long?, nowMillis: Long): Boolean =
        pickupMillis != null && pickupMillis > nowMillis

    fun alarmAtOffset(pickupMillis: Long, offsetMs: Long, nowMillis: Long): Long? {
        val trigger = pickupMillis - offsetMs
        return trigger.takeIf { it > nowMillis }
    }

    fun alarmAtCustom(customMillis: Long, nowMillis: Long): Long? =
        customMillis.takeIf { it > nowMillis }
}
