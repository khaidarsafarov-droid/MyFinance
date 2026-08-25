package com.truckerload.domain.model

import com.truckerload.utils.formatIsoDate

/** Week / calendar-day slices of diesel fills for the journal screen. */
object DieselJournalFilter {

    fun forWeek(entries: List<Diesel>, weekNumber: Int, year: Int): List<Diesel> =
        entries.filter { it.weekNumber == weekNumber && it.year == year }
            .sortedByDescending { it.addedAt }

    fun forDate(entries: List<Diesel>, dateIso: String): List<Diesel> =
        entries.filter { formatIsoDate(it.addedAt) == dateIso }
            .sortedByDescending { it.addedAt }
}
