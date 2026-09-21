package com.truckerload.widget

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WidgetDayProjectionTest {

    private val weekStart = LocalDate.of(2026, 8, 16) // Sunday
    private val wednesday = LocalDate.of(2026, 8, 19)

    @Test
    fun todayOffset_wednesdayIs3() {
        assertEquals(3, WidgetDayProjection.todayOffset(wednesday, weekStart))
    }

    @Test
    fun clampSelection_defaultsToTodayAndBlocksFuture() {
        assertEquals(3, WidgetDayProjection.clampSelection(null, todayOffset = 3))
        assertEquals(1, WidgetDayProjection.clampSelection(1, todayOffset = 3))
        assertEquals(3, WidgetDayProjection.clampSelection(6, todayOffset = 3))
        assertEquals(0, WidgetDayProjection.clampSelection(-4, todayOffset = 3))
    }

    @Test
    fun totalsByDay_assignsEachLoadOnce() {
        val days = WidgetDayProjection.totalsByDay(
            listOf(
                load("sun", "2026-08-16", rate = 1000.0, miles = 500.0),
                load("wed-a", "2026-08-19", rate = 2000.0, miles = 800.0),
                load("wed-b", "2026-08-19", rate = 2000.0, miles = 200.0),
                load("outside", "2026-08-10", rate = 999.0, miles = 99.0),
            ),
            weekStart,
        )
        assertEquals(7, days.size)
        assertEquals(1, days[0].loadsCount)
        assertEquals(1000.0, days[0].gross, 0.0)
        assertEquals(2, days[3].loadsCount)
        assertEquals(4000.0, days[3].gross, 0.0)
        assertEquals(1000.0, days[3].miles, 0.0)
        assertEquals(0, days[6].loadsCount)
        assertEquals(2.0, days[0].rpm, 0.0)
    }

    @Test
    fun through_sumsSundayThroughSelectedDay() {
        val days = listOf(
            WidgetDayProjection.DayTotals(1, 1000.0, 400.0),
            WidgetDayProjection.DayTotals(1, 500.0, 100.0),
            WidgetDayProjection.DayTotals(0, 0.0, 0.0),
            WidgetDayProjection.DayTotals(2, 3500.0, 1750.0),
            WidgetDayProjection.DayTotals(5, 8000.0, 100.0),
            WidgetDayProjection.DayTotals(),
            WidgetDayProjection.DayTotals(),
        )
        val wed = WidgetDayProjection.through(days, endOffset = 3)
        assertEquals(4, wed.loadsCount)
        assertEquals(5000.0, wed.gross, 0.0)
        assertEquals(2250.0, wed.miles, 0.0)
        assertEquals(5000.0 / 2250.0, wed.rpm, 0.0001)

        val sun = WidgetDayProjection.through(days, endOffset = 0)
        assertEquals(1, sun.loadsCount)
        assertEquals(1000.0, sun.gross, 0.0)
    }

    @Test
    fun totalsByDay_placesFinishWeekLoadOnFinishDay() {
        // Home This week includes PU-Saturday / DEL-Sunday loads; chips must too.
        val days = WidgetDayProjection.totalsByDay(
            listOf(
                spanningLoad(
                    id = "finish-sun",
                    puDate = "2026-08-15",
                    puTime = "08/15 18:00 EDT",
                    delTime = "08/16 08:00 EDT",
                    rate = 2141.0,
                    miles = 727.0,
                ),
                spanningLoad(
                    id = "finish-mon",
                    puDate = "2026-08-15",
                    puTime = "08/15 08:00 EDT",
                    delTime = "08/17 18:00 EDT",
                    rate = 2336.96,
                    miles = 1011.0,
                ),
            ),
            weekStart,
        )
        assertEquals(1, days[0].loadsCount)
        assertEquals(2141.0, days[0].gross, 0.0)
        assertEquals(1, days[1].loadsCount)
        assertEquals(2336.96, days[1].gross, 0.0)
        assertEquals(0, days[2].loadsCount)
        assertEquals(
            0b0000011,
            WidgetDayProjection.maskFromDayTotals(days),
        )
    }

    @Test
    fun project_todayMatchesHomeThisWeekWhenSecondLoadFinishesLater() {
        val weekStartSep20 = LocalDate.of(2026, 9, 20)
        val days = WidgetDayProjection.totalsByDay(
            listOf(
                load("T-115MVR657", "2026-09-20", rate = 2141.0, miles = 727.0),
                spanningLoad(
                    id = "112LL59VN",
                    puDate = "2026-09-19",
                    puTime = "09/19 08:00 EDT",
                    delTime = "09/21 18:00 EDT",
                    rate = 2336.96,
                    miles = 1011.0,
                ),
            ),
            weekStartSep20,
        )
        val week = WidgetStats(
            loadsCount = 2,
            totalLoadRate = 4477.96,
            totalMiles = 1738.0,
            avgCpm = 4477.96 / 1738.0,
            weeklyProfitGoal = 13_000.0,
            goalProgressPercent = ((4477.96 / 13_000.0) * 100).toFloat(),
            dayLoads = days.map { it.loadsCount },
            dayGross = days.map { it.gross },
            dayMiles = days.map { it.miles },
        )
        val monday = WidgetDayProjection.todayOffset(
            LocalDate.of(2026, 9, 21),
            weekStartSep20,
        )
        val shown = WidgetDayProjection.project(week, selectedOffset = null, todayOffset = monday)
        assertEquals(1, monday)
        assertEquals(2141.0, days[0].gross, 0.0)
        assertEquals(2336.96, days[1].gross, 0.0)
        assertEquals(2, shown.loadsCount)
        assertEquals(4477.96, shown.totalLoadRate, 0.0)
        assertEquals(1738.0, shown.totalMiles, 0.0)
        assertEquals(4477.96 / 1738.0, shown.currentWeeklyRpm, 0.0001)
    }

    @Test
    fun project_todayKeepsFullWeekIncludingLaterDays() {
        val week = WidgetStats(
            loadsCount = 43,
            totalLoadRate = 13_000.0,
            totalMiles = 3500.0,
            avgCpm = 13_000.0 / 3500.0,
            weeklyProfitGoal = 13_000.0,
            goalProgressPercent = 100f,
            goalRemainingAmount = 0.0,
            dayLoads = listOf(1, 0, 0, 2, 40, 0, 0),
            dayGross = listOf(1000.0, 0.0, 0.0, 4000.0, 8000.0, 0.0, 0.0),
            dayMiles = listOf(500.0, 0.0, 0.0, 2000.0, 1000.0, 0.0, 0.0),
        )
        val shown = WidgetDayProjection.project(week, selectedOffset = 3, todayOffset = 3)
        assertEquals(43, shown.loadsCount)
        assertEquals(13_000.0, shown.totalLoadRate, 0.0)
        assertEquals(3500.0, shown.totalMiles, 0.0)
        assertEquals(13_000.0, shown.weeklyProfitGoal, 0.0)
        assertEquals(100f, shown.goalProgressPercent, 0.05f)
        assertEquals(0.0, shown.goalRemainingAmount, 0.0)
    }

    @Test
    fun project_pastDayStillSumsSundayThroughSelected() {
        val week = WidgetStats(
            loadsCount = 43,
            totalLoadRate = 13_000.0,
            totalMiles = 3500.0,
            weeklyProfitGoal = 13_000.0,
            goalProgressPercent = 100f,
            dayLoads = listOf(1, 0, 0, 2, 40, 0, 0),
            dayGross = listOf(1000.0, 0.0, 0.0, 4000.0, 8000.0, 0.0, 0.0),
            dayMiles = listOf(500.0, 0.0, 0.0, 2000.0, 1000.0, 0.0, 0.0),
        )
        val shown = WidgetDayProjection.project(week, selectedOffset = 3, todayOffset = 5)
        assertEquals(3, shown.loadsCount)
        assertEquals(5000.0, shown.totalLoadRate, 0.0)
        assertEquals(2500.0, shown.totalMiles, 0.0)
        assertEquals(2.0, shown.currentWeeklyRpm, 0.0)
        assertEquals(38.5f, shown.goalProgressPercent, 0.05f)
        assertEquals(8000.0, shown.goalRemainingAmount, 0.0)
    }

    @Test
    fun project_legacyCacheWithoutSlicesKeepsWeekWhenTodaySelected() {
        val week = WidgetStats(
            loadsCount = 10,
            totalLoadRate = 441.0,
            weeklyProfitGoal = 13_000.0,
            goalProgressPercent = 3.4f,
        )
        val today = WidgetDayProjection.project(week, selectedOffset = null, todayOffset = 2)
        assertEquals(10, today.loadsCount)
        assertEquals(441.0, today.totalLoadRate, 0.0)

        val monday = WidgetDayProjection.project(week, selectedOffset = 1, todayOffset = 2)
        assertEquals(0, monday.loadsCount)
        assertEquals(0.0, monday.totalLoadRate, 0.0)
    }

    @Test
    fun packAndUnpack_roundTripSevenDays() {
        val loads = listOf(1, 0, 2, 3, 0, 0, 4)
        val gross = listOf(10.0, 0.0, 20.5, 30.0, 0.0, 0.0, 40.0)
        assertEquals(loads, WidgetDataStore.unpackInts(WidgetDataStore.packInts(loads)))
        assertEquals(gross, WidgetDataStore.unpackDoubles(WidgetDataStore.packDoubles(gross)))
        assertEquals(List(7) { 0 }, WidgetDataStore.unpackInts(null))
        assertTrue(WidgetDataStore.unpackDoubles("1,2").all { it == 0.0 })
    }

    private fun load(
        id: String,
        date: String,
        rate: Double,
        miles: Double,
    ) = Load(
        id = id,
        tripId = id,
        date = date,
        totalRate = rate,
        totalMiles = miles,
        pointA = "A",
        pointB = "B",
        puCount = 1,
        delCount = 1,
        weekNumber = 34,
        year = 2026,
        rawMessage = "",
        parsedAt = 1L,
        updatedAt = 1L,
    )

    private fun spanningLoad(
        id: String,
        puDate: String,
        puTime: String,
        delTime: String,
        rate: Double,
        miles: Double,
    ) = Load(
        id = id,
        tripId = id,
        date = puDate,
        totalRate = rate,
        totalMiles = miles,
        pointA = "A",
        pointB = "B",
        puCount = 1,
        delCount = 1,
        weekNumber = 34,
        year = 2026,
        rawMessage = "",
        parsedAt = 1L,
        updatedAt = 1L,
        stops = listOf(
            Stop(
                id = 1,
                loadId = id,
                stopNumber = 1,
                type = StopType.PU,
                puNumber = "PU1",
                note = null,
                scheduledTime = puTime,
                timezone = "EDT",
                facilityCode = null,
                fullAddress = "A",
                city = "A",
                state = "MS",
                zip = "38611",
            ),
            Stop(
                id = 2,
                loadId = id,
                stopNumber = 2,
                type = StopType.DEL,
                puNumber = null,
                note = null,
                scheduledTime = delTime,
                timezone = "EDT",
                facilityCode = null,
                fullAddress = "B",
                city = "B",
                state = "LA",
                zip = "71101",
            ),
        ),
    )
}
