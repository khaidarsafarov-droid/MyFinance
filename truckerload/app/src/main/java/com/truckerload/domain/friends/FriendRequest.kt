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

/** True when the peer is now a mutual friend and should appear across the account. */
fun FriendRequestSendResult.establishesFriendship(): Boolean = when (this) {
    FriendRequestSendResult.ADDED_DIRECT,
    FriendRequestSendResult.ACCEPTED,
    FriendRequestSendResult.ALREADY_FRIENDS,
    -> true
    FriendRequestSendResult.SENT,
    FriendRequestSendResult.ALREADY_SENT,
    FriendRequestSendResult.BLOCKED,
    -> false
}

fun FriendRequestSendResult.statusKey(): String = when (this) {
    FriendRequestSendResult.SENT -> "request_sent"
    FriendRequestSendResult.ALREADY_SENT -> "already_sent"
    FriendRequestSendResult.ALREADY_FRIENDS -> "already_friends"
    FriendRequestSendResult.ACCEPTED -> "accepted"
    FriendRequestSendResult.BLOCKED -> "blocked"
    FriendRequestSendResult.ADDED_DIRECT -> "added"
}

fun friendCommunityLabel(nickname: String, displayName: String = ""): String {
    val nick = nickname.trim().trimStart('@')
    if (nick.isNotBlank()) return "@$nick"
    return displayName.trim().ifBlank { "Driver" }
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
