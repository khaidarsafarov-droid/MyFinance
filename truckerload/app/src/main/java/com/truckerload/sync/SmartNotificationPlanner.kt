package com.truckerload.sync

/**
 * Pure decisions for [SmartNotificationWorker].
 * Safe with zero loads — paycheck/diesel for last week; maintenance due list is precomputed.
 *
 * Reminder spam is controlled by [alreadyNotifiedMissingWeek]: callers pass true when
 * paycheck/diesel for that week were already nudged this week, so we emit at most once.
 * Maintenance titles are returned as a list so the worker can post a single bundled alert.
 */
object SmartNotificationPlanner {

    data class Plan(
        val notifyMissingPaycheck: Boolean,
        val notifyMissingDiesel: Boolean,
        val maintenanceDueTitles: List<String> = emptyList(),
    ) {
        val hasAny: Boolean
            get() = notifyMissingPaycheck || notifyMissingDiesel || maintenanceDueTitles.isNotEmpty()

        /** Single body line for maintenance when bundling into one notification. */
        fun maintenanceSummaryBody(separator: String = ", "): String =
            maintenanceDueTitles.joinToString(separator)
    }

    fun plan(
        hasPaycheckForLastWeek: Boolean,
        dieselEntriesLastWeek: Int,
        maintenanceDueTitles: List<String> = emptyList(),
        alreadyNotifiedMissingWeek: Boolean = false,
    ): Plan =
        Plan(
            notifyMissingPaycheck = !alreadyNotifiedMissingWeek && !hasPaycheckForLastWeek,
            notifyMissingDiesel = !alreadyNotifiedMissingWeek && dieselEntriesLastWeek <= 0,
            maintenanceDueTitles = maintenanceDueTitles,
        )
}
