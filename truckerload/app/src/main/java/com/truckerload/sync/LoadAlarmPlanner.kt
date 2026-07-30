package com.truckerload.sync

/**
 * Pure decisions for offering / computing load pickup alarms.
 * Safe to unit-test without Android framework.
 */
object LoadAlarmPlanner {

    const val HOUR_MS = 3_600_000L

    enum class Preset(val hoursBefore: Int) {
        TWO_HOURS(2),
        ONE_HOUR(1),
    }

    data class Offer(
        val pickupMillis: Long,
        val availablePresets: List<Preset>,
        /** True when at least one preset or a custom time can still be chosen. */
        val canOffer: Boolean,
    )

    /** Offer an alarm only when pickup is strictly in the future. */
    fun shouldOfferAlarm(pickupMillis: Long?, nowMillis: Long): Boolean {
        if (pickupMillis == null) return false
        return pickupMillis > nowMillis
    }

    fun buildOffer(pickupMillis: Long, nowMillis: Long): Offer? {
        if (!shouldOfferAlarm(pickupMillis, nowMillis)) return null
        val presets = Preset.entries.filter { preset ->
            isValidAlarmTime(triggerAt(pickupMillis, preset.hoursBefore), pickupMillis, nowMillis)
        }
        return Offer(
            pickupMillis = pickupMillis,
            availablePresets = presets,
            canOffer = true, // custom time always possible while pickup is future
        )
    }

    fun triggerAt(pickupMillis: Long, hoursBefore: Int): Long =
        pickupMillis - hoursBefore * HOUR_MS

    fun isValidAlarmTime(triggerAtMillis: Long, pickupMillis: Long, nowMillis: Long): Boolean =
        triggerAtMillis > nowMillis && triggerAtMillis < pickupMillis
}
