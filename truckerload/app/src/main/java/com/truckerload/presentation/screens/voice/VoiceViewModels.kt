package com.truckerload.presentation.screens.voice

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.VoiceRepository
import com.truckerload.data.repository.social.ProfileRepository
import com.truckerload.domain.voice.CallState
import com.truckerload.domain.voice.CallStatus
import com.truckerload.domain.voice.VoiceRoom
import com.truckerload.domain.voice.VoiceRoomRole
import com.truckerload.domain.voice.VoiceTransportKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VoiceRoomsUiState(
    val rooms: List<VoiceRoom> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val currentUserId: String = "",
)

@HiltViewModel
class VoiceRoomsViewModel @Inject constructor(
    private val voiceRepository: VoiceRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(VoiceRoomsUiState())
    val uiState: StateFlow<VoiceRoomsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { voiceRepository.ensureInitialized() }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = error.toUiMessage())
                }
            while (isActive) {
                delay(3_000)
                runCatching { voiceRepository.pullRemote() }
            }
        }
        viewModelScope.launch {
            voiceRepository.watchRooms()
                .catch { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = error.toUiMessage())
                }
                .collect { rooms ->
                    _uiState.value = VoiceRoomsUiState(
                        rooms = rooms,
                        isLoading = false,
                        currentUserId = voiceRepository.currentUserId(),
                    )
                }
        }
    }

    fun createRoom(name: String, description: String = "", onCreated: (String) -> Unit) {
        viewModelScope.launch {
            voiceRepository.createRoom(name, description = description)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(errorMessage = null)
                    onCreated(it)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(errorMessage = error.toUiMessage())
                }
        }
    }

    fun deleteRoom(roomId: String) {
        viewModelScope.launch {
            voiceRepository.deleteRoom(roomId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(errorMessage = null)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(errorMessage = error.toUiMessage())
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

data class VoiceRoomUiState(
    val room: VoiceRoom? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isMuted: Boolean = false,
    val isDeafened: Boolean = false,
    val durationSeconds: Long = 0,
    val audioBitrate: Int = 16_000,
    val currentUserId: String = "",
    val role: VoiceRoomRole = VoiceRoomRole.SPEAKER,
    val transport: VoiceTransportKind = VoiceTransportKind.NONE,
)

@HiltViewModel
class VoiceRoomViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val voiceRepository: VoiceRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {
    private val roomId = Uri.decode(savedStateHandle.get<String>("roomId").orEmpty())
    private val _local = MutableStateFlow(VoiceRoomUiState())
    val uiState: StateFlow<VoiceRoomUiState> = _local.asStateFlow()
    private var ticker: Job? = null
    private var levelJob: Job? = null
    private var joinedAt = 0L
    private var joined = false

    init {
        viewModelScope.launch {
            runCatching { voiceRepository.ensureInitialized() }
                .onFailure { error ->
                    _local.value = _local.value.copy(isLoading = false, errorMessage = error.toUiMessage())
                }
        }
        viewModelScope.launch {
            combine(
                profileRepository.watchMyEnhancedProfile(),
                voiceRepository.watchRoom(roomId, ""),
            ) { profile, room ->
                profile.displayName to room
            }.catch { error ->
                _local.value = _local.value.copy(isLoading = false, errorMessage = error.toUiMessage())
            }.collect { (displayName, room) ->
                if (!joined) {
                    val joinResult = voiceRepository.joinRoom(roomId, displayName, viewModelScope)
                    if (joinResult.isFailure) {
                        _local.value = _local.value.copy(
                            isLoading = false,
                            errorMessage = joinResult.exceptionOrNull()?.toUiMessage(),
                        )
                        return@collect
                    }
                    joined = true
                    joinedAt = System.currentTimeMillis()
                    startTicker()
                    startLevelMonitor()
                }
                voiceRepository.syncRoomPeers(
                    room?.participants.orEmpty().filterNot { it.isMe }.map { it.userId },
                )
                val role = voiceRepository.currentRole()
                _local.value = _local.value.copy(
                    room = room,
                    isLoading = false,
                    errorMessage = if (joined && room == null && _local.value.room != null) "room_gone" else null,
                    audioBitrate = voiceRepository.qualityManager.currentSettings().bitrate,
                    currentUserId = voiceRepository.currentUserId(),
                    role = role,
                    transport = voiceRepository.currentTransport(),
                    isMuted = if (role == VoiceRoomRole.LISTENER) true else _local.value.isMuted,
                )
            }
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                val seconds = ((System.currentTimeMillis() - joinedAt) / 1000).coerceAtLeast(0)
                _local.value = _local.value.copy(durationSeconds = seconds)
                delay(1_000)
            }
        }
    }

    private fun startLevelMonitor() {
        levelJob?.cancel()
        levelJob = viewModelScope.launch {
            while (isActive) {
                runCatching { voiceRepository.pullRemote() }
                voiceRepository.updateSpeakingLevel(roomId)
                delay(500)
            }
        }
    }

    fun toggleMute() {
        if (_local.value.role == VoiceRoomRole.LISTENER) return
        viewModelScope.launch {
            runCatching {
                val next = !_local.value.isMuted
                voiceRepository.setMuted(roomId, next)
                _local.value = _local.value.copy(isMuted = next, errorMessage = null)
            }.onFailure { error ->
                _local.value = _local.value.copy(errorMessage = error.toUiMessage())
            }
        }
    }

    fun setRole(role: VoiceRoomRole) {
        viewModelScope.launch {
            runCatching {
                voiceRepository.setRoomRole(roomId, role)
                _local.value = _local.value.copy(
                    role = role,
                    isMuted = role == VoiceRoomRole.LISTENER || _local.value.isMuted,
                    transport = voiceRepository.currentTransport(),
                    audioBitrate = voiceRepository.qualityManager.currentSettings().bitrate,
                    errorMessage = null,
                )
            }.onFailure { error ->
                _local.value = _local.value.copy(errorMessage = error.toUiMessage())
            }
        }
    }

    fun toggleDeafen() {
        viewModelScope.launch {
            runCatching {
                val next = !_local.value.isDeafened
                voiceRepository.setDeafened(roomId, next)
                _local.value = _local.value.copy(isDeafened = next, errorMessage = null)
            }.onFailure { error ->
                _local.value = _local.value.copy(errorMessage = error.toUiMessage())
            }
        }
    }

    fun leave(onLeft: () -> Unit) {
        viewModelScope.launch {
            voiceRepository.leaveRoom(roomId)
                .onSuccess { onLeft() }
                .onFailure { error ->
                    _local.value = _local.value.copy(errorMessage = error.toUiMessage())
                }
        }
    }

    fun updateRoom(name: String? = null, description: String? = null, moderatorId: String? = null, clearModerator: Boolean = false) {
        viewModelScope.launch {
            voiceRepository.updateRoom(
                roomId = roomId,
                name = name,
                description = description,
                moderatorId = moderatorId,
                clearModerator = clearModerator,
            ).onFailure { error ->
                _local.value = _local.value.copy(errorMessage = error.toUiMessage())
            }
        }
    }

    fun deleteRoom(onDeleted: () -> Unit) {
        viewModelScope.launch {
            voiceRepository.deleteRoom(roomId)
                .onSuccess { onDeleted() }
                .onFailure { error ->
                    _local.value = _local.value.copy(errorMessage = error.toUiMessage())
                }
        }
    }

    fun clearError() {
        _local.value = _local.value.copy(errorMessage = null)
    }

    override fun onCleared() {
        ticker?.cancel()
        levelJob?.cancel()
        val shouldLeave = joined
        val id = roomId
        val repo = voiceRepository
        super.onCleared()
        if (shouldLeave) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                repo.leaveRoom(id)
            }
        }
    }
}

private fun Throwable.toUiMessage(): String =
    localizedMessage ?: message ?: javaClass.simpleName

data class CallUiState(
    val call: CallState? = null,
    val isMuted: Boolean = false,
    val durationSeconds: Long = 0,
)

@HiltViewModel
class CallViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val voiceRepository: VoiceRepository,
) : ViewModel() {
    private val callId = Uri.decode(savedStateHandle.get<String>("callId").orEmpty())
    private val _local = MutableStateFlow(CallUiState())
    val uiState: StateFlow<CallUiState> = _local.asStateFlow()
    private var ticker: Job? = null
    private var audioStarted = false

    init {
        viewModelScope.launch {
            voiceRepository.watchCall(callId).collect { call ->
                _local.value = _local.value.copy(call = call)
                if (call?.status == CallStatus.ACTIVE && ticker == null) {
                    startTicker(call.startedAt)
                }
                if (call?.status == CallStatus.ACTIVE && !audioStarted) {
                    audioStarted = true
                    voiceRepository.beginCallAudio(
                        viewModelScope,
                        callId,
                        isCaller = call.isIncoming != true,
                    )
                }
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(500)
                runCatching { voiceRepository.pullRemote() }
            }
        }
    }

    private fun startTicker(startedAt: Long) {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                val seconds = ((System.currentTimeMillis() - startedAt) / 1000).coerceAtLeast(0)
                _local.value = _local.value.copy(durationSeconds = seconds)
                delay(1_000)
            }
        }
    }

    fun toggleMute() {
        val next = !_local.value.isMuted
        voiceRepository.setCallMuted(next)
        _local.value = _local.value.copy(isMuted = next)
    }

    fun endCall(onEnded: () -> Unit) {
        viewModelScope.launch {
            voiceRepository.endCall(callId)
            onEnded()
        }
    }

    override fun onCleared() {
        ticker?.cancel()
        super.onCleared()
    }
}

@HiltViewModel
class IncomingCallViewModel @Inject constructor(
    private val voiceRepository: VoiceRepository,
) : ViewModel() {
    val incomingCall: StateFlow<CallState?> =
        voiceRepository.watchIncomingCall()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(3_000)
                runCatching { voiceRepository.pullRemote() }
            }
        }
    }

    fun accept(callId: String, onAccepted: (String) -> Unit) {
        viewModelScope.launch {
            voiceRepository.acceptCall(callId).onSuccess {
                voiceRepository.beginCallAudio(viewModelScope, callId, isCaller = false)
                onAccepted(callId)
            }
        }
    }

    fun reject(callId: String) {
        viewModelScope.launch { voiceRepository.rejectCall(callId) }
    }
}
