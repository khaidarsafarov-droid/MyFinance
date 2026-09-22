package com.truckerload.sync.telegram

import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.DieselJournalFilter
import com.truckerload.domain.week.WeekStartRebinder
import com.truckerload.domain.week.WeekStartRuntime
import com.truckerload.utils.dateStringToStartOfDayMillis
import com.truckerload.utils.getWeekNumberAndYearFromTimestamp
import com.truckerload.utils.getWeekRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DieselRecordedAtTest {

    @Test
    fun receiptDate_isStoredInsteadOfIngestClock() {
        val today = dateStringToStartOfDayMillis("2026-09-22")!!
        val recorded = dieselRecordedAtMillis(
            dateHint = "2026-09-14",
            messageDateSeconds = today / 1000,
            fallbackMillis = today,
        )
        val receiptDay = dateStringToStartOfDayMillis("2026-09-14")!!
        assertEquals(receiptDay, recorded)

        val (receiptWeek, receiptYear) = getWeekNumberAndYearFromTimestamp(
            recorded,
            WeekStartRuntime.diesel,
        )
        val (todayWeek, todayYear) = getWeekNumberAndYearFromTimestamp(
            today,
            WeekStartRuntime.diesel,
        )
        assertNotEquals(todayWeek to todayYear, receiptWeek to receiptYear)

        val fill = sample(addedAt = recorded, week = receiptWeek, year = receiptYear)
        assertEquals(
            listOf(1),
            DieselJournalFilter.forWeek(listOf(fill), receiptWeek, receiptYear).map { it.id },
        )
        assertTrue(
            DieselJournalFilter.forWeek(listOf(fill), todayWeek, todayYear).isEmpty(),
        )
        val (start, end, _) = getWeekRange(receiptWeek, receiptYear, WeekStartRuntime.diesel)
        assertTrue(WeekStartRebinder.dieselIsoInRange(recorded, start, end))
    }

    @Test
    fun messageTime_usedWhenReceiptHasNoDate() {
        val messageDay = dateStringToStartOfDayMillis("2026-09-14")!!
        val today = dateStringToStartOfDayMillis("2026-09-22")!!
        assertEquals(
            messageDay,
            dieselRecordedAtMillis(
                dateHint = null,
                messageDateSeconds = messageDay / 1000,
                fallbackMillis = today,
            ),
        )
    }

    @Test
    fun fallback_usedWhenNoDateAtAll() {
        assertEquals(99L, dieselRecordedAtMillis(null, null, 99L))
        assertEquals(99L, dieselRecordedAtMillis("not-a-date", 0L, 99L))
    }

    private fun sample(addedAt: Long, week: Int, year: Int) = Diesel(
        id = 1,
        weekNumber = week,
        year = year,
        weekLabel = "W$week",
        weekStartDate = "2026-09-13",
        weekEndDate = "2026-09-19",
        totalAmount = 180.0,
        gallons = 40.0,
        pricePerGallon = 4.5,
        location = "Pilot",
        rawExtractedText = "",
        sourceFileName = null,
        addedAt = addedAt,
    )
}
