package com.truckerload.presentation.screens.voice

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.preferences.CallPrivacyStore
import com.truckerload.data.repository.VoiceRepository
import com.truckerload.data.repository.social.ChatRepository
import com.truckerload.data.repository.social.ProfileRepository
import com.truckerload.data.voice.CallHistory
import com.truckerload.data.voice.CallNotifications
import com.truckerload.domain.social.getOrNull
import com.truckerload.domain.voice.CallConfig
import com.truckerload.domain.voice.CallPolicy
import com.truckerload.domain.voice.CallState
import com.truckerload.domain.voice.CallStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CallUiState(
    val call: CallState? = null,
    val isMuted: Boolean = false,
    val durationSeconds: Long = 0,
    val audioBitrateKbps: Int = 16,
    val connectionLost: Boolean = false,
    val offerVoiceMessage: Boolean = false,
)

@HiltViewModel
class CallViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val voiceRepository: VoiceRepository,
    private val chatRepository: ChatRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {
    private val callId = Uri.decode(savedStateHandle.get<String>("callId").orEmpty())
    private val _local = MutableStateFlow(CallUiState())
    val uiState: StateFlow<CallUiState> = _local.asStateFlow()
    private var ticker: Job? = null
    private var ringJob: Job? = null
    private var audioStarted = false
    private var recorded = false

    init {
        viewModelScope.launch {
            voiceRepository.watchCall(callId).collect { call ->
                val elapsed = call?.let { System.currentTimeMillis() - it.startedAt } ?: 0L
                _local.value = _local.value.copy(
                    call = call,
                    offerVoiceMessage = call != null && CallPolicy.shouldPromptVoiceMessage(
                        call.status,
                        call.isIncoming,
                        elapsed,
                    ),
                    audioBitrateKbps = voiceRepository.qualityManager.currentSettings().bitrate / 1000,
                    connectionLost = !voiceRepository.qualityManager.hasNetwork(),
                )
                if (call?.status == CallStatus.RINGING) startRingTimeout(call)
                if (call?.status == CallStatus.ACTIVE) {
                    ringJob?.cancel()
                    if (ticker == null) startTicker(call.startedAt)
                    if (!audioStarted) {
                        audioStarted = true
                        voiceRepository.beginCallAudio(
                            viewModelScope,
                            callId,
                            isCaller = !call.isIncoming,
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                runCatching { voiceRepository.pullRemote() }
                voiceRepository.qualityManager.adjustForNetwork()
                val call = _local.value.call
                val elapsed = call?.let { System.currentTimeMillis() - it.startedAt } ?: 0L
                _local.value = _local.value.copy(
                    audioBitrateKbps = voiceRepository.qualityManager.currentSettings().bitrate / 1000,
                    connectionLost = !voiceRepository.qualityManager.hasNetwork(),
                    offerVoiceMessage = call != null && CallPolicy.shouldPromptVoiceMessage(
                        call.status,
                        call.isIncoming,
                        elapsed,
                    ),
                )
            }
        }
    }

    private fun startRingTimeout(call: CallState) {
        if (ringJob?.isActive == true) return
        ringJob = viewModelScope.launch {
            val wait = CallConfig.RING_TIMEOUT_MS - (System.currentTimeMillis() - call.startedAt)
            if (wait > 0) delay(wait)
            val current = _local.value.call ?: return@launch
            if (CallPolicy.shouldAutoMiss(current.status, System.currentTimeMillis() - current.startedAt)) {
                voiceRepository.missCall(callId)
                record(current, CallStatus.MISSED, 0)
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

    fun cancelOutgoing(onDone: () -> Unit) {
        viewModelScope.launch {
            voiceRepository.endCall(callId)
            onDone()
        }
    }

    fun endCall(onEnded: () -> Unit) {
        viewModelScope.launch {
            val current = _local.value.call
            val durationMs = _local.value.durationSeconds * 1000L
            voiceRepository.endCall(callId)
            if (current != null && current.status == CallStatus.ACTIVE) {
                record(current, CallStatus.ENDED, durationMs)
            }
            onEnded()
        }
    }

    fun openPeerChat(onOpened: (String) -> Unit) {
        val peer = _local.value.call?.let(CallPolicy::peerIdFor).orEmpty()
        if (peer.isBlank()) return
        viewModelScope.launch {
            chatRepository.createPrivateChatWithPeer(peer).getOrNull()?.let(onOpened)
        }
    }

    fun redial(onStarted: (String) -> Unit) {
        val call = _local.value.call ?: return
        val peerId = CallPolicy.peerIdFor(call)
        val peerName = CallPolicy.peerNameFor(call)
        if (peerId.isBlank()) return
        viewModelScope.launch {
            val me = profileRepository.watchMyProfile().first().displayName.ifBlank { "Me" }
            voiceRepository.startCall(peerId, peerName, me).getOrNull()?.let { onStarted(it.callId) }
        }
    }

    private suspend fun record(call: CallState, status: CallStatus, durationMs: Long) {
        if (recorded) return
        recorded = true
        runCatching {
            CallHistory.record(chatRepository, profileRepository, call, status, durationMs)
        }
    }

    override fun onCleared() {
        ticker?.cancel()
        ringJob?.cancel()
        super.onCleared()
    }
}

@HiltViewModel
class IncomingCallViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val voiceRepository: VoiceRepository,
    private val chatRepository: ChatRepository,
    private val profileRepository: ProfileRepository,
    private val callPrivacyStore: CallPrivacyStore,
) : ViewModel() {
    private val notifications = CallNotifications(context)
    val incomingCall: StateFlow<CallState?> =
        voiceRepository.watchIncomingCall()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val _offerSwitch = MutableStateFlow(false)
    val offerSwitch: StateFlow<Boolean> = _offerSwitch.asStateFlow()
    private var ringJob: Job? = null
    private var lastIncomingId: String? = null

    init {
        viewModelScope.launch {
            incomingCall.collect { call ->
                _offerSwitch.value = voiceRepository.isBusy()
                if (call == null || call.status != CallStatus.RINGING) {
                    notifications.cancelIncoming()
                    return@collect
                }
                if (!canReceive(call)) {
                    voiceRepository.rejectCall(call.callId)
                    return@collect
                }
                if (lastIncomingId != call.callId) {
                    lastIncomingId = call.callId
                    startRingTimeout(call)
                }
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(3_000)
                runCatching { voiceRepository.pullRemote() }
            }
        }
    }

    private suspend fun canReceive(call: CallState): Boolean {
        val blocked = profileRepository.isBlocked(call.callerId)
        val contact = profileRepository.watchIsFollowing(call.callerId).first()
        return CallPolicy.canReceiveCall(callPrivacyStore.current(), contact, blocked)
    }

    private fun startRingTimeout(call: CallState) {
        ringJob?.cancel()
        ringJob = viewModelScope.launch {
            val wait = CallConfig.RING_TIMEOUT_MS - (System.currentTimeMillis() - call.startedAt)
            if (wait > 0) delay(wait)
            val current = incomingCall.value ?: return@launch
            if (current.callId != call.callId) return@launch
            if (CallPolicy.shouldAutoMiss(current.status, System.currentTimeMillis() - current.startedAt)) {
                voiceRepository.missCall(call.callId)
                runCatching {
                    CallHistory.record(chatRepository, profileRepository, current, CallStatus.MISSED, 0)
                }
                notifications.showMissed(current.callerId, current.callerName)
            }
        }
    }

    fun accept(callId: String, onAccepted: (String) -> Unit) {
        viewModelScope.launch {
            voiceRepository.endActiveIfDifferent(callId)
            voiceRepository.acceptCall(callId).onSuccess {
                notifications.cancelIncoming()
                voiceRepository.beginCallAudio(viewModelScope, callId, isCaller = false)
                onAccepted(callId)
            }
        }
    }

    fun reject(callId: String) {
        viewModelScope.launch {
            val current = incomingCall.value
            voiceRepository.rejectCall(callId)
            notifications.cancelIncoming()
            if (current != null) {
                runCatching {
                    CallHistory.record(chatRepository, profileRepository, current, CallStatus.REJECTED, 0)
                }
            }
        }
    }

    override fun onCleared() {
        ringJob?.cancel()
        super.onCleared()
    }
}
