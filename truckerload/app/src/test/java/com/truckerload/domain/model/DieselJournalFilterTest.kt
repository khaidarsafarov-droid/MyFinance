package com.truckerload.domain.model

import com.truckerload.utils.getMillisForWeek
import org.junit.Assert.assertEquals
import org.junit.Test

class DieselJournalFilterTest {

    @Test
    fun forWeek_keepsMatchingWeekOnly() {
        val week35 = getMillisForWeek(35, 2026)
        val week34 = getMillisForWeek(34, 2026)
        val fills = listOf(
            sample(id = 1, week = 35, year = 2026, addedAt = week35),
            sample(id = 2, week = 34, year = 2026, addedAt = week34),
            sample(id = 3, week = 35, year = 2026, addedAt = week35 + 3_600_000L),
        )
        assertEquals(listOf(3, 1), DieselJournalFilter.forWeek(fills, 35, 2026).map { it.id })
    }

    @Test
    fun forDate_keepsMatchingCalendarDay() {
        val dayA = 1_777_046_400_000L // 2026-05-25 00:00 UTC; local day still 2026-05-25 in US/EU
        val dayB = dayA + 86_400_000L
        val fills = listOf(
            sample(id = 1, week = 22, year = 2026, addedAt = dayA + 3_600_000L),
            sample(id = 2, week = 22, year = 2026, addedAt = dayB + 3_600_000L),
        )
        val iso = com.truckerload.utils.formatIsoDate(dayA + 3_600_000L)
        assertEquals(listOf(1), DieselJournalFilter.forDate(fills, iso).map { it.id })
    }

    private fun sample(
        id: Int,
        week: Int,
        year: Int,
        addedAt: Long,
    ) = Diesel(
        id = id,
        weekNumber = week,
        year = year,
        weekLabel = "W$week",
        weekStartDate = "2026-01-01",
        weekEndDate = "2026-01-07",
        totalAmount = 100.0,
        gallons = 20.0,
        pricePerGallon = 5.0,
        discountPricePerGallon = 4.0,
        location = "Pilot",
        rawExtractedText = "",
        sourceFileName = null,
        addedAt = addedAt,
    )
}
