package com.truckerload.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PaycheckJournalFilterTest {

    @Test
    fun all_sortsNewestWeekFirst() {
        val paychecks = listOf(
            sample(id = 1, week = 34, year = 2026, addedAt = 4_000L),
            sample(id = 2, week = 35, year = 2026, addedAt = 2_000L),
            sample(id = 3, week = 1, year = 2027, addedAt = 1_000L),
        )
        assertEquals(listOf(3, 2, 1), PaycheckJournalFilter.all(paychecks).map { it.id })
    }

    @Test
    fun forWeek_keepsMatchingWeekOnly() {
        val paychecks = listOf(
            sample(id = 1, week = 35, year = 2026, addedAt = 2_000L),
            sample(id = 2, week = 34, year = 2026, addedAt = 3_000L),
            sample(id = 3, week = 35, year = 2026, addedAt = 4_000L),
        )
        assertEquals(listOf(3, 1), PaycheckJournalFilter.forWeek(paychecks, 35, 2026).map { it.id })
    }

    @Test
    fun matching_findsWeekLabelAmountOrFile() {
        val paychecks = listOf(
            sample(id = 1, week = 35, net = 2500.0, file = "settlement.pdf"),
            sample(id = 2, week = 34, net = 1800.0, driver = "Alex"),
        )
        assertEquals(listOf(1), PaycheckJournalFilter.matching(paychecks, "35").map { it.id })
        assertEquals(listOf(2), PaycheckJournalFilter.matching(paychecks, "alex").map { it.id })
        assertEquals(listOf(1), PaycheckJournalFilter.matching(paychecks, "settlement").map { it.id })
        assertEquals(listOf(1, 2), PaycheckJournalFilter.matching(paychecks, "").map { it.id })
    }

    private fun sample(
        id: Int,
        week: Int = 35,
        year: Int = 2026,
        addedAt: Long = 1_000L,
        net: Double = 1000.0,
        file: String? = null,
        driver: String? = null,
    ) = Paycheck(
        id = id,
        weekNumber = week,
        year = year,
        weekLabel = "W$week $year",
        weekStartDate = "2026-01-01",
        weekEndDate = "2026-01-07",
        driverName = driver,
        grossAmount = null,
        netAmount = net,
        rawExtractedText = "",
        sourceFileName = file,
        addedAt = addedAt,
    )
}
