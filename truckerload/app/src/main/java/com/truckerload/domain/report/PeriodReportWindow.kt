package com.truckerload.domain.report

import com.truckerload.domain.filter.LoadFilter
import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.utils.getMonthRange
import com.truckerload.utils.getPreviousWeekNumberAndYear
import com.truckerload.utils.getWeekNumberAndYearFromDate
import com.truckerload.utils.getWeekRange
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class PeriodReportKind {
    WEEK,
    MONTH,
    DAY,
    DISPUTES,
    ALL,
}

/**
 * Date window for one share. [start] and [end] are inclusive ISO dates.
 * [includeWeeklyGoal] is true only for a single reporting week.
 * [includeFuel] is false for a single day and for disputes: diesel is stored per week.
 */
data class PeriodReportWindow(
    val kind: PeriodReportKind,
    val start: LocalDate?,
    val end: LocalDate?,
    val weekNumber: Int? = null,
    val weekYear: Int? = null,
    val calendarYear: Int? = null,
    val includeWeeklyGoal: Boolean = false,
    val includeFuel: Boolean = false,
    val includeExpenses: Boolean = false,
    val monthTitle: String = "",
    val dayTitle: String = "",
)

data class PeriodReportSelection(
    val filter: LoadFilter,
    val selectedDate: String? = null,
    val selectedDateLabel: String = "",
    val selectedWeekStart: String? = null,
    val selectedMonthYear: Int? = null,
    val selectedMonth: Int? = null,
    val selectedMonthLabel: String = "",
    val selectedYear: Int? = null,
)

object PeriodReportWindowResolver {

    fun resolve(selection: PeriodReportSelection, today: LocalDate = LocalDate.now()): PeriodReportWindow {
        return when (selection.filter) {
            LoadFilter.THIS_WEEK -> week(getCurrentWeekNumberAndYear())
            LoadFilter.LAST_WEEK -> week(getPreviousWeekNumberAndYear())
            LoadFilter.CALENDAR_WEEK -> {
                val start = selection.selectedWeekStart?.take(10)
                if (start.isNullOrBlank()) {
                    week(getCurrentWeekNumberAndYear())
                } else {
                    week(getWeekNumberAndYearFromDate(start))
                }
            }
            LoadFilter.THIS_MONTH -> {
                val year = selection.selectedMonthYear ?: today.year
                val month = selection.selectedMonth ?: today.monthValue
                val (start, end) = getMonthRange(month, year)
                ranged(
                    kind = PeriodReportKind.MONTH,
                    start = start,
                    end = end,
                    includeFuel = true,
                    includeExpenses = true,
                    monthTitle = selection.selectedMonthLabel,
                )
            }
            LoadFilter.YESTERDAY,
            LoadFilter.CALENDAR_DATE,
            LoadFilter.DISPUTE,
            LoadFilter.ALL,
            -> week(getCurrentWeekNumberAndYear())
        }
    }

    fun label(
        window: PeriodReportWindow,
        locale: Locale,
        formatDate: (LocalDate) -> String,
        templates: PeriodReportPeriodTemplates,
    ): String = when (window.kind) {
        PeriodReportKind.WEEK -> String.format(
            locale,
            templates.weekRange,
            window.weekNumber,
            formatDate(requireNotNull(window.start)),
            formatDate(requireNotNull(window.end)),
        )
        PeriodReportKind.MONTH -> {
            val start = requireNotNull(window.start)
            val name = window.monthTitle.ifBlank {
                start.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
            }
            String.format(
                locale,
                templates.monthRange,
                name,
                formatDate(start),
                formatDate(requireNotNull(window.end)),
            )
        }
        PeriodReportKind.DAY -> window.dayTitle.ifBlank {
            formatDate(requireNotNull(window.start))
        }
        PeriodReportKind.DISPUTES -> templates.disputes
        PeriodReportKind.ALL -> {
            val year = window.calendarYear
            val start = window.start
            val end = window.end
            if (year != null && start != null && end != null) {
                String.format(locale, templates.yearRange, year, formatDate(start), formatDate(end))
            } else {
                templates.allLoads
            }
        }
    }

    private fun week(numberAndYear: Pair<Int, Int>): PeriodReportWindow {
        val (week, year) = numberAndYear
        val (start, end, _) = getWeekRange(week, year)
        return ranged(
            kind = PeriodReportKind.WEEK,
            start = start,
            end = end,
            includeFuel = true,
            includeExpenses = true,
            includeWeeklyGoal = true,
            weekNumber = week,
            weekYear = year,
        )
    }

    private fun ranged(
        kind: PeriodReportKind,
        start: String,
        end: String,
        includeFuel: Boolean,
        includeExpenses: Boolean,
        includeWeeklyGoal: Boolean = false,
        weekNumber: Int? = null,
        weekYear: Int? = null,
        calendarYear: Int? = null,
        monthTitle: String = "",
        dayTitle: String = "",
    ): PeriodReportWindow = PeriodReportWindow(
        kind = kind,
        start = LocalDate.parse(start.take(10)),
        end = LocalDate.parse(end.take(10)),
        weekNumber = weekNumber,
        weekYear = weekYear,
        calendarYear = calendarYear,
        includeWeeklyGoal = includeWeeklyGoal,
        includeFuel = includeFuel,
        includeExpenses = includeExpenses,
        monthTitle = monthTitle,
        dayTitle = dayTitle,
    )
}
