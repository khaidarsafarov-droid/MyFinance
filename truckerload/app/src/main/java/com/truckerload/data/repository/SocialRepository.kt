package com.truckerload.data.repository

import android.graphics.Bitmap
import com.truckerload.data.repository.social.ChatRepository
import com.truckerload.data.repository.social.GroupRepository
import com.truckerload.data.repository.social.MediaRepository
import com.truckerload.data.repository.social.ProfileRepository
import com.truckerload.data.repository.social.SocialConstants
import com.truckerload.data.repository.social.SocialSyncCoordinator
import com.truckerload.data.repository.social.StatusRepository
import com.truckerload.di.UserScope
import com.truckerload.domain.social.Challenge
import com.truckerload.domain.social.ChatMember
import com.truckerload.domain.social.DriverProfile
import com.truckerload.domain.social.DriverStatus
import com.truckerload.domain.social.DriverStatusPost
import com.truckerload.domain.social.EnhancedDriverProfile
import com.truckerload.domain.social.LeaderboardCategory
import com.truckerload.domain.social.LeaderboardEntry
import com.truckerload.domain.social.MessageType
import com.truckerload.domain.social.SocialChat
import com.truckerload.domain.social.SocialMessage
import com.truckerload.domain.social.SocialPeerProfile
import com.truckerload.domain.social.SocialResult
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Deprecated facade over domain-specific social repositories.
 * Existing ViewModels keep using this type until Phase 3 follow-up PRs migrate call sites.
 */
@Deprecated(
    message = "Use specific repositories in com.truckerload.data.repository.social. Will be removed in Phase 3.6",
    replaceWith = ReplaceWith("ProfileRepository"),
)
@UserScope
class SocialRepository internal constructor(
    private val profile: ProfileRepository,
    private val chat: ChatRepository,
    private val group: GroupRepository,
    private val status: StatusRepository,
    private val media: MediaRepository,
    private val syncCoordinator: SocialSyncCoordinator,
) {
    suspend fun ensureInitialized() = syncCoordinator.ensureInitialized()

    suspend fun syncIdentityFromUserProfile() = profile.syncIdentityFromUserProfile()

    suspend fun needsProfileSetup(): Boolean = profile.needsProfileSetup()

    suspend fun completeProfileSetup(
        displayName: String,
        phoneNumber: String,
        homeCountryIso2: String,
        truckType: String = "",
        dateOfBirthEpochDay: Long? = null,
        licenseClass: String = "",
        cdlNumber: String = "",
        axleCount: Int = 0,
        homeHubCity: String = "",
    ): SocialResult<Unit> = profile.completeProfileSetup(
        displayName,
        phoneNumber,
        homeCountryIso2,
        truckType,
        dateOfBirthEpochDay,
        licenseClass,
        cdlNumber,
        axleCount,
        homeHubCity,
    )

    suspend fun clearLocalIdentity() = profile.clearLocalIdentity()

    fun watchMyEnhancedProfile(): Flow<EnhancedDriverProfile> = profile.watchMyEnhancedProfile()

    fun watchMyProfile(): Flow<DriverProfile> = profile.watchMyProfile()

    suspend fun updateProfile(
        displayName: String,
        truckType: String,
        experienceYears: Int,
        homeState: String,
        routes: List<String>,
        about: String,
        status: DriverStatus,
        licenseClass: String = "",
        endorsements: List<String> = emptyList(),
        specialties: List<String> = emptyList(),
        phoneNumber: String? = null,
        telegramUsername: String? = null,
        whatsappNumber: String? = null,
        maxRadius: Int = 500,
    ): SocialResult<Unit> = profile.updateProfile(
        displayName,
        truckType,
        experienceYears,
        homeState,
        routes,
        about,
        status,
        licenseClass,
        endorsements,
        specialties,
        phoneNumber,
        telegramUsername,
        whatsappNumber,
        maxRadius,
    )

    suspend fun updateStatus(status: DriverStatus): SocialResult<Unit> = profile.updateStatus(status)

    suspend fun uploadAvatar(bitmap: Bitmap): SocialResult<String> = profile.uploadAvatar(bitmap)

    suspend fun removeAvatar(): SocialResult<Unit> = profile.removeAvatar()

    fun watchChats(): Flow<List<SocialChat>> = chat.watchChats()

    fun watchPublicGroups(): Flow<List<SocialChat>> = chat.watchPublicGroups()

    fun watchPeers(): Flow<List<SocialPeerProfile>> = chat.watchPeers()

    fun watchChatsSearch(query: String): Flow<List<SocialChat>> = chat.watchChatsSearch(query)

    fun watchTotalUnread(): Flow<Int> = chat.watchTotalUnread()

    fun watchMessages(chatId: String, limit: Int = MESSAGE_PAGE_SIZE): Flow<List<SocialMessage>> =
        chat.watchMessages(chatId, limit)

    suspend fun loadMoreMessages(
        chatId: String,
        beforeSentAt: Long,
        limit: Int = MESSAGE_PAGE_SIZE,
    ): SocialResult<List<SocialMessage>> = chat.loadMoreMessages(chatId, beforeSentAt, limit)

    suspend fun getChat(chatId: String): SocialChat? = chat.getChat(chatId)

    suspend fun sendMessage(
        chatId: String,
        text: String,
        senderName: String,
        messageType: MessageType = MessageType.TEXT,
        attachmentUrl: String? = null,
        replyToId: String? = null,
        locationLabel: String? = null,
        durationMs: Long = 0,
    ): SocialResult<Unit> = chat.sendMessage(
        chatId,
        text,
        senderName,
        messageType,
        attachmentUrl,
        replyToId,
        locationLabel,
        durationMs,
    )

    suspend fun addReaction(messageId: String, reaction: String): SocialResult<Unit> =
        chat.addReaction(messageId, reaction)

    fun recommendGroups(): Flow<List<SocialChat>> = chat.recommendGroups()

    suspend fun markChatRead(chatId: String) = chat.markChatRead(chatId)

    suspend fun createPrivateChat(peerName: String): SocialResult<String> = chat.createPrivateChat(peerName)

    suspend fun createPrivateChatWithPeer(peerId: String): SocialResult<String> =
        chat.createPrivateChatWithPeer(peerId)

    suspend fun createGroupChat(name: String, category: String = ""): SocialResult<String> =
        group.createGroupChat(name, category)

    suspend fun leaveGroup(chatId: String): SocialResult<Unit> = group.leaveGroup(chatId)

    suspend fun blockUser(blockedId: String): SocialResult<Unit> = profile.blockUser(blockedId)

    suspend fun unblockUser(blockedId: String): SocialResult<Unit> = profile.unblockUser(blockedId)

    suspend fun isBlocked(targetId: String): Boolean = profile.isBlocked(targetId)

    fun watchIsBlocked(targetId: String): Flow<Boolean> = profile.watchIsBlocked(targetId)

    fun watchFriendStatuses(): Flow<List<DriverStatusPost>> = status.watchFriendStatuses()

    suspend fun createTextStatus(text: String, displayName: String): SocialResult<Unit> =
        status.createTextStatus(text, displayName)

    suspend fun markStatusViewed(statusId: String): SocialResult<Unit> = status.markStatusViewed(statusId)

    suspend fun sendImageMessage(
        chatId: String,
        bitmap: Bitmap,
        caption: String,
        senderName: String,
    ): SocialResult<Unit> = media.sendImageMessage(chatId, bitmap, caption, senderName)

    suspend fun sendVoiceMessage(
        chatId: String,
        audioFile: File,
        durationMs: Long,
        senderName: String,
    ): SocialResult<Unit> = media.sendVoiceMessage(chatId, audioFile, durationMs, senderName)

    suspend fun createPhotoStatus(bitmap: Bitmap, displayName: String, caption: String = ""): SocialResult<Unit> =
        status.createPhotoStatus(bitmap, displayName, caption)

    suspend fun createVoiceStatus(audioFile: File, durationMs: Long, displayName: String): SocialResult<Unit> =
        status.createVoiceStatus(audioFile, durationMs, displayName)

    fun watchGroupMembers(chatId: String): Flow<List<ChatMember>> = group.watchGroupMembers(chatId)

    suspend fun joinGroup(chatId: String, displayName: String): SocialResult<Unit> =
        group.joinGroup(chatId, displayName)

    suspend fun joinGroupByInviteCode(code: String, displayName: String): SocialResult<String> =
        group.joinGroupByInviteCode(code, displayName)

    suspend fun followDriver(targetId: String): SocialResult<Unit> = profile.followDriver(targetId)

    suspend fun unfollowDriver(targetId: String): SocialResult<Unit> = profile.unfollowDriver(targetId)

    fun watchIsFollowing(targetId: String): Flow<Boolean> = profile.watchIsFollowing(targetId)

    fun watchPeer(peerId: String): Flow<SocialPeerProfile?> = profile.watchPeer(peerId)

    suspend fun getPeer(peerId: String): SocialPeerProfile? = profile.getPeer(peerId)

    fun watchLeaderboard(category: LeaderboardCategory = LeaderboardCategory.OVERALL): Flow<List<LeaderboardEntry>> =
        profile.watchLeaderboard(category)

    suspend fun hasJoinedWeeklyChallenge(): Boolean = profile.hasJoinedWeeklyChallenge()

    suspend fun refreshMyChallengeScore() = profile.refreshMyChallengeScore()

    suspend fun joinWeeklyChallenge(): SocialResult<Unit> = profile.joinWeeklyChallenge()

    suspend fun weeklyChallenge(): Challenge = profile.weeklyChallenge()

    suspend fun getLeaderboard(category: LeaderboardCategory): List<LeaderboardEntry> =
        profile.getLeaderboard(category)

    companion object {
        const val MESSAGE_PAGE_SIZE = SocialConstants.MESSAGE_PAGE_SIZE
        const val STATUS_TTL_MS = SocialConstants.STATUS_TTL_MS
        const val WEEKLY_CHALLENGE_ID = SocialConstants.WEEKLY_CHALLENGE_ID
    }
}
