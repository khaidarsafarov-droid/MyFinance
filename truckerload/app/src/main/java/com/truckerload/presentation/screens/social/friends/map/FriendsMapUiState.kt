package com.truckerload.presentation.screens.social.friends.map

import com.truckerload.domain.friends.FriendActiveRoute
import com.truckerload.domain.friends.FriendPresence
import com.truckerload.domain.friends.FriendProfileHit
import com.truckerload.domain.friends.FriendShareLink
import com.truckerload.domain.friends.LatLngPoint
import com.truckerload.domain.friends.RouteOverlapMatch

data class FriendMapOverlay(
    val presence: FriendPresence,
    val route: FriendActiveRoute?,
    val showPath: Boolean,
    val past: List<LatLngPoint>,
    val remaining: List<LatLngPoint>,
)

data class FriendsMapUiState(
    val isLoading: Boolean = true,
    val sharePathEnabled: Boolean = false,
    val supabaseReady: Boolean = false,
    val searchQuery: String = "",
    val searchHit: FriendProfileHit? = null,
    val searchNotFound: Boolean = false,
    val searchBusy: Boolean = false,
    val shareLinks: List<FriendShareLink> = emptyList(),
    val editingFriendId: String? = null,
    val friends: List<FriendMapOverlay> = emptyList(),
    /** Gray (driven) + blue (remaining) for the user's own active/upcoming load. */
    val myPathPast: List<LatLngPoint> = emptyList(),
    val myPathRemaining: List<LatLngPoint> = emptyList(),
    val myRouteSummary: String? = null,
    /** True when only a straight-line fallback is shown (OSRM unavailable). */
    val myRouteStraightFallback: Boolean = false,
    val selectedFriendId: String? = null,
    val overlaps: List<RouteOverlapMatch> = emptyList(),
    val showOverlapsPanel: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val lastRefreshAt: Long = 0L,
)
