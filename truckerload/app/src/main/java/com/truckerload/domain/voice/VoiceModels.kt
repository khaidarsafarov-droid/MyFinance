package com.truckerload.domain.voice

data class VoiceRoom(
    val id: String,
    val name: String,
    val type: VoiceRoomType,
    val creatorId: String,
    val participants: List<VoiceParticipant>,
    val maxParticipants: Int = 50,
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class VoiceRoomType { PUBLIC, PRIVATE, GROUP }

data class VoiceParticipant(
    val userId: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val isMuted: Boolean = false,
    val isDeafened: Boolean = false,
    val isSpeaking: Boolean = false,
    val joinedAt: Long,
    val audioLevel: Int = 0,
    val isMe: Boolean = false,
)

data class VoiceRoomSettings(
    val bitrate: Int = 64_000,
    val sampleRate: Int = 48_000,
    val echoCancellation: Boolean = true,
    val noiseSuppression: Boolean = true,
    val autoGainControl: Boolean = true,
)

data class CallState(
    val callId: String,
    val type: CallType,
    val status: CallStatus,
    val participants: List<String>,
    val startedAt: Long,
    val durationMs: Long = 0,
    val isIncoming: Boolean,
    val callerId: String,
    val callerName: String,
    val calleeId: String?,
    val calleeName: String?,
)

enum class CallType { P2P, GROUP }

enum class CallStatus { RINGING, ACTIVE, ENDED, MISSED, REJECTED }

data class Signal(
    val type: SignalType,
    val fromUserId: String,
    val sdp: String? = null,
    val candidate: String? = null,
    val sdpMid: String? = null,
    val sdpMLineIndex: Int? = null,
    val timestamp: Long = System.currentTimeMillis(),
)

enum class SignalType {
    OFFER, ANSWER, ICE_CANDIDATE, LEAVE, MUTE, UNMUTE
}
