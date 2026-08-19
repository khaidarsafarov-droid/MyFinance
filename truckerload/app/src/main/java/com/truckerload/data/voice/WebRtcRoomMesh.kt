package com.truckerload.data.voice

import android.content.Context
import android.media.AudioManager
import com.truckerload.domain.voice.Signal
import com.truckerload.domain.voice.SignalType
import com.truckerload.domain.voice.VoiceRoomSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RTCStatsCollectorCallback
import org.webrtc.RTCStatsReport
import org.webrtc.RtpTransceiver
import org.webrtc.SessionDescription
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Mesh of P2P audio connections for a voice room. One [PeerConnection] per remote user,
 * signaled through [SignalingService] using [VoiceMeshSession.pairId].
 */
class WebRtcRoomMesh(
    private val context: Context,
    private val signaling: SignalingService,
    private val localUserId: String,
) {
    private var factory: PeerConnectionFactory? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var scope: CoroutineScope? = null
    private var roomId: String? = null
    private var muted = false
    private var deafened = false
    private val peers = ConcurrentHashMap<String, MeshPeer>()
    private val remoteTracks = CopyOnWriteArrayList<AudioTrack>()
    private val speaking = ConcurrentHashMap<String, Int>()

    fun start(scope: CoroutineScope, roomId: String, settings: VoiceRoomSettings) {
        stop()
        this.scope = scope
        this.roomId = roomId
        factory = WebRtcInitializer.createFactory(context)
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", settings.echoCancellation.toString()))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", settings.noiseSuppression.toString()))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", settings.autoGainControl.toString()))
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
        }
        val source = factory?.createAudioSource(constraints) ?: error("PeerConnectionFactory not ready")
        audioSource = source
        localAudioTrack = factory?.createAudioTrack("tl_room_audio", source)?.apply { setEnabled(true) }
        setSpeakerphone(true)
    }

    fun syncPeers(remoteUserIds: Collection<String>) {
        val room = roomId ?: return
        val wanted = remoteUserIds.filter { it.isNotBlank() && it != localUserId }.toSet()
        peers.keys.filter { it !in wanted }.forEach { closePeer(it) }
        wanted.filter { !peers.containsKey(it) }.forEach { remoteId ->
            connectPeer(room, remoteId)
        }
    }

    fun setMuted(value: Boolean) {
        muted = value
        localAudioTrack?.setEnabled(!value)
        if (value) speaking[localUserId] = 0
    }

    fun setDeafened(value: Boolean) {
        deafened = value
        val volume = if (value) 0.0 else 1.0
        remoteTracks.forEach { track ->
            runCatching { track.setEnabled(!value) }
            runCatching { track.setVolume(volume) }
        }
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
        val target = if (value) 0 else max.coerceAtLeast(1)
        runCatching { audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, target, 0) }
    }

    fun localAudioLevel(): Int = speaking[localUserId] ?: 0

    fun speakingSnapshot(): Map<String, Int> = HashMap(speaking)

    fun pollAudioLevels() {
        val local = localAudioTrack
        if (muted || local?.enabled() != true) {
            speaking[localUserId] = 0
        }
        peers.values.forEach { peer ->
            val pc = peer.connection ?: return@forEach
            runCatching {
                pc.getStats(
                    RTCStatsCollectorCallback { report -> ingestStats(peer.remoteUserId, report) },
                )
            }
        }
        if (!muted) {
            val localPc = peers.values.firstOrNull()?.connection
            localPc?.let { pc ->
                runCatching {
                    pc.getStats(RTCStatsCollectorCallback { report -> ingestLocalStats(report) })
                }
            }
        }
    }

    fun stop() {
        peers.keys.toList().forEach { closePeer(it) }
        peers.clear()
        remoteTracks.clear()
        speaking.clear()
        localAudioTrack?.setEnabled(false)
        localAudioTrack?.dispose()
        localAudioTrack = null
        audioSource?.dispose()
        audioSource = null
        factory?.dispose()
        factory = null
        scope = null
        roomId = null
        muted = false
        deafened = false
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_NORMAL
    }

    private fun connectPeer(roomId: String, remoteUserId: String) {
        if (factory == null || localAudioTrack == null) return
        if (peers.containsKey(remoteUserId)) return
        val sessionId = VoiceMeshSession.pairId(roomId, localUserId, remoteUserId)
        val offerer = VoiceMeshSession.isOfferer(localUserId, remoteUserId)
        val peer = MeshPeer(remoteUserId, sessionId, offerer)
        peers[remoteUserId] = peer
        peer.connection = createPeerConnection(peer)
        localAudioTrack?.let { track ->
            peer.connection?.addTrack(track, listOf("room_stream"))
        }
        observeSignals(peer)
        if (offerer) createOffer(peer)
    }

    private fun createPeerConnection(peer: MeshPeer): PeerConnection? {
        val observer = object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) = Unit
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                if (state == PeerConnection.IceConnectionState.DISCONNECTED ||
                    state == PeerConnection.IceConnectionState.FAILED ||
                    state == PeerConnection.IceConnectionState.CLOSED
                ) {
                    speaking[peer.remoteUserId] = 0
                }
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) = Unit
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate ?: return
                scope?.launch {
                    signaling.sendSignal(
                        peer.sessionId,
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
            override fun onAddStream(stream: org.webrtc.MediaStream?) {
                stream?.audioTracks?.forEach { attachRemoteTrack(it) }
            }
            override fun onRemoveStream(stream: org.webrtc.MediaStream?) = Unit
            override fun onDataChannel(channel: org.webrtc.DataChannel?) = Unit
            override fun onRenegotiationNeeded() = Unit
            override fun onAddTrack(
                receiver: org.webrtc.RtpReceiver?,
                streams: Array<out org.webrtc.MediaStream>?,
            ) {
                (receiver?.track() as? AudioTrack)?.let { attachRemoteTrack(it) }
            }
            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) = Unit
            override fun onTrack(transceiver: RtpTransceiver?) {
                (transceiver?.receiver?.track() as? AudioTrack)?.let { attachRemoteTrack(it) }
            }
        }
        return factory?.createPeerConnection(PeerConnection.RTCConfiguration(IceServers.list()), observer)
    }

    private fun attachRemoteTrack(track: AudioTrack) {
        if (!remoteTracks.contains(track)) remoteTracks.add(track)
        track.setEnabled(!deafened)
        runCatching { track.setVolume(if (deafened) 0.0 else 1.0) }
    }

    private fun observeSignals(peer: MeshPeer) {
        val jobScope = scope ?: return
        peer.signalJob?.cancel()
        peer.signalJob = jobScope.launch {
            signaling.watchSignals(peer.sessionId, localUserId).collect { signals ->
                signals.forEach { signal ->
                    val key = signal.id.ifBlank {
                        "${signal.fromUserId}:${signal.type}:${signal.timestamp}:${signal.sdp}:${signal.candidate}"
                    }
                    if (peer.seenSignals.add(key)) handleSignal(peer, signal)
                }
            }
        }
    }

    private fun handleSignal(peer: MeshPeer, signal: Signal) {
        when (signal.type) {
            SignalType.OFFER -> {
                val sdp = signal.sdp ?: return
                peer.connection?.setRemoteDescription(
                    object : SimpleSdpObserver() {
                        override fun onSetSuccess() {
                            peer.remoteReady = true
                            drainIce(peer)
                            createAnswer(peer)
                        }
                    },
                    SessionDescription(SessionDescription.Type.OFFER, sdp),
                )
            }
            SignalType.ANSWER -> {
                val sdp = signal.sdp ?: return
                peer.connection?.setRemoteDescription(
                    object : SimpleSdpObserver() {
                        override fun onSetSuccess() {
                            peer.remoteReady = true
                            drainIce(peer)
                        }
                    },
                    SessionDescription(SessionDescription.Type.ANSWER, sdp),
                )
            }
            SignalType.ICE_CANDIDATE -> {
                val candidate = signal.candidate ?: return
                val ice = IceCandidate(signal.sdpMid, signal.sdpMLineIndex ?: 0, candidate)
                if (peer.remoteReady) {
                    peer.connection?.addIceCandidate(ice)
                } else {
                    peer.pendingIce.add(ice)
                }
            }
            SignalType.LEAVE -> closePeer(peer.remoteUserId)
            else -> Unit
        }
    }

    private fun createOffer(peer: MeshPeer) {
        val constraints = receiveAudioConstraints()
        peer.connection?.createOffer(
            object : SimpleSdpObserver() {
                override fun onCreateSuccess(description: SessionDescription?) {
                    description ?: return
                    peer.connection?.setLocalDescription(SimpleSdpObserver(), description)
                    scope?.launch {
                        signaling.sendSignal(
                            peer.sessionId,
                            Signal(
                                type = SignalType.OFFER,
                                fromUserId = localUserId,
                                sdp = description.description,
                            ),
                        )
                    }
                }
            },
            constraints,
        )
    }

    private fun createAnswer(peer: MeshPeer) {
        val constraints = receiveAudioConstraints()
        peer.connection?.createAnswer(
            object : SimpleSdpObserver() {
                override fun onCreateSuccess(description: SessionDescription?) {
                    description ?: return
                    peer.connection?.setLocalDescription(SimpleSdpObserver(), description)
                    scope?.launch {
                        signaling.sendSignal(
                            peer.sessionId,
                            Signal(
                                type = SignalType.ANSWER,
                                fromUserId = localUserId,
                                sdp = description.description,
                            ),
                        )
                    }
                }
            },
            constraints,
        )
    }

    private fun drainIce(peer: MeshPeer) {
        peer.pendingIce.forEach { peer.connection?.addIceCandidate(it) }
        peer.pendingIce.clear()
    }

    private fun closePeer(remoteUserId: String) {
        val peer = peers.remove(remoteUserId) ?: return
        peer.signalJob?.cancel()
        peer.connection?.close()
        peer.connection?.dispose()
        speaking.remove(remoteUserId)
    }

    private fun ingestStats(remoteUserId: String, report: RTCStatsReport?) {
        val level = audioLevelFrom(report, inbound = true)
        if (level != null) speaking[remoteUserId] = level
    }

    private fun ingestLocalStats(report: RTCStatsReport?) {
        if (muted) {
            speaking[localUserId] = 0
            return
        }
        val level = audioLevelFrom(report, inbound = false)
        if (level != null) speaking[localUserId] = level
    }

    private fun audioLevelFrom(report: RTCStatsReport?, inbound: Boolean): Int? {
        report ?: return null
        var best: Double? = null
        report.statsMap.values.forEach { stats ->
            val type = stats.type.orEmpty()
            val members = stats.members
            val candidate = numberMember(members, "audioLevel")
                ?: numberMember(members, "audioOutputLevel")
                ?: numberMember(members, "audioInputLevel")
            if (candidate != null) {
                val prefer = when {
                    inbound && (type == "inbound-rtp" || type == "track") -> true
                    !inbound && (type == "media-source" || type == "outbound-rtp" || type == "track") -> true
                    else -> false
                }
                if (prefer) best = maxOf(best ?: 0.0, candidate)
            }
        }
        val raw = best ?: return null
        val scaled = if (raw <= 1.0) raw * 100.0 else raw
        return scaled.toInt().coerceIn(0, 100)
    }

    private fun numberMember(members: Map<String, Any>, key: String): Double? {
        val value = members[key] ?: return null
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        }
    }

    private fun setSpeakerphone(enabled: Boolean) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = enabled
    }

    private fun receiveAudioConstraints(): MediaConstraints =
        MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }

    private class MeshPeer(
        val remoteUserId: String,
        val sessionId: String,
        val isOfferer: Boolean,
    ) {
        var connection: PeerConnection? = null
        var signalJob: Job? = null
        var remoteReady: Boolean = false
        val pendingIce = mutableListOf<IceCandidate>()
        val seenSignals = mutableSetOf<String>()
    }

    private open class SimpleSdpObserver : org.webrtc.SdpObserver {
        override fun onCreateSuccess(description: SessionDescription?) = Unit
        override fun onSetSuccess() = Unit
        override fun onCreateFailure(error: String?) = Unit
        override fun onSetFailure(error: String?) = Unit
    }
}
