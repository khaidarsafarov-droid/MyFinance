package com.truckerload.domain.model

/** Week / full-history slices of paychecks for the journal screen. */
object PaycheckJournalFilter {

    fun all(entries: List<Paycheck>): List<Paycheck> =
        entries.sortedWith(
            compareByDescending<Paycheck> { it.year }
                .thenByDescending { it.weekNumber }
                .thenByDescending { it.addedAt },
        )

    fun forWeek(entries: List<Paycheck>, weekNumber: Int, year: Int): List<Paycheck> =
        entries.filter { it.weekNumber == weekNumber && it.year == year }
            .sortedByDescending { it.addedAt }

    fun matching(entries: List<Paycheck>, query: String): List<Paycheck> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return entries
        return entries.filter { paycheck ->
            paycheck.weekLabel.lowercase().contains(q) ||
                paycheck.weekNumber.toString() == q ||
                paycheck.year.toString().contains(q) ||
                paycheck.driverName.orEmpty().lowercase().contains(q) ||
                paycheck.sourceFileName.orEmpty().lowercase().contains(q) ||
                paycheck.netAmount.toString().contains(q) ||
                paycheck.grossAmount?.toString()?.contains(q) == true
        }
    }
}
