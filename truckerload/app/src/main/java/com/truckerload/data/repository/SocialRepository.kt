package com.truckerload.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.repository.social.ChatRepository
import com.truckerload.data.repository.social.GroupRepository
import com.truckerload.data.repository.social.MediaRepository
import com.truckerload.data.repository.social.ProfileRepository
import com.truckerload.data.repository.social.SocialConstants
import com.truckerload.data.repository.social.SocialStackFactory
import com.truckerload.data.repository.social.SocialSyncCoordinator
import com.truckerload.data.repository.social.StatusRepository
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
 * Deprecated facade over focused social repositories.
 *
 * ViewModels keep injecting this type until they migrate to concrete repos.
 * Prefer [ProfileRepository], [ChatRepository], [GroupRepository],
 * [StatusRepository], [MediaRepository], or [SocialSyncCoordinator].
 */
@Deprecated(
    message = "Use specific repositories. Will be removed after ViewModels migrate (Phase 3.6)",
    replaceWith = ReplaceWith(
        "ProfileRepository",
        "com.truckerload.data.repository.social.ProfileRepository",
    ),
)
class SocialRepository(
    val profileRepository: ProfileRepository,
    val chatRepository: ChatRepository,
    val groupRepository: GroupRepository,
    val statusRepository: StatusRepository,
    val mediaRepository: MediaRepository,
    val syncCoordinator: SocialSyncCoordinator,
) {
    companion object {
        const val MESSAGE_PAGE_SIZE = SocialConstants.MESSAGE_PAGE_SIZE
        const val STATUS_TTL_MS = SocialConstants.STATUS_TTL_MS
        const val WEEKLY_CHALLENGE_ID = SocialConstants.WEEKLY_CHALLENGE_ID

        fun create(
            db: AppDatabase,
            loadRepository: LoadRepository,
            userProfileStore: UserProfileStore,
            context: Context,
        ): SocialRepository = SocialStackFactory.create(db, loadRepository, userProfileStore, context)
    }

    suspend fun ensureInitialized() = syncCoordinator.ensureInitialized()

    suspend fun syncIdentityFromUserProfile() = profileRepository.syncIdentityFromUserProfile()

    suspend fun needsProfileSetup(): Boolean = profileRepository.needsProfileSetup()

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
    ): SocialResult<Unit> = profileRepository.completeProfileSetup(
        displayName = displayName,
        phoneNumber = phoneNumber,
        homeCountryIso2 = homeCountryIso2,
        truckType = truckType,
        dateOfBirthEpochDay = dateOfBirthEpochDay,
        licenseClass = licenseClass,
        cdlNumber = cdlNumber,
        axleCount = axleCount,
        homeHubCity = homeHubCity,
    )

    suspend fun clearLocalIdentity() = profileRepository.clearLocalIdentity()

    fun watchMyEnhancedProfile(): Flow<EnhancedDriverProfile> =
        profileRepository.watchMyEnhancedProfile()

    fun watchMyProfile(): Flow<DriverProfile> = profileRepository.watchMyProfile()

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
    ): SocialResult<Unit> = profileRepository.updateProfile(
        displayName = displayName,
        truckType = truckType,
        experienceYears = experienceYears,
        homeState = homeState,
        routes = routes,
        about = about,
        status = status,
        licenseClass = licenseClass,
        endorsements = endorsements,
        specialties = specialties,
        phoneNumber = phoneNumber,
        telegramUsername = telegramUsername,
        whatsappNumber = whatsappNumber,
        maxRadius = maxRadius,
    )

    suspend fun updateStatus(status: DriverStatus): SocialResult<Unit> =
        profileRepository.updateStatus(status)

    suspend fun uploadAvatar(bitmap: Bitmap): SocialResult<String> =
        profileRepository.uploadAvatar(bitmap)

    suspend fun removeAvatar(): SocialResult<Unit> = profileRepository.removeAvatar()

    fun watchChats(): Flow<List<SocialChat>> = chatRepository.watchChats()

    fun watchPublicGroups(): Flow<List<SocialChat>> = groupRepository.watchPublicGroups()

    fun watchPeers(): Flow<List<SocialPeerProfile>> = chatRepository.watchPeers()

    fun watchChatsSearch(query: String): Flow<List<SocialChat>> =
        chatRepository.watchChatsSearch(query)

    fun watchTotalUnread(): Flow<Int> = chatRepository.watchTotalUnread()

    fun watchMessages(chatId: String, limit: Int = MESSAGE_PAGE_SIZE): Flow<List<SocialMessage>> =
        chatRepository.watchMessages(chatId, limit)

    suspend fun loadMoreMessages(
        chatId: String,
        beforeSentAt: Long,
        limit: Int = MESSAGE_PAGE_SIZE,
    ): SocialResult<List<SocialMessage>> =
        chatRepository.loadMoreMessages(chatId, beforeSentAt, limit)

    suspend fun getChat(chatId: String): SocialChat? = chatRepository.getChat(chatId)

    suspend fun sendMessage(
        chatId: String,
        text: String,
        senderName: String,
        messageType: MessageType = MessageType.TEXT,
        attachmentUrl: String? = null,
        replyToId: String? = null,
        locationLabel: String? = null,
        durationMs: Long = 0,
    ): SocialResult<Unit> = chatRepository.sendMessage(
        chatId = chatId,
        text = text,
        senderName = senderName,
        messageType = messageType,
        attachmentUrl = attachmentUrl,
        replyToId = replyToId,
        locationLabel = locationLabel,
        durationMs = durationMs,
    )

    suspend fun addReaction(messageId: String, reaction: String): SocialResult<Unit> =
        chatRepository.addReaction(messageId, reaction)

    fun recommendGroups(): Flow<List<SocialChat>> =
        groupRepository.recommendGroups(chatRepository.watchChats())

    suspend fun markChatRead(chatId: String) = chatRepository.markChatRead(chatId)

    suspend fun createPrivateChat(peerName: String): SocialResult<String> =
        chatRepository.createPrivateChat(peerName)

    suspend fun createPrivateChatWithPeer(peerId: String): SocialResult<String> =
        chatRepository.createPrivateChatWithPeer(peerId)

    suspend fun createGroupChat(name: String, category: String = ""): SocialResult<String> =
        groupRepository.createGroupChat(name, category)

    suspend fun leaveGroup(chatId: String): SocialResult<Unit> =
        groupRepository.leaveGroup(chatId)

    suspend fun blockUser(blockedId: String): SocialResult<Unit> =
        profileRepository.blockUser(blockedId)

    suspend fun unblockUser(blockedId: String): SocialResult<Unit> =
        profileRepository.unblockUser(blockedId)

    suspend fun isBlocked(targetId: String): Boolean = profileRepository.isBlocked(targetId)

    fun watchIsBlocked(targetId: String): Flow<Boolean> = profileRepository.watchIsBlocked(targetId)

    fun watchFriendStatuses(): Flow<List<DriverStatusPost>> = statusRepository.watchFriendStatuses()

    suspend fun createTextStatus(text: String, displayName: String): SocialResult<Unit> =
        statusRepository.createTextStatus(text, displayName)

    suspend fun markStatusViewed(statusId: String): SocialResult<Unit> =
        statusRepository.markStatusViewed(statusId)

    suspend fun sendImageMessage(
        chatId: String,
        bitmap: Bitmap,
        caption: String,
        senderName: String,
    ): SocialResult<Unit> = mediaRepository.sendImageMessage(chatId, bitmap, caption, senderName)

    suspend fun sendVoiceMessage(
        chatId: String,
        audioFile: File,
        durationMs: Long,
        senderName: String,
    ): SocialResult<Unit> = mediaRepository.sendVoiceMessage(chatId, audioFile, durationMs, senderName)

    suspend fun createPhotoStatus(
        bitmap: Bitmap,
        displayName: String,
        caption: String = "",
    ): SocialResult<Unit> = statusRepository.createPhotoStatus(bitmap, displayName, caption)

    suspend fun createVoiceStatus(
        audioFile: File,
        durationMs: Long,
        displayName: String,
    ): SocialResult<Unit> = statusRepository.createVoiceStatus(audioFile, durationMs, displayName)

    fun watchGroupMembers(chatId: String): Flow<List<ChatMember>> =
        groupRepository.watchGroupMembers(chatId)

    suspend fun joinGroup(chatId: String, displayName: String): SocialResult<Unit> =
        groupRepository.joinGroup(chatId, displayName)

    suspend fun joinGroupByInviteCode(code: String, displayName: String): SocialResult<String> =
        groupRepository.joinGroupByInviteCode(code, displayName)

    suspend fun followDriver(targetId: String): SocialResult<Unit> =
        profileRepository.followDriver(targetId)

    suspend fun unfollowDriver(targetId: String): SocialResult<Unit> =
        profileRepository.unfollowDriver(targetId)

    fun watchIsFollowing(targetId: String): Flow<Boolean> =
        profileRepository.watchIsFollowing(targetId)

    fun watchPeer(peerId: String): Flow<SocialPeerProfile?> = profileRepository.watchPeer(peerId)

    suspend fun getPeer(peerId: String): SocialPeerProfile? = profileRepository.getPeer(peerId)

    fun watchLeaderboard(
        category: LeaderboardCategory = LeaderboardCategory.OVERALL,
    ): Flow<List<LeaderboardEntry>> = syncCoordinator.watchLeaderboard(category)

    suspend fun hasJoinedWeeklyChallenge(): Boolean = syncCoordinator.hasJoinedWeeklyChallenge()

    suspend fun refreshMyChallengeScore() = syncCoordinator.refreshMyChallengeScore()

    suspend fun joinWeeklyChallenge(): SocialResult<Unit> = syncCoordinator.joinWeeklyChallenge()

    suspend fun weeklyChallenge(): Challenge = syncCoordinator.weeklyChallenge()

    suspend fun getLeaderboard(category: LeaderboardCategory): List<LeaderboardEntry> =
        syncCoordinator.getLeaderboard(category)
}
