package com.truckerload.domain.model.analytics

/**
 * Reporting window for «Мои цифры».
 *
 * Either a rolling [preset], or a calendar drill-down: year → month of that year →
 * trucking week (Sun–Sat) inside the month. Tapping the selected month or week
 * again clears that level so totals go back to the whole year / month.
 */
data class AnalyticsFilter(
    val preset: AnalyticsPeriod? = AnalyticsPeriod.LAST_12_WEEKS,
    val year: Int? = null,
    val month: Int? = null,
    val weekNumber: Int? = null,
    val weekYear: Int? = null,
) {
    val isCalendar: Boolean get() = year != null

    fun selectPreset(period: AnalyticsPeriod): AnalyticsFilter =
        AnalyticsFilter(preset = period)

    fun selectYear(selectedYear: Int): AnalyticsFilter {
        if (year == selectedYear && month == null) return this
        return AnalyticsFilter(preset = null, year = selectedYear)
    }

    fun selectMonth(selectedMonth: Int): AnalyticsFilter {
        val y = year ?: return this
        if (month == selectedMonth && weekNumber == null) {
            return copy(month = null, weekNumber = null, weekYear = null)
        }
        return copy(
            preset = null,
            year = y,
            month = selectedMonth,
            weekNumber = null,
            weekYear = null,
        )
    }

    fun selectWeek(selectedWeek: Int, selectedWeekYear: Int): AnalyticsFilter {
        if (year == null || month == null) return this
        if (weekNumber == selectedWeek && weekYear == selectedWeekYear) {
            return copy(weekNumber = null, weekYear = null)
        }
        return copy(weekNumber = selectedWeek, weekYear = selectedWeekYear)
    }

    fun exportKey(): String = when {
        weekNumber != null && weekYear != null ->
            "Y${year}_M${month}_W${weekNumber}_$weekYear"
        month != null && year != null -> "Y${year}_M$month"
        year != null -> "Y$year"
        else -> (preset ?: AnalyticsPeriod.LAST_12_WEEKS).name
    }

    companion object {
        val DEFAULT = AnalyticsFilter()
        const val YEAR_COUNT = 5

        fun preset(period: AnalyticsPeriod): AnalyticsFilter =
            AnalyticsFilter(preset = period)
    }
}
