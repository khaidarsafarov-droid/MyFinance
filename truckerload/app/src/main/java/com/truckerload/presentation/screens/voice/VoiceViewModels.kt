package com.truckerload.presentation.screens.voice

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.SocialRepository
import com.truckerload.data.repository.VoiceRepository
import com.truckerload.domain.voice.CallState
import com.truckerload.domain.voice.CallStatus
import com.truckerload.domain.voice.VoiceRoom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class VoiceRoomsUiState(
    val rooms: List<VoiceRoom> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

class VoiceRoomsViewModel(
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
        }
        viewModelScope.launch {
            voiceRepository.watchRooms()
                .catch { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = error.toUiMessage())
                }
                .collect { rooms ->
                    _uiState.value = VoiceRoomsUiState(rooms = rooms, isLoading = false)
                }
        }
    }

    fun createRoom(name: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            voiceRepository.createRoom(name)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(errorMessage = null)
                    onCreated(it)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(errorMessage = error.toUiMessage())
                }
        }
    }

    class Factory(private val voiceRepository: VoiceRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            VoiceRoomsViewModel(voiceRepository) as T
    }
}

data class VoiceRoomUiState(
    val room: VoiceRoom? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isMuted: Boolean = false,
    val isDeafened: Boolean = false,
    val durationSeconds: Long = 0,
    val audioBitrate: Int = 64_000,
)

class VoiceRoomViewModel(
    private val roomId: String,
    private val voiceRepository: VoiceRepository,
    private val socialRepository: SocialRepository,
) : ViewModel() {
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
                socialRepository.watchMyEnhancedProfile(),
                voiceRepository.watchRoom(roomId, ""),
            ) { profile, room ->
                profile.displayName to room
            }.catch { error ->
                _local.value = _local.value.copy(isLoading = false, errorMessage = error.toUiMessage())
            }.collect { (displayName, room) ->
                if (!joined) {
                    val joinResult = voiceRepository.joinRoom(roomId, displayName)
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
                _local.value = _local.value.copy(
                    room = room,
                    isLoading = false,
                    errorMessage = null,
                    audioBitrate = voiceRepository.qualityManager.currentSettings().bitrate,
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
                voiceRepository.updateSpeakingLevel(roomId)
                delay(500)
            }
        }
    }

    fun toggleMute() {
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

    override fun onCleared() {
        ticker?.cancel()
        levelJob?.cancel()
        val shouldLeave = joined
        val id = roomId
        val repo = voiceRepository
        if (shouldLeave) {
            // Process-scoped leave so it outlives viewModelScope cancellation without an orphan root Job.
            ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.IO) {
                withContext(NonCancellable) {
                    runCatching { repo.leaveRoom(id) }
                }
            }
        }
        super.onCleared()
    }

    class Factory(
        private val roomId: String,
        private val voiceRepository: VoiceRepository,
        private val socialRepository: SocialRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            VoiceRoomViewModel(roomId, voiceRepository, socialRepository) as T
    }
}

private fun Throwable.toUiMessage(): String =
    localizedMessage ?: message ?: javaClass.simpleName

data class CallUiState(
    val call: CallState? = null,
    val isMuted: Boolean = false,
    val durationSeconds: Long = 0,
)

class CallViewModel(
    private val callId: String,
    private val voiceRepository: VoiceRepository,
) : ViewModel() {
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

    class Factory(
        private val callId: String,
        private val voiceRepository: VoiceRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CallViewModel(callId, voiceRepository) as T
    }
}

class IncomingCallViewModel(
    private val voiceRepository: VoiceRepository,
) : ViewModel() {
    val incomingCall: StateFlow<CallState?> =
        voiceRepository.watchIncomingCall()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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

    fun simulateDemoCall() {
        viewModelScope.launch { voiceRepository.simulateIncomingCall() }
    }

    class Factory(private val voiceRepository: VoiceRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            IncomingCallViewModel(voiceRepository) as T
    }
}
