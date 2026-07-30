package com.truckerload.presentation.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.remote.SupabaseFriendsRealtimeService
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.friends.ActiveLoadSelector
import com.truckerload.domain.friends.FriendActiveRoute
import com.truckerload.domain.friends.FriendPresence
import com.truckerload.domain.friends.FriendRoutePolylineBuilder
import com.truckerload.domain.friends.LatLngPoint
import com.truckerload.domain.friends.RouteIntersectionMatcher
import com.truckerload.domain.friends.RouteOverlapMatch
import com.truckerload.domain.friends.SharedLoadStatus
import com.truckerload.utils.extractStateFromLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FriendMapOverlay(
    val presence: FriendPresence,
    val route: FriendActiveRoute?,
    val showPath: Boolean,
    val past: List<LatLngPoint>,
    val remaining: List<LatLngPoint>,
)

data class FriendsLiveMapUiState(
    val isLoading: Boolean = true,
    val sharePathEnabled: Boolean = false,
    val supabaseReady: Boolean = false,
    val friends: List<FriendMapOverlay> = emptyList(),
    val selectedFriendId: String? = null,
    val overlaps: List<RouteOverlapMatch> = emptyList(),
    val showOverlapsPanel: Boolean = false,
    val errorMessage: String? = null,
    val lastRefreshAt: Long = 0L,
)

class FriendsLiveMapViewModel(
    private val loadRepository: LoadRepository,
    private val settingsDataStore: SettingsDataStore,
    private val authStore: AuthStore,
    private val friendsApi: SupabaseFriendsRealtimeService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsLiveMapUiState())
    val uiState = _uiState.asStateFlow()

    private var pollJob: Job? = null
    private val showPathFor = linkedSetOf<String>()

    init {
        viewModelScope.launch {
            settingsDataStore.sharePathWithFriends.collect { enabled ->
                _uiState.update { it.copy(sharePathEnabled = enabled) }
            }
        }
        refresh()
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(20_000)
                refresh(silent = true)
            }
        }
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }

    fun refresh(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val ready = friendsApi.isConfigured()
                _uiState.update { it.copy(supabaseReady = ready) }
                val presence = if (ready) {
                    friendsApi.fetchFriendPresence().getOrElse { emptyList() }
                } else {
                    emptyList()
                }
                val routes = if (ready) {
                    friendsApi.fetchFriendActiveRoutes().getOrElse { emptyList() }
                } else {
                    emptyList()
                }
                val byUser = routes.associateBy { it.userId }
                val overlays = presence.map { p ->
                    val route = byUser[p.userId]
                    val show = p.userId in showPathFor
                    val split = if (show && route != null) {
                        FriendRoutePolylineBuilder.split(
                            route,
                            LatLngPoint(p.latitude, p.longitude),
                        )
                    } else {
                        FriendRoutePolylineBuilder.SplitPolylines(emptyList(), emptyList())
                    }
                    FriendMapOverlay(
                        presence = p,
                        route = route,
                        showPath = show,
                        past = split.past,
                        remaining = split.remaining,
                    )
                }
                val overlaps = computeOverlaps(routes)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        friends = overlays,
                        overlaps = overlaps,
                        lastRefreshAt = System.currentTimeMillis(),
                        errorMessage = null,
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: e.javaClass.simpleName)
                }
            }
        }
    }

    fun setSharePathEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveSharePathWithFriends(enabled)
        }
    }

    fun selectFriend(userId: String?) {
        _uiState.update { it.copy(selectedFriendId = userId) }
    }

    fun toggleShowPath(userId: String) {
        if (userId in showPathFor) showPathFor.remove(userId) else showPathFor.add(userId)
        refresh(silent = true)
    }

    fun setShowOverlapsPanel(show: Boolean) {
        _uiState.update { it.copy(showOverlapsPanel = show) }
    }

    private suspend fun computeOverlaps(friendRoutes: List<FriendActiveRoute>): List<RouteOverlapMatch> =
        withContext(Dispatchers.Default) {
            val myActive = ActiveLoadSelector.selectActive(loadRepository.getAllLoadsOnce())
                ?: return@withContext emptyList()
            RouteIntersectionMatcher.findOverlaps(
                myOriginState = extractStateFromLocation(myActive.pointA),
                myDestState = extractStateFromLocation(myActive.pointB),
                myStartDate = ActiveLoadSelector.startDateIso(myActive),
                myEndDate = ActiveLoadSelector.endDateIso(myActive),
                friendRoutes = friendRoutes.map {
                    it.copy(status = SharedLoadStatus.ACTIVE)
                },
            )
        }

    class Factory(
        private val loadRepository: LoadRepository,
        private val settingsDataStore: SettingsDataStore,
        private val authStore: AuthStore,
        private val friendsApi: SupabaseFriendsRealtimeService,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FriendsLiveMapViewModel(loadRepository, settingsDataStore, authStore, friendsApi) as T
    }
}
