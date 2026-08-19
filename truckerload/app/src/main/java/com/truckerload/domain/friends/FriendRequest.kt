package com.truckerload.domain.friends

enum class FriendRequestDirection {
    INCOMING,
    OUTGOING,
}

data class FriendRequest(
    val id: String,
    val peerId: String,
    val peerNickname: String,
    val direction: FriendRequestDirection,
    val createdAtMillis: Long,
)

enum class FriendRequestSendResult {
    SENT,
    ALREADY_SENT,
    ALREADY_FRIENDS,
    ACCEPTED,
    BLOCKED,
    ADDED_DIRECT,
}

object FriendRequestSendResults {
    fun fromRpc(raw: String): FriendRequestSendResult =
        when (raw.trim().lowercase().trim('"')) {
            "sent" -> FriendRequestSendResult.SENT
            "already_sent" -> FriendRequestSendResult.ALREADY_SENT
            "already_friends" -> FriendRequestSendResult.ALREADY_FRIENDS
            "accepted" -> FriendRequestSendResult.ACCEPTED
            "blocked" -> FriendRequestSendResult.BLOCKED
            else -> FriendRequestSendResult.SENT
        }
}
