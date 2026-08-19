package com.truckerload.presentation.screens.social.friends.map

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.community.FriendSafetyClient
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.remote.CompositeDirectionsProvider
import com.truckerload.data.remote.SupabaseFriendsRealtimeService
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.social.ProfileRepository
import com.truckerload.domain.friends.ActiveLoadSelector
import com.truckerload.domain.friends.FriendActiveRoute
import com.truckerload.domain.friends.FriendMapLabels
import com.truckerload.domain.friends.FriendRoutePolylineBuilder
import com.truckerload.domain.friends.FriendsRouteDisplayMode
import com.truckerload.domain.friends.LatLngPoint
import com.truckerload.domain.friends.FriendRequestDirection
import com.truckerload.domain.friends.FriendRequestSendResult
import com.truckerload.domain.friends.NicknameValidator
import com.truckerload.domain.friends.RoadRouteResult
import com.truckerload.domain.friends.RoadRouteSession
import com.truckerload.domain.friends.RouteIntersectionMatcher
import com.truckerload.domain.friends.RouteOverlapMatch
import com.truckerload.domain.friends.SharedLoadStatus
import com.truckerload.domain.friends.TruckRoutingParams
import com.truckerload.domain.friends.VehicleRoutingMode
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
    private val profileRepository: ProfileRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val friendsApi = SupabaseFriendsRealtimeService(authStore)
    private val safetyApi = FriendSafetyClient(authStore)
    private val locationHelper = LocationHelper(context)
    private val directions = CompositeDirectionsProvider()
    private var roadRoutes = RoadRouteSession(directions)

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
        viewModelScope.launch {
            settingsDataStore.locationBatterySaver.collect { enabled ->
                _uiState.update { it.copy(locationBatterySaver = enabled) }
            }
        }
        viewModelScope.launch {
            settingsDataStore.routeVehicleTruck.collect { truck ->
                _uiState.update { it.copy(routeVehicleTruck = truck) }
                rebuildRoadSession(truck)
                rebuildMyPath()
            }
        }
        viewModelScope.launch {
            settingsDataStore.friendsRouteShowTraveled.collect { traveled ->
                _uiState.update {
                    it.copy(routeDisplayMode = FriendsRouteDisplayMode.fromStored(traveled))
                }
            }
        }
        viewModelScope.launch {
            profileRepository.watchMyProfile().collect { profile ->
                _uiState.update {
                    it.copy(myAvatarUrl = profile.avatarUrl?.takeIf { url -> url.isNotBlank() })
                }
            }
        }
        refresh()
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(20_000)
                pullMyLocationQuietly()
                refresh(silent = true)
            }
        }
    }

    private fun rebuildRoadSession(truck: Boolean) {
        roadRoutes.clear()
        roadRoutes = RoadRouteSession(
            directions = directions,
            vehicleMode = if (truck) VehicleRoutingMode.TRUCK else VehicleRoutingMode.CAR,
            truckParams = TruckRoutingParams(),
        )
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
            val send = safetyApi.sendFriendRequest(hit.userId)
            val result = send.fold(
                onSuccess = { Result.success(it) },
                onFailure = { err ->
                    if (err.message == FriendSafetyClient.ERROR_SAFETY_SCHEMA_MISSING) {
                        friendsApi.addFriend(hit).map { FriendRequestSendResult.ADDED_DIRECT }
                    } else {
                        Result.failure(err)
                    }
                },
            )
            if (result.isSuccess) {
                val status = when (result.getOrNull()) {
                    FriendRequestSendResult.SENT -> "request_sent"
                    FriendRequestSendResult.ALREADY_SENT -> "already_sent"
                    FriendRequestSendResult.ALREADY_FRIENDS -> "already_friends"
                    FriendRequestSendResult.ACCEPTED -> "accepted"
                    FriendRequestSendResult.BLOCKED -> "blocked"
                    FriendRequestSendResult.ADDED_DIRECT -> "added"
                    null -> "request_sent"
                }
                _uiState.update {
                    it.copy(
                        searchHit = null,
                        searchQuery = "",
                        statusMessage = status,
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

    fun acceptFriendRequest(requestId: String) {
        viewModelScope.launch {
            safetyApi.acceptFriendRequest(requestId).onSuccess {
                _uiState.update { it.copy(statusMessage = "accepted") }
                refresh(silent = true)
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.message) }
            }
        }
    }

    fun declineFriendRequest(requestId: String) {
        viewModelScope.launch {
            safetyApi.declineFriendRequest(requestId).onSuccess {
                refresh(silent = true)
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.message) }
            }
        }
    }

    fun cancelFriendRequest(requestId: String) {
        viewModelScope.launch {
            safetyApi.cancelFriendRequest(requestId).onSuccess {
                refresh(silent = true)
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.message) }
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
                val requests = if (ready) {
                    safetyApi.listFriendRequests().getOrElse { emptyList() }
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
                val nameById = links.associate { it.friendUserId to it.friendDisplayName }
                val overlays = presence.map { p ->
                    val route = byUser[p.userId]
                    val show = p.userId in showPathFor
                    val current = LatLngPoint(p.latitude, p.longitude)
                    val roadResult = if (show && route != null) {
                        roadRoutes.remainingRoadResult(
                            cacheKey = p.userId,
                            currentOrStart = current,
                            destination = route.destination,
                            fetchOrigin = route.origin ?: current,
                        )
                    } else {
                        null
                    }
                    val split = if (show && route != null) {
                        FriendRoutePolylineBuilder.split(
                            route,
                            current,
                            roadRemaining = roadResult?.points,
                            roadTraveled = roadResult?.traveledPoints,
                        )
                    } else {
                        FriendRoutePolylineBuilder.SplitPolylines(emptyList(), emptyList())
                    }
                    val labeled = p.copy(
                        displayName = FriendMapLabels.friendVisibleName(
                            presenceDisplayName = p.displayName,
                            linkDisplayName = nameById[p.userId],
                            nickname = nickById[p.userId],
                        ),
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
                        incomingRequests = requests.filter {
                            it.direction == FriendRequestDirection.INCOMING
                        },
                        outgoingRequests = requests.filter {
                            it.direction == FriendRequestDirection.OUTGOING
                        },
                        friends = overlays,
                        myPathPast = myPath.past,
                        myPathRemaining = myPath.remaining,
                        myRouteSummary = myPath.summary,
                        myRouteIsRoadNetwork = myPath.road?.isRoadNetwork == true,
                        myRouteProvider = myPath.road?.providerName,
                        myRouteFailureReason = myPath.road?.failureReason,
                        myRouteDistanceMeters = myPath.road?.distanceMeters,
                        myRouteDurationSeconds = myPath.road?.durationSeconds,
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
                myRouteIsRoadNetwork = myPath.road?.isRoadNetwork == true,
                myRouteProvider = myPath.road?.providerName,
                myRouteFailureReason = myPath.road?.failureReason,
                myRouteDistanceMeters = myPath.road?.distanceMeters,
                myRouteDurationSeconds = myPath.road?.durationSeconds,
            )
        }
    }

    private data class MyPathDraw(
        val past: List<LatLngPoint>,
        val remaining: List<LatLngPoint>,
        val summary: String?,
        val road: RoadRouteResult? = null,
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
        val start = myLocationPoint ?: origin
        val roadResult = roadRoutes.remainingRoadResult(
            cacheKey = RoadRouteSession.SELF_CACHE_KEY,
            currentOrStart = start,
            destination = destination,
            fetchOrigin = origin ?: start,
        )
        val split = FriendRoutePolylineBuilder.split(
            route,
            myLocationPoint,
            roadRemaining = roadResult.points,
            roadTraveled = roadResult.traveledPoints,
        )
        val summary = listOf(originLabel, destLabel)
            .filter { it.isNotBlank() }
            .joinToString(" → ")
            .ifBlank { null }
        MyPathDraw(
            past = split.past,
            remaining = split.remaining,
            summary = summary,
            road = roadResult,
        )
    }

    private suspend fun pullMyLocationQuietly() {
        if (!locationHelper.hasLocationPermission()) return
        val loc = locationHelper.getCurrentLocation() ?: return
        val lat = loc.latitude ?: return
        val lng = loc.longitude ?: return
        myLocationPoint = LatLngPoint(lat, lng)
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

    fun setLocationBatterySaver(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveLocationBatterySaver(enabled)
        }
    }

    fun setRouteVehicleTruck(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveRouteVehicleTruck(enabled)
        }
    }

    fun setRouteDisplayMode(mode: FriendsRouteDisplayMode) {
        viewModelScope.launch {
            settingsDataStore.saveFriendsRouteShowTraveled(mode == FriendsRouteDisplayMode.TRAVELED)
        }
    }

    /** GPS poll interval while the map screen is open (foreground only). */
    fun locationPollIntervalMs(): Long =
        if (_uiState.value.locationBatterySaver) LOCATION_INTERVAL_BATTERY_MS else LOCATION_INTERVAL_DEFAULT_MS

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
        const val LOCATION_INTERVAL_DEFAULT_MS = 5_000L
        const val LOCATION_INTERVAL_BATTERY_MS = 10_000L
    }
}
