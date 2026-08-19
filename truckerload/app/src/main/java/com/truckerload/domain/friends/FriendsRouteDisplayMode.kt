package com.truckerload.domain.friends

/**
 * Mutually exclusive friends-map route overlay:
 * [TRAVELED] = on-road path already driven, [REMAINING] = on-road path still ahead.
 */
enum class FriendsRouteDisplayMode {
    REMAINING,
    TRAVELED,
    ;

    companion object {
        fun fromStored(traveled: Boolean): FriendsRouteDisplayMode =
            if (traveled) TRAVELED else REMAINING
    }
}

object FriendsRouteDisplay {
    fun pastToDraw(mode: FriendsRouteDisplayMode, past: List<LatLngPoint>): List<LatLngPoint> =
        if (mode == FriendsRouteDisplayMode.TRAVELED) past else emptyList()

    fun remainingToDraw(
        mode: FriendsRouteDisplayMode,
        remaining: List<LatLngPoint>,
    ): List<LatLngPoint> =
        if (mode == FriendsRouteDisplayMode.REMAINING) remaining else emptyList()
}
