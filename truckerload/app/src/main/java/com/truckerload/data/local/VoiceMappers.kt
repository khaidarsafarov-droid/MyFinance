package com.truckerload.data.local

import com.truckerload.data.local.entities.CallSessionEntity
import com.truckerload.data.local.entities.VoiceRoomEntity
import com.truckerload.data.local.entities.VoiceRoomParticipantEntity
import com.truckerload.domain.voice.CallState
import com.truckerload.domain.voice.CallStatus
import com.truckerload.domain.voice.CallType
import com.truckerload.domain.voice.VoiceParticipant
import com.truckerload.domain.voice.VoiceRoom
import com.truckerload.domain.voice.VoiceRoomType

internal fun VoiceRoomEntity.toDomain(participants: List<VoiceParticipant>) = VoiceRoom(
    id = id,
    name = name,
    type = runCatching { VoiceRoomType.valueOf(type) }.getOrDefault(VoiceRoomType.PUBLIC),
    creatorId = creatorId,
    participants = participants,
    maxParticipants = maxParticipants,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
    description = description,
    moderatorId = moderatorId,
)

internal fun VoiceRoomParticipantEntity.toDomain(isMe: Boolean) = VoiceParticipant(
    userId = userId,
    displayName = displayName,
    isMuted = isMuted,
    isDeafened = isDeafened,
    isSpeaking = isSpeaking,
    audioLevel = audioLevel,
    joinedAt = joinedAt,
    isMe = isMe,
)

internal fun CallSessionEntity.toDomain() = CallState(
    callId = callId,
    type = runCatching { CallType.valueOf(type) }.getOrDefault(CallType.P2P),
    status = runCatching { CallStatus.valueOf(status) }.getOrDefault(CallStatus.ENDED),
    participants = listOfNotNull(callerId, calleeId),
    startedAt = startedAt,
    durationMs = durationMs,
    isIncoming = isIncoming,
    callerId = callerId,
    callerName = callerName,
    calleeId = calleeId,
    calleeName = calleeName,
)
