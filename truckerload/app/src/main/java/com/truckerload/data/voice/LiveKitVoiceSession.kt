package com.truckerload.data.voice

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.truckerload.contract.VoiceTokenResponse
import com.truckerload.domain.voice.VoiceAudioBudget
import com.truckerload.domain.voice.VoiceRoomRole
import com.truckerload.domain.voice.VoiceRoomSettings
import io.livekit.android.LiveKit
import io.livekit.android.RoomOptions
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.participant.AudioTrackPublishDefaults
import io.livekit.android.room.track.LocalAudioTrackOptions
import io.livekit.android.room.track.RemoteAudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * LiveKit SFU session for a community voice room. One uplink (Opus speech) when
 * the local role is speaker; listeners only subscribe.
 */
class LiveKitVoiceSession(
    private val context: Context,
) {
    private val speaking = ConcurrentHashMap<String, Int>()
    private var room: Room? = null
    private var eventsJob: Job? = null
    private var muted = false
    private var deafened = false
    private var role = VoiceRoomRole.SPEAKER
    private var localUserId: String = ""

    fun speakingSnapshot(): Map<String, Int> = HashMap(speaking)

    fun pollAudioLevels() {
        val current = room ?: return
        val local = current.localParticipant
        if (muted || role == VoiceRoomRole.LISTENER) {
            speaking[localUserId] = 0
        } else {
            speaking[localUserId] = (local.audioLevel * 100f).toInt().coerceIn(0, 100)
        }
    }

    suspend fun connect(
        scope: CoroutineScope,
        credentials: VoiceTokenResponse,
        settings: VoiceRoomSettings,
        role: VoiceRoomRole,
        muted: Boolean,
    ) {
        stop()
        this.role = role
        this.muted = muted || role == VoiceRoomRole.LISTENER
        this.localUserId = credentials.identity
        val created = LiveKit.create(
            appContext = context.applicationContext,
            options = RoomOptions(
                adaptiveStream = true,
                audioTrackCaptureDefaults = LocalAudioTrackOptions(
                    noiseSuppression = settings.noiseSuppression,
                    echoCancellation = settings.echoCancellation,
                    autoGainControl = settings.autoGainControl,
                    highPassFilter = true,
                    typingNoiseDetection = true,
                ),
                audioTrackPublishDefaults = AudioTrackPublishDefaults(
                    audioBitrate = settings.bitrate.coerceIn(
                        VoiceAudioBudget.TELEPHONE_BPS,
                        VoiceAudioBudget.SPEECH_BPS,
                    ),
                    dtx = true,
                    red = true,
                ),
            ),
        )
        room = created
        eventsJob = scope.launch {
            created.events.collect { event -> handleEvent(event) }
        }
        setSpeakerphone(true)
        created.connect(credentials.url, credentials.token)
        val shouldPublish = this.role == VoiceRoomRole.SPEAKER && !this.muted
        created.localParticipant.setMicrophoneEnabled(shouldPublish)
        applyDeafen(created)
        Log.i(TAG, "connected room=${credentials.roomName} role=$role bitrate=${settings.bitrate}")
    }

    suspend fun setMuted(value: Boolean) {
        muted = value || role == VoiceRoomRole.LISTENER
        if (muted) speaking[localUserId] = 0
        room?.localParticipant?.setMicrophoneEnabled(!muted && role == VoiceRoomRole.SPEAKER)
    }

    fun setDeafened(value: Boolean) {
        deafened = value
        room?.setSpeakerMute(value)
        room?.let { applyDeafen(it) }
    }

    fun stop() {
        eventsJob?.cancel()
        eventsJob = null
        speaking.clear()
        val current = room
        room = null
        muted = false
        deafened = false
        role = VoiceRoomRole.SPEAKER
        localUserId = ""
        if (current != null) {
            runCatching { current.disconnect() }
            runCatching { current.release() }
        }
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_NORMAL
    }

    private fun handleEvent(event: RoomEvent) {
        when (event) {
            is RoomEvent.ActiveSpeakersChanged -> {
                val activeIds = event.speakers.map { participantId(it) }.toSet()
                speaking.keys.filter { it !in activeIds && it != localUserId }.forEach { speaking[it] = 0 }
                event.speakers.forEach { participant ->
                    speaking[participantId(participant)] = (participant.audioLevel * 100f).toInt().coerceIn(0, 100)
                }
            }
            is RoomEvent.TrackSubscribed -> {
                val track = event.track
                if (track is RemoteAudioTrack) {
                    track.enabled = !deafened
                    runCatching { track.setVolume(if (deafened) 0.0 else 1.0) }
                }
            }
            is RoomEvent.Disconnected -> speaking.clear()
            else -> Unit
        }
    }

    private fun applyDeafen(current: Room) {
        current.remoteParticipants.values.forEach { participant ->
            participant.audioTrackPublications.forEach { (_, track) ->
                val audio = track as? RemoteAudioTrack ?: return@forEach
                audio.enabled = !deafened
                runCatching { audio.setVolume(if (deafened) 0.0 else 1.0) }
            }
        }
    }

    private fun participantId(participant: io.livekit.android.room.participant.Participant): String {
        val identity = participant.identity?.value.orEmpty()
        if (identity.isNotBlank()) return identity
        return participant.sid.toString()
    }

    private fun setSpeakerphone(enabled: Boolean) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = enabled
    }

    companion object {
        private const val TAG = "LiveKitVoice"
    }
}
