package com.truckerload.presentation.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.remote.SupabaseFriendsRealtimeService
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.friends.ActiveLoadSelector
import com.truckerload.domain.friends.FriendActiveRoute
import com.truckerload.domain.friends.FriendPresence
import com.truckerload.domain.friends.FriendProfileHit
import com.truckerload.domain.friends.FriendRoutePolylineBuilder
import com.truckerload.domain.friends.FriendShareLink
import com.truckerload.domain.friends.LatLngPoint
import com.truckerload.domain.friends.NicknameValidator
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
    val selectedFriendId: String? = null,
    val overlaps: List<RouteOverlapMatch> = emptyList(),
    val showOverlapsPanel: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val lastRefreshAt: Long = 0L,
)

class FriendsLiveMapViewModel(
    private val loadRepository: LoadRepository,
    private val settingsDataStore: SettingsDataStore,
    private val authStore: AuthStore,
    private val userProfileStore: UserProfileStore,
    private val friendsApi: SupabaseFriendsRealtimeService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsLiveMapUiState())
    val uiState = _uiState.asStateFlow()

    private var pollJob: Job? = null
    private val showPathFor = linkedSetOf<String>()

    init {
        val nick = userProfileStore.profile.value?.nickname.orEmpty()
        _uiState.update {
            it.copy(myNickname = nick, nicknameDraft = nick)
        }
        viewModelScope.launch {
            settingsDataStore.sharePathWithFriends.collect { enabled ->
                _uiState.update { it.copy(sharePathEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            userProfileStore.profile.collect { profile ->
                val n = profile?.nickname.orEmpty()
                _uiState.update { s ->
                    s.copy(
                        myNickname = n,
                        nicknameDraft = if (s.nicknameDraft.isBlank()) n else s.nicknameDraft,
                    )
                }
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

    fun setNicknameDraft(value: String) {
        _uiState.update { it.copy(nicknameDraft = value, nicknameMessage = null) }
    }

    fun saveNickname() {
        viewModelScope.launch {
            val draft = _uiState.value.nicknameDraft
            val handle = NicknameValidator.sanitizeOrNull(draft)
            if (handle == null) {
                _uiState.update { it.copy(nicknameMessage = "invalid") }
                return@launch
            }
            if (!friendsApi.isConfigured()) {
                persistLocalNickname(handle)
                _uiState.update { it.copy(nicknameMessage = "saved_local") }
                return@launch
            }
            val profile = userProfileStore.profile.value
            val result = friendsApi.upsertMyNickname(handle, profile?.displayName)
            val err = result.exceptionOrNull()?.message
            if (result.isSuccess) {
                persistLocalNickname(handle)
                _uiState.update { it.copy(nicknameMessage = "saved") }
            } else if (err == SupabaseFriendsRealtimeService.ERROR_NICKNAME_SCHEMA_MISSING) {
                persistLocalNickname(handle)
                _uiState.update {
                    it.copy(nicknameMessage = SupabaseFriendsRealtimeService.ERROR_NICKNAME_SCHEMA_MISSING)
                }
            } else {
                _uiState.update {
                    it.copy(errorMessage = err ?: "nickname save failed")
                }
            }
        }
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
                        FriendRoutePolylineBuilder.split(
                            route,
                            LatLngPoint(p.latitude, p.longitude),
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
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        shareLinks = links,
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

    fun clearStatus() {
        _uiState.update { it.copy(statusMessage = null, nicknameMessage = null) }
    }

    private fun persistLocalNickname(handle: String) {
        val current = userProfileStore.profile.value ?: return
        userProfileStore.saveProfile(current.copy(nickname = handle))
        _uiState.update { it.copy(myNickname = handle, nicknameDraft = handle) }
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
        private val userProfileStore: UserProfileStore,
        private val friendsApi: SupabaseFriendsRealtimeService,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FriendsLiveMapViewModel(
                loadRepository,
                settingsDataStore,
                authStore,
                userProfileStore,
                friendsApi,
            ) as T
    }
}
