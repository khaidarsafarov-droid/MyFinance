package com.truckerload.sync

/**
 * Pure decisions for [SmartNotificationWorker].
 * Safe with zero loads — the worker never queries loads; only paycheck/diesel for last week.
 */
object SmartNotificationPlanner {

    data class Plan(
        val notifyMissingPaycheck: Boolean,
        val notifyMissingDiesel: Boolean,
    )

    fun plan(hasPaycheckForLastWeek: Boolean, dieselEntriesLastWeek: Int): Plan =
        Plan(
            notifyMissingPaycheck = !hasPaycheckForLastWeek,
            notifyMissingDiesel = dieselEntriesLastWeek <= 0,
        )
}
