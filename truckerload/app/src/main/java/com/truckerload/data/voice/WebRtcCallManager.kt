package com.truckerload.data.voice

import android.content.Context
import com.truckerload.domain.voice.Signal
import com.truckerload.domain.voice.SignalType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SessionDescription
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Minimal P2P audio call manager using local Room signaling.
 * Creates real SDP offers/answers and exchanges ICE candidates.
 */
class WebRtcCallManager(
    private val context: Context,
    private val signaling: SignalingService,
    private val localUserId: String,
) {
    private val initialized = AtomicBoolean(false)
    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var signalJob: Job? = null
    private var callScope: CoroutineScope? = null
    private var activeSessionId: String? = null

    fun initialize(): Result<Unit> = runCatching {
        if (initialized.get()) return@runCatching
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                .setEnableInternalTracer(false)
                .createInitializationOptions(),
        )
        factory = PeerConnectionFactory.builder().createPeerConnectionFactory()
        initialized.set(true)
    }

    fun startAsCaller(
        scope: CoroutineScope,
        sessionId: String,
        onConnected: () -> Unit = {},
    ): Result<Unit> = runCatching {
        initialize().getOrThrow()
        releasePeerConnection()
        activeSessionId = sessionId
        callScope = scope
        setupLocalAudio()
        createPeerConnection(sessionId, isCaller = true, onConnected)
        observeSignals(scope, sessionId)
        createOffer()
    }

    fun startAsCallee(
        scope: CoroutineScope,
        sessionId: String,
        onConnected: () -> Unit = {},
    ): Result<Unit> = runCatching {
        initialize().getOrThrow()
        releasePeerConnection()
        activeSessionId = sessionId
        callScope = scope
        setupLocalAudio()
        createPeerConnection(sessionId, isCaller = false, onConnected)
        observeSignals(scope, sessionId)
    }

    fun setMuted(muted: Boolean) {
        localAudioTrack?.setEnabled(!muted)
    }

    fun release() {
        signalJob?.cancel()
        signalJob = null
        callScope = null
        releasePeerConnection()
        localAudioTrack?.dispose()
        localAudioTrack = null
        audioSource?.dispose()
        audioSource = null
        factory?.dispose()
        factory = null
        initialized.set(false)
        activeSessionId = null
    }

    private fun setupLocalAudio() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
        }
        val source = factory?.createAudioSource(constraints) ?: error("Factory not ready")
        audioSource = source
        localAudioTrack = factory?.createAudioTrack("tl_call_audio", source)?.apply { setEnabled(true) }
    }

    private fun createPeerConnection(sessionId: String, isCaller: Boolean, onConnected: () -> Unit) {
        val iceServers = IceServers.list()
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        val observer = object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) = Unit
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                if (state == PeerConnection.IceConnectionState.CONNECTED ||
                    state == PeerConnection.IceConnectionState.COMPLETED
                ) {
                    onConnected()
                }
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) = Unit
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate ?: return
                callScope?.launch {
                    signaling.sendSignal(
                        sessionId,
                        Signal(
                            type = SignalType.ICE_CANDIDATE,
                            fromUserId = localUserId,
                            candidate = candidate.sdp,
                            sdpMid = candidate.sdpMid,
                            sdpMLineIndex = candidate.sdpMLineIndex,
                        ),
                    )
                }
            }
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) = Unit
            override fun onAddStream(stream: org.webrtc.MediaStream?) = Unit
            override fun onRemoveStream(stream: org.webrtc.MediaStream?) = Unit
            override fun onDataChannel(channel: org.webrtc.DataChannel?) = Unit
            override fun onRenegotiationNeeded() = Unit
            override fun onAddTrack(receiver: org.webrtc.RtpReceiver?, streams: Array<out org.webrtc.MediaStream>?) = Unit
            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) = Unit
            override fun onTrack(transceiver: org.webrtc.RtpTransceiver?) = Unit
        }
        peerConnection = factory?.createPeerConnection(rtcConfig, observer)
        localAudioTrack?.let { track ->
            peerConnection?.addTrack(track, listOf("call_stream"))
        }
    }

    private fun createOffer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }
        peerConnection?.createOffer(object : org.webrtc.SdpObserver {
            override fun onCreateSuccess(description: SessionDescription?) {
                description ?: return
                peerConnection?.setLocalDescription(SimpleSdpObserver(), description)
                val sessionId = activeSessionId ?: return
                callScope?.launch {
                    signaling.sendSignal(
                        sessionId,
                        Signal(
                            type = SignalType.OFFER,
                            fromUserId = localUserId,
                            sdp = description.description,
                        ),
                    )
                }
            }
            override fun onSetSuccess() = Unit
            override fun onCreateFailure(error: String?) = Unit
            override fun onSetFailure(error: String?) = Unit
        }, constraints)
    }

    private fun createAnswer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }
        peerConnection?.createAnswer(object : org.webrtc.SdpObserver {
            override fun onCreateSuccess(description: SessionDescription?) {
                description ?: return
                peerConnection?.setLocalDescription(SimpleSdpObserver(), description)
                val sessionId = activeSessionId ?: return
                callScope?.launch {
                    signaling.sendSignal(
                        sessionId,
                        Signal(
                            type = SignalType.ANSWER,
                            fromUserId = localUserId,
                            sdp = description.description,
                        ),
                    )
                }
            }
            override fun onSetSuccess() = Unit
            override fun onCreateFailure(error: String?) = Unit
            override fun onSetFailure(error: String?) = Unit
        }, constraints)
    }

    private fun observeSignals(scope: CoroutineScope, sessionId: String) {
        signalJob?.cancel()
        signalJob = scope.launch {
            signaling.watchSignals(sessionId, localUserId).collectLatest { signals ->
                signals.forEach { handleSignal(it) }
            }
        }
    }

    private fun handleSignal(signal: Signal) {
        when (signal.type) {
            SignalType.OFFER -> {
                val sdp = signal.sdp ?: return
                peerConnection?.setRemoteDescription(
                    SimpleSdpObserver(),
                    SessionDescription(SessionDescription.Type.OFFER, sdp),
                )
                createAnswer()
            }
            SignalType.ANSWER -> {
                val sdp = signal.sdp ?: return
                peerConnection?.setRemoteDescription(
                    SimpleSdpObserver(),
                    SessionDescription(SessionDescription.Type.ANSWER, sdp),
                )
            }
            SignalType.ICE_CANDIDATE -> {
                val candidate = signal.candidate ?: return
                peerConnection?.addIceCandidate(
                    IceCandidate(signal.sdpMid, signal.sdpMLineIndex ?: 0, candidate),
                )
            }
            else -> Unit
        }
    }

    private fun releasePeerConnection() {
        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null
    }

    private class SimpleSdpObserver : org.webrtc.SdpObserver {
        override fun onCreateSuccess(sessionDescription: SessionDescription?) = Unit
        override fun onSetSuccess() = Unit
        override fun onCreateFailure(error: String?) = Unit
        override fun onSetFailure(error: String?) = Unit
    }
}
