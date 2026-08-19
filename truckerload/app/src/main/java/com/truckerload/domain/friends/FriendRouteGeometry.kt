package com.truckerload.domain.friends

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Splits a friend route into past (gray) and remaining (blue) polylines.
 * Past = track crumbs + line to current position.
 * Remaining = road polyline when provided, otherwise a straight current→destination segment.
 */
object FriendRoutePolylineBuilder {

    data class SplitPolylines(
        val past: List<LatLngPoint>,
        val remaining: List<LatLngPoint>,
    )

    fun split(
        route: FriendActiveRoute,
        current: LatLngPoint?,
        roadRemaining: List<LatLngPoint>? = null,
        roadTraveled: List<LatLngPoint>? = null,
    ): SplitPolylines {
        val origin = route.origin
        val dest = route.destination
        val track = route.trackPoints

        val past = if (roadTraveled != null && roadTraveled.size >= 2) {
            roadTraveled.distinctConsecutive()
        } else {
            buildList {
                if (origin != null) add(origin)
                addAll(track)
                if (current != null) add(current)
            }.distinctConsecutive()
        }

        val remaining = if (roadRemaining != null && roadRemaining.size >= 2) {
            roadRemaining.distinctConsecutive()
        } else {
            buildList {
                val start = current ?: track.lastOrNull() ?: origin
                if (start != null) add(start)
                if (dest != null) add(dest)
            }.distinctConsecutive()
        }

        return SplitPolylines(past = past, remaining = remaining)
    }

    private fun List<LatLngPoint>.distinctConsecutive(): List<LatLngPoint> {
        if (isEmpty()) return this
        val out = ArrayList<LatLngPoint>(size)
        for (p in this) {
            val last = out.lastOrNull()
            if (last == null || last.lat != p.lat || last.lng != p.lng) out.add(p)
        }
        return out
    }
}

/**
 * Finds friends whose active routes overlap the user's active load
 * (same dates window and/or shared origin/destination state codes).
 */
object RouteIntersectionMatcher {

    fun findOverlaps(
        myOriginState: String?,
        myDestState: String?,
        myStartDate: String,
        myEndDate: String,
        friendRoutes: List<FriendActiveRoute>,
    ): List<RouteOverlapMatch> {
        val myStart = myStartDate.take(10)
        val myEnd = myEndDate.take(10)
        return friendRoutes.mapNotNull { friend ->
            if (friend.status != SharedLoadStatus.ACTIVE) return@mapNotNull null
            val datesOverlap = datesOverlap(myStart, myEnd, friend.startDate.take(10), friend.endDate.take(10))
            if (!datesOverlap) return@mapNotNull null

            val friendOrigin = extractState(friend.originLabel)
            val friendDest = extractState(friend.destinationLabel)
            var score = 0.0
            val reasons = mutableListOf<String>()
            if (!myOriginState.isNullOrBlank() && myOriginState.equals(friendOrigin, ignoreCase = true)) {
                score += 2.0
                reasons += "same origin $myOriginState"
            }
            if (!myDestState.isNullOrBlank() && myDestState.equals(friendDest, ignoreCase = true)) {
                score += 2.0
                reasons += "same destination $myDestState"
            }
            if (!myOriginState.isNullOrBlank() && myOriginState.equals(friendDest, ignoreCase = true)) {
                score += 1.5
                reasons += "you start where they end ($myOriginState)"
            }
            if (!myDestState.isNullOrBlank() && myDestState.equals(friendOrigin, ignoreCase = true)) {
                score += 1.5
                reasons += "they start where you end ($myDestState)"
            }
            if (score == 0.0) {
                score = 0.5
                reasons += "overlapping travel dates"
            }
            RouteOverlapMatch(
                friendUserId = friend.userId,
                friendDisplayName = friend.displayName,
                reason = reasons.joinToString("; "),
                score = score,
            )
        }.sortedByDescending { it.score }
    }

    fun datesOverlap(aStart: String, aEnd: String, bStart: String, bEnd: String): Boolean {
        // ISO dates compare lexicographically when YYYY-MM-DD
        return aStart <= bEnd && bStart <= aEnd
    }

    fun haversineKm(a: LatLngPoint, b: LatLngPoint): Double {
        val r = 6371.0
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLng = Math.toRadians(b.lng - a.lng)
        val lat1 = Math.toRadians(a.lat)
        val lat2 = Math.toRadians(b.lat)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1) * cos(lat2) * sin(dLng / 2) * sin(dLng / 2)
        return 2 * r * atan2(sqrt(h), sqrt(1 - h))
    }

    private fun extractState(label: String): String? {
        val trimmed = label.trim()
        if (trimmed.length == 2 && trimmed.all { it.isLetter() }) return trimmed.uppercase()
        val comma = trimmed.lastIndexOf(',')
        if (comma >= 0) {
            val tail = trimmed.substring(comma + 1).trim()
            val code = tail.take(2)
            if (code.length == 2 && code.all { it.isLetter() }) return code.uppercase()
        }
        return null
    }
}
