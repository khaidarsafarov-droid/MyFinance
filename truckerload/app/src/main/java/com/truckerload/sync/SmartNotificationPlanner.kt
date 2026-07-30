package com.truckerload.sync

/**
 * Pure decisions for [SmartNotificationWorker].
 * Paycheck/diesel reminders only when the user actually hauled last week —
 * empty weeks must not spam the shade every 24h.
 */
object SmartNotificationPlanner {

    data class Plan(
        val notifyMissingPaycheck: Boolean,
        val notifyMissingDiesel: Boolean,
        val maintenanceDueTitles: List<String> = emptyList(),
    )

    fun plan(
        hasPaycheckForLastWeek: Boolean,
        dieselEntriesLastWeek: Int,
        maintenanceDueTitles: List<String> = emptyList(),
        hadLoadsLastWeek: Boolean = true,
    ): Plan =
        Plan(
            notifyMissingPaycheck = hadLoadsLastWeek && !hasPaycheckForLastWeek,
            notifyMissingDiesel = hadLoadsLastWeek && dieselEntriesLastWeek <= 0,
            maintenanceDueTitles = maintenanceDueTitles,
        )
}
