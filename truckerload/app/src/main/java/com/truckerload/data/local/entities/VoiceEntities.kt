package com.truckerload.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "voice_rooms",
    indices = [Index(value = ["isActive", "updatedAt"])],
)
data class VoiceRoomEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val creatorId: String,
    val maxParticipants: Int,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "voice_room_participants",
    primaryKeys = ["roomId", "userId"],
    indices = [Index(value = ["roomId"])],
)
data class VoiceRoomParticipantEntity(
    val roomId: String,
    val userId: String,
    val displayName: String,
    val isMuted: Boolean,
    val isDeafened: Boolean,
    val isSpeaking: Boolean,
    val audioLevel: Int,
    val joinedAt: Long,
)

@Entity(
    tableName = "call_sessions",
    indices = [
        Index(value = ["status"]),
        Index(value = ["startedAt"]),
        Index(value = ["status", "isIncoming", "startedAt"]),
    ],
)
data class CallSessionEntity(
    @PrimaryKey val callId: String,
    val type: String,
    val status: String,
    val callerId: String,
    val callerName: String,
    val calleeId: String?,
    val calleeName: String?,
    val isIncoming: Boolean,
    val startedAt: Long,
    val endedAt: Long?,
    val durationMs: Long,
)

@Entity(
    tableName = "voice_signals",
    indices = [Index(value = ["sessionId", "timestamp"])],
)
data class VoiceSignalEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val fromUserId: String,
    val type: String,
    val sdp: String?,
    val candidate: String?,
    val sdpMid: String?,
    val sdpMLineIndex: Int?,
    val timestamp: Long,
)
