package com.truckerload.sync

/**
 * Pure decisions for [SmartNotificationWorker].
 * Safe with zero loads — paycheck/diesel for last week; maintenance due list is precomputed.
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
    ): Plan =
        Plan(
            notifyMissingPaycheck = !hasPaycheckForLastWeek,
            notifyMissingDiesel = dieselEntriesLastWeek <= 0,
            maintenanceDueTitles = maintenanceDueTitles,
        )
}
