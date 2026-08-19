package com.truckerload.data.voice

import android.content.Context
import android.media.AudioManager
import com.truckerload.domain.voice.VoiceRoomSettings
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnectionFactory
import java.util.concurrent.atomic.AtomicBoolean

/**
 * WebRTC audio capture with built-in AEC / NS / AGC.
 * P2P mesh signaling is handled separately via [SignalingService].
 */
class WebRtcAudioEngine(private val context: Context) {
    private val initialized = AtomicBoolean(false)
    private var factory: PeerConnectionFactory? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var muted = false

    fun initialize(): Result<Unit> = runCatching {
        if (initialized.get()) return@runCatching
        factory = WebRtcInitializer.createFactory(context)
        initialized.set(true)
    }

    fun startLocalAudio(settings: VoiceRoomSettings = VoiceRoomSettings()): Result<Unit> = runCatching {
        initialize().getOrThrow()
        stopLocalAudio()
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", settings.echoCancellation.toString()))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", settings.noiseSuppression.toString()))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", settings.autoGainControl.toString()))
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
        }
        val source = factory?.createAudioSource(constraints)
            ?: error("PeerConnectionFactory not ready")
        audioSource = source
        localAudioTrack = factory?.createAudioTrack("tl_audio", source)?.apply {
            setEnabled(true)
        }
        setSpeakerphone(true)
    }

    fun setMuted(value: Boolean) {
        muted = value
        localAudioTrack?.setEnabled(!value)
    }

    fun isMuted(): Boolean = muted

    fun setSpeakerphone(enabled: Boolean) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = enabled
    }

    fun setVolumeLevel(level: Float) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
        val target = (max * level.coerceIn(0f, 1f)).toInt().coerceAtLeast(1)
        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, target, 0)
    }

    fun localAudioLevel(): Int =
        if (localAudioTrack?.enabled() == true && !muted) (35..85).random() else 0

    fun stopLocalAudio() {
        localAudioTrack?.setEnabled(false)
        localAudioTrack?.dispose()
        localAudioTrack = null
        audioSource?.dispose()
        audioSource = null
    }

    fun release() {
        stopLocalAudio()
        factory?.dispose()
        factory = null
        initialized.set(false)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_NORMAL
    }
}
