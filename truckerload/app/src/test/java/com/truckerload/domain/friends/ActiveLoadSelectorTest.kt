package com.truckerload.domain.friends

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
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

    @Test
    fun activeWhenTodayInsideRange() {
        val load = sample(date = "2026-07-19", plannedEnd = "2026-07-22", actualFinish = null)
        assertEquals(SharedLoadStatus.ACTIVE, ActiveLoadSelector.statusFor(load, today, noonTodayMs))
        assertEquals(load.id, ActiveLoadSelector.selectActive(listOf(load), today, noonTodayMs)?.id)
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
        // No active load → no route (do not fall back to future facility).
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
        ).copy(
            stops = listOf(
                stop(1, StopType.PU, "2026-07-18 08:00", "A", "WA"),
                stop(2, StopType.DEL, "2026-07-20 18:10", "B", "OR"),
            ),
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

    private fun stop(
        number: Int,
        type: StopType,
        scheduledTime: String,
        city: String,
        state: String,
    ) = Stop(
        id = number,
        loadId = "L1",
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
        weekNumber = 30,
        year = 2026,
        rawMessage = "",
        parsedAt = 1L,
        updatedAt = 2L,
        actualFinishDate = actualFinish,
        lastDelCityState = "Portland, OR",
        stops = listOf(
            stop(1, StopType.PU, "$date 08:00", "Seattle", "WA"),
            stop(2, StopType.DEL, "$plannedEnd 23:59", "Portland", "OR"),
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
