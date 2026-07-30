package com.truckerload.presentation.screens.map

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

/**
 * Flat ViewModel state (updated via `copy`).
 * Map chrome / permission modes use [FriendsMapChrome].
 */
data class FriendsLiveMapUiState(
    val isLoading: Boolean = true,
    val sharePathEnabled: Boolean = false,
    val supabaseReady: Boolean = false,
    val myNickname: String = "",
    val nicknameDraft: String = "",
    val nicknameMessage: String? = null,
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
    val selectedFriendId: String? = null,
    val overlaps: List<RouteOverlapMatch> = emptyList(),
    val showOverlapsPanel: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val lastRefreshAt: Long = 0L,
)

/** Composition-level map presentation modes. */
sealed interface FriendsMapChrome {
    /** Compact self-preview card on the friends screen. */
    data object Preview : FriendsMapChrome

    /** Full-bleed map dialog with friends overlays. */
    data class Fullscreen(
        val centerOnMeNonce: Int,
        val showMyLocationLayer: Boolean,
    ) : FriendsMapChrome
}

/** Derived map content for overlays / loading affordances. */
sealed interface FriendsMapContent {
    data object Loading : FriendsMapContent

    data class Ready(
        val overlays: List<FriendMapOverlay>,
        val myPathPast: List<LatLngPoint>,
        val myPathRemaining: List<LatLngPoint>,
        val selectedFriendId: String?,
    ) : FriendsMapContent

    data class Failed(val message: String) : FriendsMapContent
}

fun FriendsLiveMapUiState.toMapContent(
    mapExpanded: Boolean,
    hasMyLocation: Boolean,
): FriendsMapContent {
    errorMessage?.takeIf { it.isNotBlank() }?.let { return FriendsMapContent.Failed(it) }
    if (isLoading && !hasMyLocation && !mapExpanded) return FriendsMapContent.Loading
    return FriendsMapContent.Ready(
        overlays = friends,
        myPathPast = myPathPast,
        myPathRemaining = myPathRemaining,
        selectedFriendId = selectedFriendId,
    )
}
