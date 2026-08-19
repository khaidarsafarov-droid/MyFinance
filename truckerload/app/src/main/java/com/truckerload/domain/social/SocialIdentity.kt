package com.truckerload.domain.social

object SocialIdentity {
    const val LOCAL_PROFILE_ID = "local_user"
    const val LEGACY_SENDER_ID = "me"

    private val UUID_REGEX =
        Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    fun isUuid(id: String): Boolean = UUID_REGEX.matches(id.trim())

    fun actorId(sessionUserId: String?): String {
        val id = sessionUserId?.trim().orEmpty()
        return if (isUuid(id)) id else LOCAL_PROFILE_ID
    }

    fun isMine(senderId: String, actorId: String): Boolean {
        if (senderId.isBlank()) return false
        if (senderId == actorId) return true
        return senderId == LEGACY_SENDER_ID || senderId == LOCAL_PROFILE_ID
    }

    fun privateChatIdForPeer(peerId: String): String = "dm_$peerId"

    fun peerIdFromPrivateChat(chatId: String): String? =
        chatId.takeIf { it.startsWith("dm_") && it.length > 3 }?.removePrefix("dm_")
}
