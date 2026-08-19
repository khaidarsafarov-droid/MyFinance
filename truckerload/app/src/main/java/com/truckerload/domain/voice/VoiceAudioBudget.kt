package com.truckerload.domain.voice

enum class VoiceRoomRole {
    SPEAKER,
    LISTENER,
    ;

    fun apiValue(): String = name.lowercase()

    companion object {
        fun fromApi(value: String): VoiceRoomRole =
            if (value.equals("listener", ignoreCase = true)) LISTENER else SPEAKER
    }
}

enum class VoiceTransportKind {
    NONE,
    LIVEKIT,
    MESH,
}

/**
 * Opus speech budgets for highway cellular. Speakers stay in 12–20 kbps
 * (well under 1 MB/min). Listeners do not publish, so uplink is signaling only.
 */
object VoiceAudioBudget {
    const val SPEECH_BPS = 20_000
    const val HIGHWAY_BPS = 16_000
    const val TELEPHONE_BPS = 12_000

    fun bitrateForEstimatedKbps(downloadKbps: Int): Int = when {
        downloadKbps >= 500 -> SPEECH_BPS
        downloadKbps >= 100 -> HIGHWAY_BPS
        else -> TELEPHONE_BPS
    }
}
