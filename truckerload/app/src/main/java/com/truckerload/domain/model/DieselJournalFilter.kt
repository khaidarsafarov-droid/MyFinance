package com.truckerload.domain.model

import com.truckerload.domain.week.WeekStartRuntime
import com.truckerload.utils.formatIsoDate
import com.truckerload.utils.getWeekNumberAndYearFromTimestamp
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Week / calendar-day slices of diesel fills for the journal screen. */
object DieselJournalFilter {

    fun forWeek(entries: List<Diesel>, weekNumber: Int, year: Int): List<Diesel> =
        entries.filter { diesel ->
            val (w, y) = reportingWeek(diesel, entries)
            w == weekNumber && y == year
        }.sortedByDescending { it.addedAt }

    /**
     * One spreadsheet is one diesel week: every row from that file follows the week
     * of its earliest fill, using the diesel week-start from settings.
     * A lone fill, or a file whose dates span more than seven days, stays on its own date.
     */
    fun reportingWeek(diesel: Diesel, entries: List<Diesel>): Pair<Int, Int> {
        val anchor = fileAnchor(diesel, entries) ?: diesel
        return getWeekNumberAndYearFromTimestamp(anchor.addedAt, WeekStartRuntime.diesel)
    }

    private fun fileAnchor(diesel: Diesel, entries: List<Diesel>): Diesel? {
        val file = diesel.sourceFileName?.takeIf { it.isNotBlank() } ?: return null
        val siblings = entries.filter { it.sourceFileName == file }
        if (siblings.size < 2 || !spansAtMostOneWeek(siblings)) return null
        return siblings.minBy { it.addedAt }
    }

    private fun spansAtMostOneWeek(entries: List<Diesel>): Boolean {
        val dates = entries.map { formatIsoDate(it.addedAt) }.filter { it.length >= 10 }.sorted()
        if (dates.size < 2) return true
        val start = LocalDate.parse(dates.first())
        val end = LocalDate.parse(dates.last())
        return ChronoUnit.DAYS.between(start, end) <= 6
    }

    fun forDate(entries: List<Diesel>, dateIso: String): List<Diesel> =
        entries.filter { formatIsoDate(it.addedAt) == dateIso }
            .sortedByDescending { it.addedAt }
}
