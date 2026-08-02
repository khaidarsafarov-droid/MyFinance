package com.truckerload.domain.friends

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoadRouteResolverTest {

  private class FakeDirections(
    private val points: List<LatLngPoint>?,
  ) : DirectionsProvider {
    override suspend fun fetchDrivingRoute(
      origin: LatLngPoint,
      destination: LatLngPoint,
    ): List<LatLngPoint>? = points
  }

    @Test
    fun enrichRemainingUsesRoadGeometry() = runBlocking {
        val road = listOf(
            LatLngPoint(40.0, -100.0),
            LatLngPoint(40.5, -99.5),
            LatLngPoint(41.0, -99.0),
        )
        val resolver = RoadRouteResolver(FakeDirections(road))
        val split = FriendRoutePolylineBuilder.SplitPolylines(
            past = listOf(LatLngPoint(39.0, -101.0)),
            remaining = listOf(LatLngPoint(40.0, -100.0), LatLngPoint(41.0, -99.0)),
        )
        val enriched = resolver.enrichRemaining(split, current = null, cacheKey = "test")
        assertEquals(3, enriched.size)
        assertEquals(40.0, enriched.first().lat, 0.001)
    }

    @Test
    fun enrichRemainingFallsBackToStraightLineWhenApiFails() = runBlocking {
        val resolver = RoadRouteResolver(FakeDirections(null))
        val split = FriendRoutePolylineBuilder.SplitPolylines(
            past = emptyList(),
            remaining = listOf(LatLngPoint(40.0, -100.0), LatLngPoint(41.0, -99.0)),
        )
        val enriched = resolver.enrichRemaining(split, current = null, cacheKey = "test2")
        assertEquals(2, enriched.size)
    }

    @Test
    fun trimRouteFromCurrentDropsPassedPoints() {
        val resolver = RoadRouteResolver(FakeDirections(null))
        val route = listOf(
            LatLngPoint(40.0, -100.0),
            LatLngPoint(40.5, -99.5),
            LatLngPoint(41.0, -99.0),
        )
        val current = LatLngPoint(40.5, -99.5)
        val trimmed = resolver.trimRouteFromCurrent(route, current)
        assertTrue(trimmed.size >= 2)
        assertEquals(40.5, trimmed.first().lat, 0.001)
        assertEquals(41.0, trimmed.last().lat, 0.001)
    }
}
