package com.truckerload.data.community

import org.json.JSONObject

data class RemoteCommunityPeer(
    val userId: String,
    val displayName: String,
    val weeklyMiles: Double,
    val weeklyRevenue: Double,
    val weeklyLoads: Int,
    val weeklyRpm: Double,
)

data class RemoteCommunityChat(
    val id: String,
    val type: String,
    val title: String,
    val inviteCode: String,
    val creatorId: String,
    val category: String,
    val isPublic: Boolean,
    val lastMessage: String,
    val lastMessageAt: Long,
)

data class RemoteCommunityMember(
    val chatId: String,
    val userId: String,
    val displayName: String,
    val role: String,
    val joinedAt: Long,
)

data class RemoteCommunityMessage(
    val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val messageType: String,
    val attachmentUrl: String?,
    val replyToId: String?,
    val locationLabel: String?,
    val durationMs: Long,
    val sentAt: Long,
)

data class RemoteCommunityStatus(
    val id: String,
    val userId: String,
    val displayName: String,
    val type: String,
    val text: String?,
    val mediaPath: String?,
    val durationMs: Long,
    val createdAt: Long,
    val expiresAt: Long,
)

data class RemoteVoiceRoom(
    val id: String,
    val title: String,
    val creatorId: String,
    val isActive: Boolean,
    val createdAt: Long,
)

data class RemoteVoiceParticipant(
    val roomId: String,
    val userId: String,
    val displayName: String,
    val muted: Boolean,
    val deafened: Boolean,
    val joinedAt: Long,
)

data class RemoteCall(
    val id: String,
    val callerId: String,
    val calleeId: String,
    val callerName: String,
    val calleeName: String,
    val status: String,
    val createdAt: Long,
)

internal fun JSONObject.optEpochMillis(key: String): Long {
    if (!has(key) || isNull(key)) return 0L
    return when (val value = opt(key)) {
        is Number -> CommunityTime.parseMillis(value.toLong().toString())
        else -> CommunityTime.parseMillis(value?.toString().orEmpty())
    }
}
