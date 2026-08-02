package com.truckerload.presentation.screens.social.friends.map

import com.truckerload.data.remote.RoadDirectionsClient
import com.truckerload.domain.friends.FriendActiveRoute
import com.truckerload.domain.friends.FriendRoutePolylineBuilder
import com.truckerload.domain.friends.LatLngPoint
import com.truckerload.domain.friends.RoadRouteGeometry
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves road-following polylines for the friends map.
 * Caches full road geometry per route key and replans when the driver leaves the corridor
 * or the destination changes.
 */
class FriendsMapRoadRouter(
    private val directions: RoadDirectionsClient = RoadDirectionsClient(),
) {

    private val fullRoadByKey = ConcurrentHashMap<String, List<LatLngPoint>>()

    suspend fun splitWithRoads(
        routeKey: String,
        route: FriendActiveRoute,
        current: LatLngPoint?,
    ): FriendRoutePolylineBuilder.SplitPolylines {
        val dest = route.destination
        val start = current ?: route.trackPoints.lastOrNull() ?: route.origin
        if (dest == null || start == null) {
            return FriendRoutePolylineBuilder.split(route, current)
        }

        val previous = fullRoadByKey[routeKey].orEmpty()
        val needReplan = previous.size < 2 ||
            !sameBucket(previous.lastOrNull(), dest) ||
            RoadRouteGeometry.isOffRoute(previous, start)

        val fullRoad = if (needReplan) {
            directions.routeAlongRoads(start, dest, forceRefresh = true).also {
                if (it.size >= 2) fullRoadByKey[routeKey] = it
            }
        } else {
            previous
        }

        return FriendRoutePolylineBuilder.split(route, current, roadPath = fullRoad)
    }

    fun clear() {
        fullRoadByKey.clear()
        directions.clearCache()
    }

    private fun sameBucket(a: LatLngPoint?, b: LatLngPoint?): Boolean {
        if (a == null || b == null) return false
        return RoadRouteGeometry.cacheBucket(a) == RoadRouteGeometry.cacheBucket(b)
    }
}
