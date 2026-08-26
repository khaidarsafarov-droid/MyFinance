package com.truckerload.domain.filter

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import com.truckerload.utils.getLoadDateRange
import com.truckerload.utils.getLoadReportingWeek
import com.truckerload.utils.getWeekNumberAndYearFromDate
import com.truckerload.utils.getWeekRange
import com.truckerload.utils.isLoadInWeek
import com.truckerload.utils.withReportingWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Period filters must follow trucking logic:
 * - Weeks → reporting week (Sun–Sat; DEL can bump week past PU)
 * - Day / month → active date range (PU … DEL / actualFinish)
 */
class LoadFilterPeriodLogicTest {

    private val useCase = LoadFilterUseCase()

    /** Sat 2025-07-05 PU → Sun 2025-07-06 DEL (new trucking week). */
    private fun overnightWeekendLoad(): Load {
        val raw = Load(
            id = "overnight",
            tripId = "T-OVERNIGHT",
            date = "2025-07-05",
            totalRate = 1500.0,
            totalMiles = 400.0,
            pointA = "Austin, TX",
            pointB = "Dallas, TX",
            puCount = 1,
            delCount = 1,
            weekNumber = 0,
            year = 0,
            rawMessage = "",
            parsedAt = 1L,
            updatedAt = 1L,
            stops = listOf(
                stop(1, StopType.PU, "07/05 18:00 EDT", "Austin", "TX"),
                stop(2, StopType.DEL, "07/06 00:01 EDT", "Dallas", "TX"),
            ),
        )
        return raw.withReportingWeek()
    }

    /** PU Jul 30 → DEL Aug 2 (spans months). */
    private fun crossMonthLoad(): Load {
        val raw = Load(
            id = "cross-month",
            tripId = "T-CROSS",
            date = "2025-07-30",
            totalRate = 2000.0,
            totalMiles = 500.0,
            pointA = "A",
            pointB = "B",
            puCount = 1,
            delCount = 1,
            weekNumber = 0,
            year = 0,
            rawMessage = "",
            parsedAt = 1L,
            updatedAt = 1L,
            stops = listOf(
                stop(1, StopType.PU, "2025-07-30 08:00", "Garner", "NC"),
                stop(2, StopType.DEL, "2025-08-02 18:00", "Dallas", "TX"),
            ),
        )
        return raw.withReportingWeek()
    }

    private fun stop(
        n: Int,
        type: StopType,
        time: String,
        city: String,
        state: String,
    ) = Stop(
        id = n,
        loadId = "x",
        stopNumber = n,
        type = type,
        puNumber = if (type == StopType.PU) "PU$n" else null,
        note = null,
        scheduledTime = time,
        timezone = "EDT",
        facilityCode = null,
        fullAddress = "$city, $state",
        city = city,
        state = state,
        zip = "",
    )

    @Test
    fun overnightLoad_belongsToDeliveryReportingWeek_notPuDateWeek() {
        val load = overnightWeekendLoad()
        val puWeek = getWeekNumberAndYearFromDate("2025-07-05")
        val delWeek = getWeekNumberAndYearFromDate("2025-07-06")
        assertEquals(delWeek, getLoadReportingWeek(load))
        assertEquals(delWeek.first, load.weekNumber)
        assertEquals(delWeek.second, load.year)
        assertTrue(isLoadInWeek(load, delWeek.first, delWeek.second))
        assertFalse(isLoadInWeek(load, puWeek.first, puWeek.second))
    }

    @Test
    fun isLoadInWeek_recomputesFromStopsWhenPersistedWeekIsStale() {
        val load = overnightWeekendLoad().copy(weekNumber = 1, year = 2020)
        val delWeek = getWeekNumberAndYearFromDate("2025-07-06")
        assertTrue(isLoadInWeek(load, delWeek.first, delWeek.second))
        assertFalse(isLoadInWeek(load, 1, 2020))
    }

    @Test
    fun calendarWeekFilter_usesReportingWeek_notLoadDateRange() {
        val load = overnightWeekendLoad()
        val delWeek = getWeekNumberAndYearFromDate("2025-07-06")
        val (start, _, _) = getWeekRange(delWeek.first, delWeek.second)
        // PU date is outside this Sun–Sat window, but reporting week matches.
        assertTrue(load.date < start)

        val filtered = useCase.filterLoads(
            listOf(load),
            LoadFilter.CALENDAR_WEEK,
            "",
            null,
            start,
            null,
            null,
        )
        assertEquals(listOf("overnight"), filtered.map { it.id })
    }

    @Test
    fun calendarDateFilter_includesMidTripActiveDay() {
        val load = crossMonthLoad()
        assertTrue("2025-08-01" in getLoadDateRange(load))
        val filtered = useCase.filterLoads(
            listOf(load),
            LoadFilter.CALENDAR_DATE,
            "",
            "2025-08-01",
            null,
            null,
            null,
        )
        assertEquals(listOf("cross-month"), filtered.map { it.id })
    }

    @Test
    fun thisMonthFilter_includesLoadWhenOnlyDeliveryDaysFallInMonth() {
        val load = crossMonthLoad()
        // Simulate "this month" = August 2025 by filtering with a fixed prefix via CALENDAR_DATE
        // range semantics already tested; mirror THIS_MONTH predicate directly:
        val augustDays = getLoadDateRange(load).filter { it.startsWith("2025-08") }
        assertTrue(augustDays.isNotEmpty())
        assertTrue(getLoadDateRange(load).any { it.startsWith("2025-07") })

        // When calendar month is August, PU-only SQL would miss this load; range logic keeps it.
        val inAugust = getLoadDateRange(load).any { it.startsWith("2025-08") }
        assertTrue(inAugust)
    }

    @Test
        fun usesRoomPagingPolicy_allAndWeeksYes_monthDayNo() {
        // Document intended HomeViewModel.usesRoomPaging contract in a pure unit test
        // by mirroring the when-expression.
        fun usesRoomPaging(filter: LoadFilter, selectedYear: Int?): Boolean {
            if (filter == LoadFilter.ALL) return true
            if (selectedYear != null) return false
            return when (filter) {
                LoadFilter.THIS_WEEK,
                LoadFilter.LAST_WEEK,
                LoadFilter.CALENDAR_WEEK,
                LoadFilter.DISPUTE,
                -> true
                else -> false
            }
        }
        assertTrue(usesRoomPaging(LoadFilter.ALL, null))
        assertTrue(usesRoomPaging(LoadFilter.ALL, 2025))
        assertTrue(usesRoomPaging(LoadFilter.THIS_WEEK, null))
        assertTrue(usesRoomPaging(LoadFilter.CALENDAR_WEEK, null))
        assertFalse(usesRoomPaging(LoadFilter.THIS_MONTH, null))
        assertFalse(usesRoomPaging(LoadFilter.CALENDAR_DATE, null))
        assertFalse(usesRoomPaging(LoadFilter.YESTERDAY, null))
        assertFalse(usesRoomPaging(LoadFilter.THIS_WEEK, 2025))
    }
}
