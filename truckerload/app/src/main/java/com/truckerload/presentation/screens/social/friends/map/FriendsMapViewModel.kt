package com.truckerload.presentation.screens.social.friends.map

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.remote.SupabaseFriendsRealtimeService
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.friends.ActiveLoadSelector
import com.truckerload.domain.friends.FriendActiveRoute
import com.truckerload.domain.friends.FriendRoutePolylineBuilder
import com.truckerload.domain.friends.LatLngPoint
import com.truckerload.domain.friends.NicknameValidator
import com.truckerload.domain.friends.RouteIntersectionMatcher
import com.truckerload.domain.friends.RouteOverlapMatch
import com.truckerload.domain.friends.SharedLoadStatus
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.StopType
import com.truckerload.utils.LocationHelper
import com.truckerload.utils.extractStateFromLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class FriendsMapViewModel @Inject constructor(
    private val loadRepository: LoadRepository,
    private val settingsDataStore: SettingsDataStore,
    private val authStore: AuthStore,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val friendsApi = SupabaseFriendsRealtimeService(authStore)
    private val locationHelper = LocationHelper(context)
    private val roadRouter = FriendsMapRoadRouter()

    private val _uiState = MutableStateFlow(FriendsMapUiState())
    val uiState = _uiState.asStateFlow()

    private var pollJob: Job? = null
    private val showPathFor = linkedSetOf<String>()
    private var myLocationPoint: LatLngPoint? = null

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

    fun setSearchQuery(value: String) {
        _uiState.update {
            it.copy(searchQuery = value, searchHit = null, searchNotFound = false, statusMessage = null)
        }
    }

    fun searchFriend() {
        viewModelScope.launch {
            val q = _uiState.value.searchQuery
            if (!NicknameValidator.isValid(q)) {
                _uiState.update { it.copy(statusMessage = "invalid_search", searchHit = null, searchNotFound = false) }
                return@launch
            }
            if (!friendsApi.isConfigured()) {
                _uiState.update { it.copy(statusMessage = "need_supabase", searchHit = null) }
                return@launch
            }
            _uiState.update { it.copy(searchBusy = true, searchHit = null, searchNotFound = false) }
            val result = friendsApi.searchByNickname(q)
            val hit = result.getOrNull()
            val me = authStore.currentUserIdOrNull()
            when {
                result.isFailure -> _uiState.update {
                    it.copy(
                        searchBusy = false,
                        errorMessage = result.exceptionOrNull()?.message,
                    )
                }
                hit == null || hit.userId.isBlank() -> _uiState.update {
                    it.copy(searchBusy = false, searchNotFound = true, statusMessage = "not_found")
                }
                hit.userId == me -> _uiState.update {
                    it.copy(searchBusy = false, statusMessage = "self")
                }
                else -> _uiState.update {
                    it.copy(searchBusy = false, searchHit = hit, searchNotFound = false, statusMessage = null)
                }
            }
        }
    }

    fun addSearchedFriend() {
        viewModelScope.launch {
            val hit = _uiState.value.searchHit ?: return@launch
            val result = friendsApi.addFriend(hit)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        searchHit = null,
                        searchQuery = "",
                        statusMessage = "added",
                    )
                }
                refresh(silent = true)
            } else {
                _uiState.update {
                    it.copy(errorMessage = result.exceptionOrNull()?.message ?: "add failed")
                }
            }
        }
    }

    fun setEditingFriend(friendId: String?) {
        _uiState.update { it.copy(editingFriendId = friendId) }
    }

    fun updateSharePrefs(friendId: String, shareLocation: Boolean, shareRoute: Boolean) {
        viewModelScope.launch {
            val result = friendsApi.updateFriendShare(friendId, shareLocation, shareRoute)
            if (result.isSuccess) {
                _uiState.update { state ->
                    state.copy(
                        shareLinks = state.shareLinks.map {
                            if (it.friendUserId == friendId) {
                                it.copy(shareMyLocation = shareLocation, shareMyRoute = shareRoute)
                            } else {
                                it
                            }
                        },
                        statusMessage = "prefs_saved",
                    )
                }
            } else {
                _uiState.update {
                    it.copy(errorMessage = result.exceptionOrNull()?.message)
                }
            }
        }
    }

    fun removeFriend(friendId: String) {
        viewModelScope.launch {
            val result = friendsApi.removeFriend(friendId)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        shareLinks = it.shareLinks.filter { l -> l.friendUserId != friendId },
                        editingFriendId = null,
                        statusMessage = "removed",
                    )
                }
                refresh(silent = true)
            } else {
                _uiState.update {
                    it.copy(errorMessage = result.exceptionOrNull()?.message)
                }
            }
        }
    }

    fun updateMyLocation(lat: Double, lng: Double) {
        myLocationPoint = LatLngPoint(lat, lng)
        viewModelScope.launch { rebuildMyPath() }
    }

    fun refresh(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val ready = friendsApi.isConfigured()
                _uiState.update { it.copy(supabaseReady = ready) }
                val links = if (ready) {
                    friendsApi.listMyFriendLinks().getOrElse { emptyList() }
                } else {
                    emptyList()
                }
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
                val nickById = links.associate { it.friendUserId to it.friendNickname }
                val overlays = presence.map { p ->
                    val route = byUser[p.userId]
                    val show = p.userId in showPathFor
                    val split = if (show && route != null) {
                        roadRouter.splitWithRoads(
                            routeKey = "friend:${p.userId}:${route.loadRef.orEmpty()}",
                            route = route,
                            current = LatLngPoint(p.latitude, p.longitude),
                        )
                    } else {
                        FriendRoutePolylineBuilder.SplitPolylines(emptyList(), emptyList())
                    }
                    val labeled = p.copy(
                        displayName = nickById[p.userId]?.takeIf { it.isNotBlank() }
                            ?.let { "@$it" }
                            ?: p.displayName,
                    )
                    FriendMapOverlay(
                        presence = labeled,
                        route = route,
                        showPath = show,
                        past = split.past,
                        remaining = split.remaining,
                    )
                }
                val overlaps = computeOverlaps(routes)
                val myPath = buildMyPathOverlay()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        shareLinks = links,
                        friends = overlays,
                        myPathPast = myPath.past,
                        myPathRemaining = myPath.remaining,
                        myRouteSummary = myPath.summary,
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

    private suspend fun rebuildMyPath() {
        val myPath = buildMyPathOverlay()
        _uiState.update {
            it.copy(
                myPathPast = myPath.past,
                myPathRemaining = myPath.remaining,
                myRouteSummary = myPath.summary,
            )
        }
    }

    private data class MyPathDraw(
        val past: List<LatLngPoint>,
        val remaining: List<LatLngPoint>,
        val summary: String?,
    )

    private suspend fun buildMyPathOverlay(): MyPathDraw = withContext(Dispatchers.IO) {
        // Only draw a route for a load that is still ACTIVE (not finished / past DEL).
        val load = ActiveLoadSelector.selectActive(loadRepository.getAllLoadsOnce())
            ?: return@withContext MyPathDraw(emptyList(), emptyList(), null)
        val originLabel = load.pointA.ifBlank { load.firstPuCityState }
        val destLabel = load.pointB.ifBlank { load.lastDelCityState }
        val origin = geocodeLoadEndpoint(load, isOrigin = true)
        val destination = geocodeLoadEndpoint(load, isOrigin = false)
        if (origin == null && destination == null) {
            return@withContext MyPathDraw(emptyList(), emptyList(), null)
        }
        val route = FriendActiveRoute(
            userId = authStore.currentUserIdOrNull().orEmpty().ifBlank { SELF_ROUTE_ID },
            displayName = "Me",
            loadRef = load.id,
            originLabel = originLabel,
            destinationLabel = destLabel,
            origin = origin,
            destination = destination,
            startDate = ActiveLoadSelector.startDateIso(load),
            endDate = ActiveLoadSelector.endDateIso(load),
            status = ActiveLoadSelector.statusFor(load),
            trackPoints = emptyList(),
        )
        // New load id → new cache key so the blue line replans for the new destination.
        val split = roadRouter.splitWithRoads(
            routeKey = "me:${load.id}",
            route = route,
            current = myLocationPoint,
        )
        val summary = listOf(originLabel, destLabel)
            .filter { it.isNotBlank() }
            .joinToString(" → ")
            .ifBlank { null }
        MyPathDraw(past = split.past, remaining = split.remaining, summary = summary)
    }

    private suspend fun geocodeLoadEndpoint(load: Load, isOrigin: Boolean): LatLngPoint? {
        val labels = if (isOrigin) {
            listOf(
                load.stops.firstOrNull { it.type == StopType.PU }?.fullAddress,
                load.pointA,
                load.firstPuCityState,
            )
        } else {
            listOf(
                load.stops.lastOrNull { it.type == StopType.DEL }?.fullAddress,
                load.pointB,
                load.lastDelCityState,
            )
        }
        for (label in labels) {
            val q = label?.trim().orEmpty()
            if (q.isBlank()) continue
            locationHelper.geocodeAddress(q)?.let { return it }
        }
        return null
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

    fun clearStatus() {
        _uiState.update { it.copy(statusMessage = null) }
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

    companion object {
        const val SELF_ROUTE_ID = "__me__"
    }
}
