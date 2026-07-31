package com.truckerload.domain.friends

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import com.truckerload.utils.getWeekNumberAndYearFromDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ActiveLoadSelectorTest {

    private val today = LocalDate.of(2026, 7, 20)
    private val noonTodayMs = today.atTime(12, 0)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
    private val weekPair = getWeekNumberAndYearFromDate(today.toString())

    @Test
    fun activeWhenTodayInsideRangeAfterFirstPu() {
        val load = sample(date = "2026-07-19", plannedEnd = "2026-07-22", actualFinish = null)
        assertEquals(SharedLoadStatus.ACTIVE, ActiveLoadSelector.statusFor(load, today, noonTodayMs))
        assertEquals(load.id, ActiveLoadSelector.selectActive(listOf(load), today, noonTodayMs)?.id)
    }

    @Test
    fun beforeFirstPuClockIsFutureEvenOnStartDay() {
        val finishDay = LocalDate.of(2026, 7, 20)
        val at9pm = finishDay.atTime(21, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val at10pm = finishDay.atTime(22, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val load = sample(
            date = "2026-07-20",
            plannedEnd = "2026-07-21",
            actualFinish = null,
            puTime = "2026-07-20 22:00",
            delTime = "2026-07-21 18:00",
        )
        assertEquals(SharedLoadStatus.FUTURE, ActiveLoadSelector.statusFor(load, finishDay, at9pm))
        assertEquals(SharedLoadStatus.ACTIVE, ActiveLoadSelector.statusFor(load, finishDay, at10pm))
        assertNull(ActiveLoadSelector.selectForMapRoute(listOf(load), finishDay, at9pm))
        assertEquals(load.id, ActiveLoadSelector.selectForMapRoute(listOf(load), finishDay, at10pm)?.id)
    }

    @Test
    fun waitingForEveningPuAfterShortFinishedHidesRoute() {
        val day = LocalDate.of(2026, 7, 20)
        val at3pm = day.atTime(15, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val shortDone = sample(
            date = "2026-07-20",
            plannedEnd = "2026-07-20",
            actualFinish = "2026-07-20",
            puTime = "2026-07-20 08:00",
            delTime = "2026-07-20 12:00",
        ).copy(id = "short", updatedAt = 100L)
        val evening = sample(
            date = "2026-07-20",
            plannedEnd = "2026-07-21",
            actualFinish = null,
            puTime = "2026-07-20 22:00",
            delTime = "2026-07-21 18:00",
        ).copy(id = "evening", updatedAt = 999L)
        assertNull(
            ActiveLoadSelector.selectForMapRoute(listOf(shortDone, evening), day, at3pm),
        )
        val afterPu = day.atTime(22, 30)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        assertEquals(
            "evening",
            ActiveLoadSelector.selectForMapRoute(listOf(shortDone, evening), day, afterPu)?.id,
        )
    }

    @Test
    fun prefersChronologicalWeekLoadOverNewerUpdatedAt() {
        val early = sample(
            date = "2026-07-20",
            plannedEnd = "2026-07-20",
            actualFinish = null,
            puTime = "2026-07-20 08:00",
            delTime = "2026-07-20 18:00",
        ).copy(id = "early", updatedAt = 1L)
        val laterEditedButStillFuture = sample(
            date = "2026-07-20",
            plannedEnd = "2026-07-21",
            actualFinish = null,
            puTime = "2026-07-20 22:00",
            delTime = "2026-07-21 12:00",
        ).copy(id = "later", updatedAt = 99_999L)
        assertEquals(
            "early",
            ActiveLoadSelector.selectActive(
                listOf(laterEditedButStillFuture, early),
                today,
                noonTodayMs,
            )?.id,
        )
    }

    @Test
    fun ignoresCompletedFinishOverride() {
        val load = sample(
            date = "2026-07-18",
            plannedEnd = "2026-07-25",
            actualFinish = "2026-07-19",
        )
        assertEquals(SharedLoadStatus.COMPLETED, ActiveLoadSelector.statusFor(load, today, noonTodayMs))
        assertNull(ActiveLoadSelector.selectActive(listOf(load), today, noonTodayMs))
    }

    @Test
    fun ignoresPastEndDate() {
        val load = sample(date = "2026-07-10", plannedEnd = "2026-07-15", actualFinish = null)
        assertEquals(SharedLoadStatus.COMPLETED, ActiveLoadSelector.statusFor(load, today, noonTodayMs))
    }

    @Test
    fun ignoresFutureStart() {
        val load = sample(date = "2026-07-25", plannedEnd = "2026-07-28", actualFinish = null)
        assertEquals(SharedLoadStatus.FUTURE, ActiveLoadSelector.statusFor(load, today, noonTodayMs))
    }

    @Test
    fun selectForMapRouteOnlyReturnsActiveNeverUpcoming() {
        val active = sample(date = "2026-07-19", plannedEnd = "2026-07-22", actualFinish = null)
            .copy(id = "active", updatedAt = 1L)
        val upcoming = sample(date = "2026-07-25", plannedEnd = "2026-07-28", actualFinish = null)
            .copy(id = "future", updatedAt = 99L)
        assertEquals("active", ActiveLoadSelector.selectForMapRoute(listOf(upcoming, active), today, noonTodayMs)?.id)
        assertNull(ActiveLoadSelector.selectForMapRoute(listOf(upcoming), today, noonTodayMs))
    }

    @Test
    fun finishDayAfterDelClockMarksCompleted() {
        val finishDay = LocalDate.of(2026, 7, 20)
        val delAt610pm = finishDay.atTime(18, 10)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val at813pm = finishDay.atTime(20, 13)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val load = sample(
            date = "2026-07-18",
            plannedEnd = "2026-07-20",
            actualFinish = null,
            puTime = "2026-07-18 08:00",
            delTime = "2026-07-20 18:10",
        )
        assertEquals(
            SharedLoadStatus.ACTIVE,
            ActiveLoadSelector.statusFor(load, finishDay, delAt610pm - 60_000),
        )
        assertEquals(
            SharedLoadStatus.COMPLETED,
            ActiveLoadSelector.statusFor(load, finishDay, at813pm),
        )
        assertNull(ActiveLoadSelector.selectActive(listOf(load), finishDay, at813pm))
    }

    @Test
    fun earlyActualFinishHidesRouteOnMultiDayTrip() {
        val load = sample(
            date = "2026-07-18",
            plannedEnd = "2026-07-25",
            actualFinish = "2026-07-20",
        )
        assertEquals(SharedLoadStatus.COMPLETED, ActiveLoadSelector.statusFor(load, today, noonTodayMs))
        assertNull(ActiveLoadSelector.selectForMapRoute(listOf(load), today, noonTodayMs))
    }

    @Test
    fun finishDateTimeAllowsNewSameDayLoadAfterEarlyFinish() {
        val day = LocalDate.of(2026, 7, 20)
        val at2pm = day.atTime(14, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val at4pm = day.atTime(16, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        // Planned through tomorrow; finished today at 14:00 with explicit time.
        val earlyDone = sample(
            date = "2026-07-18",
            plannedEnd = "2026-07-21",
            actualFinish = "2026-07-20 14:00",
            puTime = "2026-07-18 08:00",
            delTime = "2026-07-21 18:00",
        ).copy(id = "early", updatedAt = 1L)
        val nextLoad = sample(
            date = "2026-07-20",
            plannedEnd = "2026-07-21",
            actualFinish = null,
            puTime = "2026-07-20 15:00",
            delTime = "2026-07-21 12:00",
        ).copy(id = "next", updatedAt = 2L)

        assertEquals(
            SharedLoadStatus.ACTIVE,
            ActiveLoadSelector.statusFor(earlyDone, day, at2pm - 60_000),
        )
        assertEquals(
            SharedLoadStatus.COMPLETED,
            ActiveLoadSelector.statusFor(earlyDone, day, at2pm),
        )
        // Before next PU → no route; after next PU → next load's route.
        assertNull(ActiveLoadSelector.selectForMapRoute(listOf(earlyDone, nextLoad), day, at2pm))
        assertEquals(
            "next",
            ActiveLoadSelector.selectForMapRoute(listOf(earlyDone, nextLoad), day, at4pm)?.id,
        )
    }

    private fun stop(
        number: Int,
        type: StopType,
        scheduledTime: String,
        city: String,
        state: String,
        loadId: String = "L1",
    ) = Stop(
        id = number,
        loadId = loadId,
        stopNumber = number,
        type = type,
        puNumber = null,
        note = null,
        scheduledTime = scheduledTime,
        timezone = "EDT",
        facilityCode = null,
        fullAddress = "$city, $state",
        city = city,
        state = state,
        zip = "",
    )

    private fun sample(
        date: String,
        plannedEnd: String,
        actualFinish: String? = null,
        puTime: String = "$date 08:00",
        delTime: String = "$plannedEnd 23:59",
    ) = Load(
        id = "L1",
        tripId = "T1",
        date = date,
        totalRate = 1000.0,
        totalMiles = 400.0,
        pointA = "Seattle, WA",
        pointB = "Portland, OR",
        puCount = 1,
        delCount = 1,
        weekNumber = weekPair.first,
        year = weekPair.second,
        rawMessage = "",
        parsedAt = 1L,
        updatedAt = 2L,
        actualFinishDate = actualFinish,
        lastDelCityState = "Portland, OR",
        stops = listOf(
            stop(1, StopType.PU, puTime, "Seattle", "WA"),
            stop(2, StopType.DEL, delTime, "Portland", "OR"),
        ),
    )
}

class RouteIntersectionMatcherTest {
    @Test
    fun findsSameCorridorAndDates() {
        val friend = FriendActiveRoute(
            userId = "f1",
            displayName = "Ivan",
            loadRef = "x",
            originLabel = "Seattle, WA",
            destinationLabel = "Portland, OR",
            origin = null,
            destination = null,
            startDate = "2026-07-19",
            endDate = "2026-07-22",
            status = SharedLoadStatus.ACTIVE,
            trackPoints = emptyList(),
        )
        val matches = RouteIntersectionMatcher.findOverlaps(
            myOriginState = "WA",
            myDestState = "OR",
            myStartDate = "2026-07-20",
            myEndDate = "2026-07-21",
            friendRoutes = listOf(friend),
        )
        assertEquals(1, matches.size)
        assertTrue(matches[0].score >= 2.0)
    }

    @Test
    fun skipsNonOverlappingDates() {
        val friend = FriendActiveRoute(
            userId = "f1",
            displayName = "Ivan",
            loadRef = null,
            originLabel = "WA",
            destinationLabel = "OR",
            origin = null,
            destination = null,
            startDate = "2026-08-01",
            endDate = "2026-08-05",
            status = SharedLoadStatus.ACTIVE,
            trackPoints = emptyList(),
        )
        assertTrue(
            RouteIntersectionMatcher.findOverlaps(
                "WA", "OR", "2026-07-20", "2026-07-21", listOf(friend),
            ).isEmpty(),
        )
    }
}

class FriendRoutePolylineBuilderTest {
    @Test
    fun splitsPastGrayAndRemainingBlueSegments() {
        val route = FriendActiveRoute(
            userId = "f1",
            displayName = "Ivan",
            loadRef = null,
            originLabel = "A",
            destinationLabel = "B",
            origin = LatLngPoint(47.6, -122.3),
            destination = LatLngPoint(45.5, -122.6),
            startDate = "2026-07-19",
            endDate = "2026-07-22",
            status = SharedLoadStatus.ACTIVE,
            trackPoints = listOf(LatLngPoint(47.0, -122.4)),
        )
        val split = FriendRoutePolylineBuilder.split(route, current = LatLngPoint(46.5, -122.5))
        assertTrue(split.past.size >= 2)
        assertTrue(split.remaining.size >= 2)
        assertEquals(46.5, split.remaining.first().lat, 0.001)
        assertEquals(45.5, split.remaining.last().lat, 0.001)
    }
}
