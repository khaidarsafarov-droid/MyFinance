package com.truckerload.presentation.screens.social.friends

import com.truckerload.domain.friends.FriendProfileHit
import com.truckerload.domain.friends.FriendRequest
import com.truckerload.domain.friends.FriendShareLink
import com.truckerload.domain.friends.friendCommunityLabel
import com.truckerload.domain.social.SocialPeerProfile

data class FriendsDirectoryUiState(
    val supabaseReady: Boolean = false,
    val searchQuery: String = "",
    val searchHit: FriendProfileHit? = null,
    val searchNotFound: Boolean = false,
    val searchBusy: Boolean = false,
    val addBusy: Boolean = false,
    val incomingRequests: List<FriendRequest> = emptyList(),
    val outgoingRequests: List<FriendRequest> = emptyList(),
    val shareLinks: List<FriendShareLink> = emptyList(),
    val communityPeers: List<SocialPeerProfile> = emptyList(),
    val statusMessage: String? = null,
    val errorMessage: String? = null,
) {
    val acceptedFriends: List<Pair<String, String>>
        get() {
            if (shareLinks.isNotEmpty()) {
                return shareLinks.map { link ->
                    link.friendUserId to friendCommunityLabel(
                        link.friendNickname,
                        link.friendDisplayName,
                    )
                }
            }
            return communityPeers.map { it.id to it.displayName }
        }
}
