package com.truckerload.data.repository.social

/**
 * After a friend link is created or removed, push the change into the
 * account-wide community cache (chats, leaderboard, DMs) instead of leaving
 * it only on the friends map.
 */
class CommunityFriendsPublisher(
    private val chatRepository: ChatRepository,
    private val socialSync: SocialSyncCoordinator,
) {
    suspend fun onFriendshipEstablished(userId: String, displayName: String) {
        if (userId.isBlank()) return
        chatRepository.rememberPeer(userId, displayName)
        runCatching { socialSync.pullRemote() }
        chatRepository.rememberPeerIfAbsent(userId, displayName)
    }

    suspend fun onFriendshipRemoved(userId: String) {
        if (userId.isBlank()) return
        chatRepository.forgetPeer(userId)
        runCatching { socialSync.pullRemote() }
    }
}
