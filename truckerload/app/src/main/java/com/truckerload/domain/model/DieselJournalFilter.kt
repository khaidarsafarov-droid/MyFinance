package com.truckerload.domain.model

import com.truckerload.domain.week.WeekStartRuntime
import com.truckerload.utils.formatIsoDate
import com.truckerload.utils.getWeekNumberAndYearFromTimestamp

/** Week / calendar-day slices of diesel fills for the journal screen. */
object DieselJournalFilter {

    fun forWeek(entries: List<Diesel>, weekNumber: Int, year: Int): List<Diesel> =
        entries.filter { diesel ->
            val (w, y) = getWeekNumberAndYearFromTimestamp(diesel.addedAt, WeekStartRuntime.diesel)
            w == weekNumber && y == year
        }.sortedByDescending { it.addedAt }

    fun forDate(entries: List<Diesel>, dateIso: String): List<Diesel> =
        entries.filter { formatIsoDate(it.addedAt) == dateIso }
            .sortedByDescending { it.addedAt }
}
