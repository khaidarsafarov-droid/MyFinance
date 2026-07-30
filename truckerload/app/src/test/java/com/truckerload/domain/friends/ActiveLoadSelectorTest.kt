package com.truckerload.domain.friends

import com.truckerload.domain.model.Load
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ActiveLoadSelectorTest {

    private val today = LocalDate.of(2026, 7, 20)

    @Test
    fun activeWhenTodayInsideRange() {
        val load = sample(date = "2026-07-19", plannedEnd = "2026-07-22")
        assertEquals(SharedLoadStatus.ACTIVE, ActiveLoadSelector.statusFor(load, today))
        assertEquals(load.id, ActiveLoadSelector.selectActive(listOf(load), today)?.id)
    }

    @Test
    fun ignoresCompletedFinishOverride() {
        val load = sample(
            date = "2026-07-18",
            plannedEnd = "2026-07-25",
            actualFinish = "2026-07-19",
        )
        assertEquals(SharedLoadStatus.COMPLETED, ActiveLoadSelector.statusFor(load, today))
        assertNull(ActiveLoadSelector.selectActive(listOf(load), today))
    }

    @Test
    fun ignoresPastEndDate() {
        val load = sample(date = "2026-07-10", plannedEnd = "2026-07-15")
        assertEquals(SharedLoadStatus.COMPLETED, ActiveLoadSelector.statusFor(load, today))
    }

    @Test
    fun ignoresFutureStart() {
        val load = sample(date = "2026-07-25", plannedEnd = "2026-07-28")
        assertEquals(SharedLoadStatus.FUTURE, ActiveLoadSelector.statusFor(load, today))
    }

    @Test
    fun selectForMapRoutePrefersActiveThenUpcoming() {
        val active = sample(date = "2026-07-19", plannedEnd = "2026-07-22").copy(id = "active", updatedAt = 1L)
        val upcoming = sample(date = "2026-07-25", plannedEnd = "2026-07-28").copy(
            id = "future",
            updatedAt = 99L,
            actualFinishDate = null,
        )
        assertEquals("active", ActiveLoadSelector.selectForMapRoute(listOf(upcoming, active), today)?.id)
        assertEquals("future", ActiveLoadSelector.selectForMapRoute(listOf(upcoming), today)?.id)
    }

    private fun sample(
        date: String,
        plannedEnd: String,
        actualFinish: String? = plannedEnd,
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
