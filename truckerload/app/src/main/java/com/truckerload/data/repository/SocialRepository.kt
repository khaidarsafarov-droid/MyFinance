package com.truckerload.data.repository

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.StringRes
import com.truckerload.R
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.BlockedUserEntity
import com.truckerload.data.local.entities.ChallengeParticipationEntity
import com.truckerload.data.local.entities.ChatMemberEntity
import com.truckerload.data.local.entities.DriverFollowEntity
import com.truckerload.data.local.entities.DriverProfileEntity
import com.truckerload.data.local.entities.DriverStatusEntity
import com.truckerload.data.local.entities.MessageReactionEntity
import com.truckerload.data.local.entities.SocialChatEntity
import com.truckerload.data.local.entities.SocialMessageEntity
import com.truckerload.data.local.entities.SocialPeerEntity
import com.truckerload.data.local.entities.WeeklyLoadStatsAgg
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.social.AvatarStorage
import com.truckerload.data.social.ChatAttachmentStorage
import com.truckerload.data.social.SocialMediaOptimizer
import com.truckerload.data.social.ContentModerator
import com.truckerload.data.social.RecommendationService
import com.truckerload.data.social.SocialPeerSeedData
import com.truckerload.domain.social.Badge
import com.truckerload.domain.social.Challenge
import com.truckerload.domain.social.ChallengeType
import com.truckerload.domain.social.ChatType
import com.truckerload.domain.social.ChatMember
import com.truckerload.domain.social.DriverProfile
import com.truckerload.domain.social.DriverStatus
import com.truckerload.domain.social.DriverStatusPost
import com.truckerload.domain.social.LeaderboardEntry
import com.truckerload.domain.social.MessageType
import com.truckerload.domain.social.StatusType
import com.truckerload.data.social.SocialSeedData
import com.truckerload.domain.geo.CountryCatalog
import com.truckerload.domain.social.BadgeEngine
import com.truckerload.domain.social.EnhancedDriverProfile
import com.truckerload.domain.social.ReactionSummary
import com.truckerload.domain.social.TruckType
import com.truckerload.domain.social.toLegacyProfile
import com.truckerload.domain.social.SocialChat
import com.truckerload.domain.social.SocialMessage
import com.truckerload.domain.social.SocialPeerProfile
import com.truckerload.domain.social.SocialResult
import com.truckerload.domain.social.LeaderboardCategory
import com.truckerload.domain.social.leaderboardScore
import com.truckerload.utils.getCurrentWeekNumberAndYear
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.UUID

class SocialRepository(
    db: AppDatabase,
    private val loadRepository: LoadRepository,
    private val userProfileStore: UserProfileStore,
    context: Context,
) {
    private val profileDao = db.driverProfileDao()
    private val chatDao = db.socialChatDao()
    private val messageDao = db.socialMessageDao()
    private val blockedUserDao = db.blockedUserDao()
    private val driverStatusDao = db.driverStatusDao()
    private val challengeDao = db.challengeParticipationDao()
    private val reactionDao = db.messageReactionDao()
    private val followDao = db.driverFollowDao()
    private val chatMemberDao = db.chatMemberDao()
    private val peerDao = db.socialPeerDao()
    private val avatarStorage = AvatarStorage(context)
    private val attachmentStorage = ChatAttachmentStorage(context)
    private val recommendations = RecommendationService()
    private val appContext = context.applicationContext
    private val initMutex = Mutex()

    private fun socialError(@StringRes fallbackRes: Int, throwable: Throwable): String =
        throwable.message?.takeIf { it.isNotBlank() } ?: appContext.getString(fallbackRes)

    suspend fun ensureInitialized() {
        initMutex.withLock {
            val userProfile = userProfileStore.profile.value
            val displayName = userProfile?.displayName.orEmpty()
            SocialSeedData.seedIfEmpty(
                chatDao,
                messageDao,
                profileDao,
                displayName,
                userProfile?.photoUrl,
                userProfile?.phoneNumber,
            )
            syncIdentityFromUserProfile()
            maybeMarkSetupCompleteFromExistingProfile()
            SocialPeerSeedData.seedIfEmpty(peerDao)
            seedDemoStatuses(displayName)
            seedGroupMemberships(displayName)
            backfillGroupInviteCodes()
            driverStatusDao.purgeExpired(System.currentTimeMillis())
            refreshMyChallengeScore()
        }
    }

    /**
     * Copies login identity (name / phone / photo) into the Room driver profile.
     * Never overwrites a local uploaded avatar file with a remote URL, or a filled phone/name with blanks.
     */
    suspend fun syncIdentityFromUserProfile() {
        val user = userProfileStore.profile.value ?: return
        val existing = profileDao.getProfile() ?: DriverProfileEntity()
        val loginName = user.displayName.takeIf { it.isNotBlank() && it != user.email }.orEmpty()
        val placeholder = existing.displayName.isBlank() ||
            existing.displayName == "Водитель" ||
            existing.displayName == "Driver" ||
            existing.displayName == "User"
        val mergedName = when {
            !placeholder -> existing.displayName
            loginName.isNotBlank() -> loginName
            else -> existing.displayName
        }
        val mergedPhone = existing.phoneNumber?.takeIf { it.isNotBlank() }
            ?: user.phoneNumber?.takeIf { it.isNotBlank() }
        val existingAvatar = existing.avatarUrl
        val mergedAvatar = when {
            !existingAvatar.isNullOrBlank() &&
                !existingAvatar.startsWith("http://") &&
                !existingAvatar.startsWith("https://") -> existingAvatar
            !existingAvatar.isNullOrBlank() -> existingAvatar
            !user.photoUrl.isNullOrBlank() -> user.photoUrl
            else -> null
        }
        val demoAbout = existing.about.contains("Дальнобойщик") || existing.about.contains("открытые дороги")
        val demoLanguages = existing.languagesJson == "Русский,Английский"
        // Legacy seed defaults (ratingCount=124, canned about/languages) must not look like real identity.
        profileDao.upsert(
            existing.copy(
                displayName = mergedName,
                phoneNumber = mergedPhone,
                avatarUrl = mergedAvatar,
                about = if (demoAbout) "" else existing.about,
                languagesJson = if (demoLanguages) "" else existing.languagesJson,
                ratingCount = 0,
                lastActive = System.currentTimeMillis(),
            ),
        )
    }

    /** True when the user still needs the first check-in (name + phone + country). */
    suspend fun needsProfileSetup(): Boolean {
        if (userProfileStore.setupComplete.value) return false
        val entity = profileDao.getProfile()
        val nameOk = !entity?.displayName.isNullOrBlank() &&
            entity?.displayName !in setOf("Водитель", "Driver", "User")
        val phoneOk = !entity?.phoneNumber.isNullOrBlank()
        val countryOk = CountryCatalog.byIso2(entity?.homeState) != null
        return !(nameOk && phoneOk && countryOk)
    }

    private suspend fun maybeMarkSetupCompleteFromExistingProfile() {
        if (userProfileStore.setupComplete.value) return
        val entity = profileDao.getProfile() ?: return
        val nameOk = entity.displayName.isNotBlank() &&
            entity.displayName !in setOf("Водитель", "Driver", "User")
        val phoneOk = !entity.phoneNumber.isNullOrBlank()
        val countryOk = CountryCatalog.byIso2(entity.homeState) != null
        // Only auto-complete for profiles that look intentionally filled (not seed leftovers).
        if (nameOk && phoneOk && countryOk) {
            userProfileStore.setSetupComplete(true)
        }
    }

    suspend fun completeProfileSetup(
        displayName: String,
        phoneNumber: String,
        homeCountryIso2: String,
        truckType: String = "",
    ): SocialResult<Unit> = runCatching {
        val existing = profileDao.getProfile() ?: DriverProfileEntity()
        val name = displayName.trim()
        val phone = phoneNumber.trim()
        val country = homeCountryIso2.trim().uppercase().take(2)
        require(name.isNotBlank()) { appContext.getString(R.string.profile_setup_name_required) }
        require(phone.filter { it.isDigit() }.length >= 8) {
            appContext.getString(R.string.profile_setup_phone_required)
        }
        require(country.length == 2 && CountryCatalog.byIso2(country) != null) {
            appContext.getString(R.string.profile_setup_country_required)
        }
        profileDao.upsert(
            existing.copy(
                displayName = name,
                phoneNumber = phone,
                homeState = country,
                truckType = truckType.trim().ifBlank { existing.truckType },
                // Wipe leftover demo identity fields from older installs.
                about = existing.about.takeIf { !it.contains("Дальнобойщик") && !it.contains("открытые дороги") }.orEmpty(),
                ratingCount = 0,
                languagesJson = existing.languagesJson.takeIf {
                    it != "Русский,Английский"
                }.orEmpty(),
                status = "ONLINE",
                lastActive = System.currentTimeMillis(),
            ),
        )
        val current = userProfileStore.profile.value
        if (current != null) {
            val parts = name.split(" ", limit = 2)
            userProfileStore.saveProfile(
                current.copy(
                    givenName = parts.firstOrNull().orEmpty(),
                    familyName = parts.getOrNull(1).orEmpty(),
                    phoneNumber = phone,
                ),
            )
        } else {
            val parts = name.split(" ", limit = 2)
            userProfileStore.saveProfile(
                com.truckerload.data.preferences.UserProfile(
                    email = "",
                    givenName = parts.firstOrNull().orEmpty(),
                    familyName = parts.getOrNull(1).orEmpty(),
                    photoUrl = null,
                    phoneNumber = phone,
                ),
            )
        }
        userProfileStore.setSetupComplete(true)
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(R.string.social_error_save_profile, it), it) }

    /** Clears personal identity fields so the next account does not inherit them. */
    suspend fun clearLocalIdentity() {
        val existing = profileDao.getProfile() ?: return
        avatarStorage.deleteAvatar(existing.avatarUrl)
        profileDao.upsert(
            existing.copy(
                displayName = "",
                avatarUrl = null,
                phoneNumber = null,
                telegramUsername = null,
                whatsappNumber = null,
                homeState = "",
                truckType = "",
                experienceYears = 0,
                routesJson = "",
                about = "",
                specialtiesJson = "",
                languagesJson = "",
                ratingCount = 0,
                reputation = 0,
                status = "OFFLINE",
            ),
        )
        userProfileStore.setSetupComplete(false)
    }

    fun watchMyEnhancedProfile(): Flow<EnhancedDriverProfile> =
        combine(
            profileDao.watchProfile(),
            loadRepository.watchTotalLoadStats(),
            userProfileStore.profile,
        ) { entity, stats, userProfile ->
            buildEnhancedProfile(
                entity,
                stats.totalLoads,
                stats.totalMiles.toInt(),
                stats.totalRevenue,
                userProfile?.photoUrl,
            )
        }

    fun watchMyProfile(): Flow<DriverProfile> =
        watchMyEnhancedProfile().map { it.toLegacyProfile() }

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
    ): SocialResult<Unit> = runCatching {
        val existing = profileDao.getProfile() ?: DriverProfileEntity()
        val country = homeState.trim().uppercase().take(2)
        profileDao.upsert(
            existing.copy(
                displayName = displayName,
                truckType = truckType,
                experienceYears = experienceYears,
                homeState = country,
                routesJson = routes.joinToString(","),
                about = about,
                status = status.name,
                licenseClass = licenseClass.trim(),
                endorsementsJson = endorsements.joinToString(","),
                specialtiesJson = specialties.joinToString(","),
                phoneNumber = phoneNumber?.trim()?.ifBlank { null },
                telegramUsername = telegramUsername?.trim()?.removePrefix("@")?.ifBlank { null },
                whatsappNumber = whatsappNumber?.trim()?.ifBlank { null },
                maxRadius = maxRadius.coerceAtLeast(50),
                lastActive = System.currentTimeMillis(),
            ),
        )
        userProfileStore.profile.value?.let { current ->
            val parts = displayName.trim().split(" ", limit = 2)
            userProfileStore.saveProfile(
                current.copy(
                    givenName = parts.firstOrNull().orEmpty(),
                    familyName = parts.getOrNull(1).orEmpty(),
                    phoneNumber = phoneNumber?.trim()?.ifBlank { null },
                ),
            )
        }
        if (displayName.isNotBlank() &&
            !phoneNumber.isNullOrBlank() &&
            CountryCatalog.byIso2(homeState) != null
        ) {
            userProfileStore.setSetupComplete(true)
        }
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(R.string.social_error_save_profile, it), it) }

    suspend fun updateStatus(status: DriverStatus): SocialResult<Unit> = runCatching {
        val existing = profileDao.getProfile() ?: DriverProfileEntity()
        profileDao.upsert(existing.copy(status = status.name))
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(R.string.social_error_update_status, it), it) }

    suspend fun uploadAvatar(bitmap: Bitmap): SocialResult<String> = runCatching {
        val compressed = SocialMediaOptimizer.compressImage(bitmap)
        val path = avatarStorage.saveAvatar(DriverProfileEntity.LOCAL_USER_ID, compressed)
        val existing = profileDao.getProfile() ?: DriverProfileEntity()
        avatarStorage.deleteAvatar(existing.avatarUrl)
        profileDao.upsert(existing.copy(avatarUrl = path))
        SocialResult.Success(path)
    }.getOrElse { SocialResult.Error(socialError(R.string.social_error_upload_avatar, it), it) }

    suspend fun removeAvatar(): SocialResult<Unit> = runCatching {
        val existing = profileDao.getProfile() ?: DriverProfileEntity()
        avatarStorage.deleteAvatar(existing.avatarUrl)
        profileDao.upsert(existing.copy(avatarUrl = null))
        // Also clear login/Google photo so remove actually sticks in the UI.
        userProfileStore.profile.value?.let { profile ->
            if (!profile.photoUrl.isNullOrBlank()) {
                userProfileStore.saveProfile(profile.copy(photoUrl = null))
            }
        }
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(R.string.social_error_upload_avatar, it), it) }

    fun watchChats(): Flow<List<SocialChat>> =
        combine(
            chatDao.watchChats(),
            chatMemberDao.watchMemberChatIds(DriverProfileEntity.LOCAL_USER_ID),
            blockedUserDao.watchBlockedIds(DriverProfileEntity.LOCAL_USER_ID),
        ) { chats, memberChatIds, blockedIds ->
            mapChatsWithMembership(chats, memberChatIds.toSet(), blockedIds.toSet())
        }

    fun watchPublicGroups(): Flow<List<SocialChat>> =
        combine(
            chatDao.watchChats(),
            chatMemberDao.watchMemberChatIds(DriverProfileEntity.LOCAL_USER_ID),
        ) { chats, memberChatIds ->
            val memberSet = memberChatIds.toSet()
            chats
                .filter { it.type == ChatType.GROUP.name && it.isPublic && !it.archived }
                .map { it.toDomain(isMember = memberSet.contains(it.id)) }
        }

    fun watchPeers(): Flow<List<SocialPeerProfile>> =
        combine(
            peerDao.watchAll(),
            blockedUserDao.watchBlockedIds(DriverProfileEntity.LOCAL_USER_ID),
        ) { peers, blockedIds ->
            val blockedSet = blockedIds.toSet()
            peers.filter { it.id !in blockedSet }.map { it.toPeerProfile() }
        }

    fun watchChatsSearch(query: String): Flow<List<SocialChat>> {
        val trimmed = query.trim()
        val chatSource = if (trimmed.isEmpty()) {
            chatDao.watchChats()
        } else {
            chatDao.watchChatsSearch(trimmed)
        }
        return combine(
            chatSource,
            chatMemberDao.watchMemberChatIds(DriverProfileEntity.LOCAL_USER_ID),
            blockedUserDao.watchBlockedIds(DriverProfileEntity.LOCAL_USER_ID),
        ) { chats, memberChatIds, blockedIds ->
            mapChatsWithMembership(chats, memberChatIds.toSet(), blockedIds.toSet())
        }
    }

    fun watchTotalUnread(): Flow<Int> = chatDao.watchTotalUnread()

    fun watchMessages(chatId: String, limit: Int = MESSAGE_PAGE_SIZE): Flow<List<SocialMessage>> =
        combine(
            messageDao.watchRecentMessages(chatId, limit),
            reactionDao.watchReactionsForChat(chatId),
        ) { messages, reactions ->
            val byId = messages.associateBy { it.id }
            messages.map { entity ->
                entity.toDomain(
                    isMine = entity.senderId == LOCAL_SENDER_ID,
                    reactions = summarizeReactions(reactions.filter { it.messageId == entity.id }),
                    replyPreview = entity.replyToId?.let { byId[it]?.text },
                )
            }
        }

    suspend fun loadMoreMessages(
        chatId: String,
        beforeSentAt: Long,
        limit: Int = MESSAGE_PAGE_SIZE,
    ): SocialResult<List<SocialMessage>> = runCatching {
        val older = messageDao.getMessagesBefore(chatId, beforeSentAt, limit)
            .map { it.toDomain(isMine = it.senderId == LOCAL_SENDER_ID) }
        SocialResult.Success(older)
    }.getOrElse { SocialResult.Error(socialError(R.string.social_error_load_messages, it), it) }

    suspend fun getChat(chatId: String): SocialChat? =
        chatDao.getChat(chatId)?.toDomain()

    suspend fun sendMessage(
        chatId: String,
        text: String,
        senderName: String,
        messageType: MessageType = MessageType.TEXT,
        attachmentUrl: String? = null,
        replyToId: String? = null,
        locationLabel: String? = null,
        durationMs: Long = 0,
    ): SocialResult<Unit> = runCatching {
        val trimmed = text.trim()
        if (trimmed.isEmpty() && attachmentUrl.isNullOrBlank()) {
            return SocialResult.Error("Пустое сообщение")
        }
        val moderation = ContentModerator.moderateText(trimmed)
        if (!moderation.allowed) {
            return SocialResult.Error(moderation.reason ?: "Сообщение отклонено")
        }
        val now = System.currentTimeMillis()
        val preview = when (messageType) {
            MessageType.IMAGE -> "📷 Фото"
            MessageType.VOICE -> "🎤 Голосовое"
            MessageType.ANNOUNCEMENT -> "📌 $trimmed"
            MessageType.TEXT -> trimmed
        }
        val message = SocialMessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = LOCAL_SENDER_ID,
            senderName = senderName,
            text = trimmed.ifBlank { preview },
            sentAt = now,
            messageType = messageType.name,
            attachmentUrl = attachmentUrl,
            replyToId = replyToId,
            locationLabel = locationLabel,
            isAnnouncement = messageType == MessageType.ANNOUNCEMENT,
            durationMs = durationMs,
        )
        messageDao.insert(message)
        val chat = chatDao.getChat(chatId) ?: return SocialResult.Error("Чат не найден")
        chatDao.upsert(
            chat.copy(
                lastMessage = preview,
                lastMessageAt = now,
                unreadCount = 0,
            ),
        )
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(R.string.social_error_send_message, it), it) }

    suspend fun addReaction(messageId: String, reaction: String): SocialResult<Unit> = runCatching {
        reactionDao.upsert(
            MessageReactionEntity(
                messageId = messageId,
                userId = DriverProfileEntity.LOCAL_USER_ID,
                reaction = reaction,
                reactedAt = System.currentTimeMillis(),
            ),
        )
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(R.string.social_error_add_reaction, it), it) }

    fun recommendGroups(): Flow<List<SocialChat>> =
        watchChats().map { recommendations.recommendGroups(it) }

    suspend fun markChatRead(chatId: String) {
        chatDao.markRead(chatId)
    }

    suspend fun createPrivateChat(peerName: String): SocialResult<String> = runCatching {
        val trimmedName = peerName.trim()
        if (trimmedName.isBlank()) {
            return SocialResult.Error(appContext.getString(R.string.social_error_create_chat))
        }
        findPrivateChatByTitle(trimmedName)?.let { return SocialResult.Success(it.id) }
        val chatId = "dm_${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        chatDao.upsert(
            SocialChatEntity(
                id = chatId,
                title = trimmedName,
                type = ChatType.PRIVATE.name,
                participantCount = 2,
                lastMessage = "",
                lastMessageAt = now,
                unreadCount = 0,
                avatarEmoji = "👤",
                onlineCount = 0,
            ),
        )
        SocialResult.Success(chatId)
    }.getOrElse { SocialResult.Error(socialError(R.string.social_error_create_chat, it), it) }

    suspend fun createPrivateChatWithPeer(peerId: String): SocialResult<String> = runCatching {
        if (peerId == DriverProfileEntity.LOCAL_USER_ID) {
            return SocialResult.Error("Нельзя написать самому себе")
        }
        if (isBlocked(peerId)) {
            return SocialResult.Error(appContext.getString(R.string.social_user_blocked))
        }
        findPrivateChatForPeer(peerId)?.let { return SocialResult.Success(it.id) }
        val peer = peerDao.getById(peerId)
            ?: return SocialResult.Error(appContext.getString(R.string.social_peer_not_found))
        val chatId = privateChatIdForPeer(peerId)
        val now = System.currentTimeMillis()
        chatDao.upsert(
            SocialChatEntity(
                id = chatId,
                title = peer.displayName,
                type = ChatType.PRIVATE.name,
                participantCount = 2,
                lastMessage = "",
                lastMessageAt = now,
                unreadCount = 0,
                avatarEmoji = "👤",
                onlineCount = 1,
            ),
        )
        SocialResult.Success(chatId)
    }.getOrElse { SocialResult.Error(socialError(R.string.social_error_create_chat, it), it) }

    suspend fun createGroupChat(name: String, category: String = ""): SocialResult<String> = runCatching {
        val chatId = "group_${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        val inviteCode = chatId.takeLast(6).uppercase()
        val displayName = profileDao.getProfile()?.displayName.orEmpty()
        chatDao.upsert(
            SocialChatEntity(
                id = chatId,
                title = name.ifBlank { "Новая группа" },
                type = ChatType.GROUP.name,
                participantCount = 1,
                lastMessage = "",
                lastMessageAt = now,
                unreadCount = 0,
                avatarEmoji = "👥",
                onlineCount = 1,
                category = category,
                creatorId = DriverProfileEntity.LOCAL_USER_ID,
                inviteCode = inviteCode,
            ),
        )
        chatMemberDao.upsert(
            ChatMemberEntity(
                chatId = chatId,
                userId = DriverProfileEntity.LOCAL_USER_ID,
                displayName = displayName.ifBlank { "Вы" },
                role = "OWNER",
                joinedAt = now,
            ),
        )
        SocialResult.Success(chatId)
    }.getOrElse { SocialResult.Error(socialError(R.string.social_error_create_group, it), it) }

    suspend fun leaveGroup(chatId: String): SocialResult<Unit> = runCatching {
        chatMemberDao.remove(chatId, DriverProfileEntity.LOCAL_USER_ID)
        chatDao.archiveChat(chatId)
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(R.string.social_error_leave_group, it), it) }

    suspend fun blockUser(blockedId: String): SocialResult<Unit> = runCatching {
        blockedUserDao.block(
            BlockedUserEntity(
                blockerId = DriverProfileEntity.LOCAL_USER_ID,
                blockedId = blockedId,
                blockedAt = System.currentTimeMillis(),
            ),
        )
        followDao.unfollow(DriverProfileEntity.LOCAL_USER_ID, blockedId)
        findPrivateChatForPeer(blockedId)?.let { chatDao.archiveChat(it.id) }
        updateFollowCounts()
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(R.string.social_error_block_user, it), it) }

    suspend fun unblockUser(blockedId: String): SocialResult<Unit> = runCatching {
        blockedUserDao.unblock(DriverProfileEntity.LOCAL_USER_ID, blockedId)
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(R.string.social_error_unblock_user, it), it) }

    suspend fun isBlocked(targetId: String): Boolean =
        blockedUserDao.isBlocked(DriverProfileEntity.LOCAL_USER_ID, targetId)

    fun watchIsBlocked(targetId: String): Flow<Boolean> =
        blockedUserDao.watchBlockedIds(DriverProfileEntity.LOCAL_USER_ID)
            .map { blockedIds -> targetId in blockedIds }

    fun watchFriendStatuses(): Flow<List<DriverStatusPost>> =
        combine(
            driverStatusDao.watchActiveStatuses(System.currentTimeMillis()),
            blockedUserDao.watchBlockedIds(DriverProfileEntity.LOCAL_USER_ID),
        ) { statuses, blockedIds ->
            val blockedSet = blockedIds.toSet()
            statuses
                .filter { it.userId !in blockedSet }
                .map { it.toDomain() }
        }

    suspend fun createTextStatus(text: String, displayName: String): SocialResult<Unit> = runCatching {
        val now = System.currentTimeMillis()
        driverStatusDao.insert(
            DriverStatusEntity(
                id = UUID.randomUUID().toString(),
                userId = DriverProfileEntity.LOCAL_USER_ID,
                displayName = displayName,
                type = StatusType.TEXT.name,
                text = text.trim(),
                mediaPath = null,
                createdAt = now,
                expiresAt = now + STATUS_TTL_MS,
                viewed = false,
            ),
        )
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(R.string.social_error_create_status, it), it) }

    suspend fun markStatusViewed(statusId: String): SocialResult<Unit> = runCatching {
        driverStatusDao.markViewed(statusId)
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(R.string.social_error_mark_viewed, it), it) }

    suspend fun sendImageMessage(
        chatId: String,
        bitmap: Bitmap,
        caption: String,
        senderName: String,
    ): SocialResult<Unit> {
        val path = attachmentStorage.saveImage(chatId, bitmap)
        return sendMessage(chatId, caption, senderName, MessageType.IMAGE, path)
    }

    suspend fun sendVoiceMessage(
        chatId: String,
        audioFile: File,
        durationMs: Long,
        senderName: String,
    ): SocialResult<Unit> {
        val path = attachmentStorage.saveVoice(chatId, audioFile)
        return sendMessage(chatId, "", senderName, MessageType.VOICE, path, durationMs = durationMs)
    }

    suspend fun createPhotoStatus(bitmap: Bitmap, displayName: String, caption: String = ""): SocialResult<Unit> =
        runCatching {
            val path = attachmentStorage.saveStatusPhoto(bitmap)
            val now = System.currentTimeMillis()
            driverStatusDao.insert(
                DriverStatusEntity(
                    id = UUID.randomUUID().toString(),
                    userId = DriverProfileEntity.LOCAL_USER_ID,
                    displayName = displayName,
                    type = StatusType.PHOTO.name,
                    text = caption.ifBlank { null },
                    mediaPath = path,
                    createdAt = now,
                    expiresAt = now + STATUS_TTL_MS,
                ),
            )
            SocialResult.Success(Unit)
        }.getOrElse { SocialResult.Error(socialError(R.string.social_error_create_status, it), it) }

    suspend fun createVoiceStatus(audioFile: File, durationMs: Long, displayName: String): SocialResult<Unit> =
        runCatching {
            val path = attachmentStorage.saveStatusVoice(audioFile)
            val now = System.currentTimeMillis()
            driverStatusDao.insert(
                DriverStatusEntity(
                    id = UUID.randomUUID().toString(),
                    userId = DriverProfileEntity.LOCAL_USER_ID,
                    displayName = displayName,
                    type = StatusType.VOICE.name,
                    text = null,
                    mediaPath = path,
                    createdAt = now,
                    expiresAt = now + STATUS_TTL_MS,
                    durationMs = durationMs,
                ),
            )
            SocialResult.Success(Unit)
        }.getOrElse { SocialResult.Error(socialError(R.string.social_error_create_status, it), it) }

    fun watchGroupMembers(chatId: String): Flow<List<ChatMember>> =
        chatMemberDao.watchMembers(chatId).map { members ->
            members.map {
                ChatMember(
                    chatId = it.chatId,
                    userId = it.userId,
                    displayName = it.displayName,
                    role = it.role,
                    joinedAt = it.joinedAt,
                    isMe = it.userId == DriverProfileEntity.LOCAL_USER_ID,
                )
            }
        }

    suspend fun joinGroup(chatId: String, displayName: String): SocialResult<Unit> = runCatching {
        if (chatMemberDao.isMember(chatId, DriverProfileEntity.LOCAL_USER_ID)) {
            return SocialResult.Success(Unit)
        }
        val now = System.currentTimeMillis()
        chatMemberDao.upsert(
            ChatMemberEntity(
                chatId = chatId,
                userId = DriverProfileEntity.LOCAL_USER_ID,
                displayName = displayName.ifBlank { "Вы" },
                role = "MEMBER",
                joinedAt = now,
            ),
        )
        val chat = chatDao.getChat(chatId) ?: return SocialResult.Error("Группа не найдена")
        chatDao.upsert(chat.copy(participantCount = chatMemberDao.countMembers(chatId).coerceAtLeast(1)))
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(R.string.social_error_join_challenge, it), it) }

    suspend fun joinGroupByInviteCode(code: String, displayName: String): SocialResult<String> {
        val normalized = com.truckerload.domain.social.GroupInviteCode.normalize(code)
        if (com.truckerload.domain.social.GroupInviteCode.isBlank(normalized)) {
            return SocialResult.Error("Группа с таким кодом не найдена")
        }
        val chat = chatDao.getChatByInviteCode(normalized)
            ?: return SocialResult.Error("Группа с таким кодом не найдена")
        return when (val joined = joinGroup(chat.id, displayName)) {
            is SocialResult.Success -> SocialResult.Success(chat.id)
            is SocialResult.Error -> joined
        }
    }

    suspend fun followDriver(targetId: String): SocialResult<Unit> = runCatching {
        if (targetId == DriverProfileEntity.LOCAL_USER_ID) return SocialResult.Error("Нельзя подписаться на себя")
        followDao.follow(
            DriverFollowEntity(
                followerId = DriverProfileEntity.LOCAL_USER_ID,
                followingId = targetId,
                followedAt = System.currentTimeMillis(),
            ),
        )
        updateFollowCounts()
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(R.string.social_error_follow, it), it) }

    suspend fun unfollowDriver(targetId: String): SocialResult<Unit> = runCatching {
        followDao.unfollow(DriverProfileEntity.LOCAL_USER_ID, targetId)
        updateFollowCounts()
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(R.string.social_error_unfollow, it), it) }

    fun watchIsFollowing(targetId: String): Flow<Boolean> =
        followDao.watchIsFollowing(DriverProfileEntity.LOCAL_USER_ID, targetId)

    fun watchPeer(peerId: String): Flow<SocialPeerProfile?> =
        peerDao.watchById(peerId).map { entity -> entity?.toPeerProfile() }

    suspend fun getPeer(peerId: String): SocialPeerProfile? =
        peerDao.getById(peerId)?.toPeerProfile()

    fun watchLeaderboard(category: LeaderboardCategory = LeaderboardCategory.OVERALL): Flow<List<LeaderboardEntry>> {
        val (week, year) = getCurrentWeekNumberAndYear()
        return combine(
            loadRepository.watchWeeklyLoadStats(week, year),
            peerDao.watchAll(),
            blockedUserDao.watchBlockedIds(DriverProfileEntity.LOCAL_USER_ID),
        ) { weekStats, peers, blockedIds ->
            val blockedSet = blockedIds.toSet()
            buildLeaderboard(weekStats, peers.filter { it.id !in blockedSet }, category)
        }
    }

    suspend fun hasJoinedWeeklyChallenge(): Boolean =
        challengeDao.getParticipation(WEEKLY_CHALLENGE_ID, DriverProfileEntity.LOCAL_USER_ID) != null

    suspend fun refreshMyChallengeScore() {
        val (week, year) = getCurrentWeekNumberAndYear()
        val miles = loadRepository.getWeeklyLoadStatsOnce(week, year).totalMiles
        val existing = challengeDao.getParticipation(WEEKLY_CHALLENGE_ID, DriverProfileEntity.LOCAL_USER_ID)
        if (existing != null) {
            challengeDao.updateScore(WEEKLY_CHALLENGE_ID, DriverProfileEntity.LOCAL_USER_ID, miles)
        }
    }

    suspend fun joinWeeklyChallenge(): SocialResult<Unit> = runCatching {
        val (week, year) = getCurrentWeekNumberAndYear()
        val miles = loadRepository.getWeeklyLoadStatsOnce(week, year).totalMiles
        challengeDao.join(
            ChallengeParticipationEntity(
                challengeId = WEEKLY_CHALLENGE_ID,
                userId = DriverProfileEntity.LOCAL_USER_ID,
                score = miles,
                joinedAt = System.currentTimeMillis(),
            ),
        )
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(R.string.social_error_join_chat, it), it) }

    suspend fun weeklyChallenge(): Challenge {
        val (week, year) = getCurrentWeekNumberAndYear()
        val weekStats = loadRepository.getWeeklyLoadStatsOnce(week, year)
        val myMiles = weekStats.totalMiles
        val myName = profileDao.getProfile()?.displayName
            ?: userProfileStore.profile.value?.displayName
            ?: "Вы"
        refreshMyChallengeScore()
        val participation = challengeDao.getParticipation(WEEKLY_CHALLENGE_ID, DriverProfileEntity.LOCAL_USER_ID)
        val score = participation?.score ?: myMiles
        val now = System.currentTimeMillis()
        val peers = peerDao.getAll()
        val peerBoard = peers.map { peer ->
            LeaderboardEntry(
                rank = 0,
                displayName = peer.displayName,
                score = peer.weeklyMiles,
                rating = peer.rating,
                trend = "—",
                userId = peer.id,
            )
        }
        val myEntry = LeaderboardEntry(
            rank = 0,
            displayName = myName,
            score = score,
            rating = 4.8,
            trend = if (score > 0) "⬆" else "—",
            isMe = true,
        )
        val merged = (peerBoard + myEntry).sortedByDescending { it.score }
            .mapIndexed { index, entry -> entry.copy(rank = index + 1) }
        val myPosition = merged.indexOfFirst { it.isMe }.let { if (it >= 0) it + 1 else merged.size }
        return Challenge(
            id = WEEKLY_CHALLENGE_ID,
            title = "Король миль",
            description = "Кто проедет больше всех миль за неделю $week/$year",
            type = ChallengeType.MILES,
            goal = 3000.0,
            startDate = now - 4 * 24 * 60 * 60_000L,
            endDate = now + 3 * 24 * 60 * 60_000L,
            leaderboard = merged.take(10),
            myPosition = myPosition,
            myScore = score,
        )
    }

    suspend fun getLeaderboard(category: LeaderboardCategory): List<LeaderboardEntry> {
        val (week, year) = getCurrentWeekNumberAndYear()
        val weekStats = loadRepository.getWeeklyLoadStatsOnce(week, year)
        val peers = peerDao.getAll()
        val blockedIds = blockedUserDao.watchBlockedIds(DriverProfileEntity.LOCAL_USER_ID).first().toSet()
        return buildLeaderboard(weekStats, peers.filter { it.id !in blockedIds }, category)
    }

    private suspend fun seedDemoStatuses(displayName: String) {
        val now = System.currentTimeMillis()
        val existing = driverStatusDao.watchActiveStatuses(now).first()
        if (existing.any { it.userId != DriverProfileEntity.LOCAL_USER_ID }) return
        listOf(
            Triple("peer_ivan", "Иван Петров", "На I-95, RPM отличный!"),
            Triple("peer_alexey", "Алексей С.", "Ищу груз TX → FL"),
            Triple("peer_sergey", "Сергей К.", "Отдыхаю в Atlanta"),
        ).forEach { (userId, name, text) ->
            driverStatusDao.insert(
                DriverStatusEntity(
                    id = "status_$userId",
                    userId = userId,
                    displayName = name,
                    type = StatusType.TEXT.name,
                    text = text,
                    mediaPath = null,
                    createdAt = now - 60_000,
                    expiresAt = now + STATUS_TTL_MS,
                ),
            )
        }
        if (displayName.isNotBlank()) {
            driverStatusDao.insert(
                DriverStatusEntity(
                    id = "status_me",
                    userId = DriverProfileEntity.LOCAL_USER_ID,
                    displayName = displayName,
                    type = StatusType.TEXT.name,
                    text = "На связи!",
                    mediaPath = null,
                    createdAt = now,
                    expiresAt = now + STATUS_TTL_MS,
                ),
            )
        }
    }

    private suspend fun seedGroupMemberships(displayName: String) {
        val seedGroupIds = listOf("group_i95", "group_fuel", "group_help")
        val now = System.currentTimeMillis()
        seedGroupIds.forEach { groupId ->
            if (!chatMemberDao.isMember(groupId, DriverProfileEntity.LOCAL_USER_ID)) {
                chatMemberDao.upsert(
                    ChatMemberEntity(
                        chatId = groupId,
                        userId = DriverProfileEntity.LOCAL_USER_ID,
                        displayName = displayName.ifBlank { "Вы" },
                        role = "MEMBER",
                        joinedAt = now,
                    ),
                )
            }
        }
    }

    private fun buildLeaderboard(
        weekStats: WeeklyLoadStatsAgg,
        peers: List<SocialPeerEntity>,
        category: LeaderboardCategory,
    ): List<LeaderboardEntry> {
        val localName = userProfileStore.profile.value?.displayName ?: "Вы"
        val myScore = weekStats.leaderboardScore(category)
        val peerEntries = peers.map { peer ->
            val score = when (category) {
                LeaderboardCategory.LOADS -> peer.weeklyLoads.toDouble()
                LeaderboardCategory.REVENUE -> peer.weeklyRevenue
                LeaderboardCategory.RPM -> peer.weeklyRpm
                LeaderboardCategory.OVERALL -> peer.weeklyMiles
            }
            LeaderboardEntry(
                rank = 0,
                displayName = peer.displayName,
                score = score,
                rating = peer.rating,
                trend = "—",
                userId = peer.id,
            )
        }
        val myEntry = LeaderboardEntry(
            rank = 0,
            displayName = localName,
            score = myScore,
            rating = 4.8,
            trend = if (myScore > 0) "⬆" else "—",
            isMe = true,
        )
        return (peerEntries + myEntry)
            .sortedByDescending { it.score }
            .mapIndexed { index, entry -> entry.copy(rank = index + 1) }
    }

    private fun buildEnhancedProfile(
        entity: DriverProfileEntity?,
        totalLoads: Int,
        totalMiles: Int,
        totalRevenue: Double,
        avatarUrl: String?,
    ): EnhancedDriverProfile {
        val base = entity ?: DriverProfileEntity()
        val routes = base.routesJson.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val endorsements = base.endorsementsJson.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val specialties = base.specialtiesJson.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val languages = base.languagesJson
            .takeIf { it != "Русский,Английский" }
            .orEmpty()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val status = runCatching { DriverStatus.valueOf(base.status) }.getOrDefault(DriverStatus.OFFLINE)
        val averageRpm = if (totalMiles > 0) totalRevenue / totalMiles else 0.0
        // No fake on-time / ratings — only real load stats until reviews exist.
        val onTimePercentage = 0.0
        val about = base.about.takeIf {
            !it.contains("Дальнобойщик") && !it.contains("открытые дороги")
        }.orEmpty()
        val badges = BadgeEngine.compute(
            totalLoads = totalLoads,
            totalMiles = totalMiles,
            totalRevenue = totalRevenue,
            averageRpm = averageRpm,
            experienceYears = base.experienceYears,
            endorsements = endorsements,
            onTimePercentage = onTimePercentage,
        )
        val reputation = if (totalLoads > 0) badges.size * 25 + totalLoads.coerceAtMost(500) else 0
        val loginName = userProfileStore.profile.value?.displayName?.takeIf {
            it.isNotBlank() && it != userProfileStore.profile.value?.email
        }
        val displayName = base.displayName
            .takeIf { it.isNotBlank() && it !in setOf("Водитель", "Driver", "User") }
            ?: loginName.orEmpty()
        return EnhancedDriverProfile(
            id = base.id,
            displayName = displayName,
            avatarUrl = base.avatarUrl ?: avatarUrl,
            coverImageUrl = base.coverImageUrl,
            truckType = TruckType.fromLabel(base.truckType),
            experienceYears = base.experienceYears,
            licenseClass = base.licenseClass,
            endorsements = endorsements,
            homeState = base.homeState,
            preferredRoutes = routes,
            maxRadius = base.maxRadius,
            totalLoads = totalLoads,
            totalMiles = totalMiles,
            totalRevenue = totalRevenue,
            averageRpm = averageRpm,
            onTimePercentage = onTimePercentage,
            rating = 0.0,
            ratingCount = 0,
            reputation = reputation,
            badges = badges,
            followers = base.followers,
            following = base.following,
            status = status,
            currentRoute = base.currentRoute ?: routes.firstOrNull(),
            about = about,
            specialties = specialties,
            languages = languages,
            phoneNumber = base.phoneNumber,
            telegramUsername = base.telegramUsername,
            whatsappNumber = base.whatsappNumber,
            joinedDate = base.joinedDate,
            lastActive = base.lastActive.takeIf { it > 0 } ?: System.currentTimeMillis(),
        )
    }

    private fun privateChatIdForPeer(peerId: String): String = "dm_$peerId"

    private suspend fun findPrivateChatForPeer(peerId: String): SocialChatEntity? {
        chatDao.getChat(privateChatIdForPeer(peerId))?.let { return it }
        val peer = peerDao.getById(peerId) ?: return null
        return findPrivateChatByTitle(peer.displayName)
    }

    private suspend fun findPrivateChatByTitle(title: String): SocialChatEntity? =
        chatDao.watchChats().first()
            .firstOrNull { it.type == ChatType.PRIVATE.name && it.title.equals(title, ignoreCase = true) }

    private fun mapChatsWithMembership(
        chats: List<SocialChatEntity>,
        memberChatIds: Set<String>,
        blockedIds: Set<String>,
    ): List<SocialChat> =
        chats
            .filter { chat -> chat.type != ChatType.PRIVATE.name || !isBlockedPrivateChat(chat, blockedIds) }
            .map { chat -> chat.toDomain(isMember = resolveIsMember(chat, memberChatIds)) }

    private fun resolveIsMember(chat: SocialChatEntity, memberChatIds: Set<String>): Boolean =
        when {
            chat.type == ChatType.PRIVATE.name -> true
            chat.type == ChatType.GROUP.name -> memberChatIds.contains(chat.id)
            else -> true
        }

    private fun isBlockedPrivateChat(chat: SocialChatEntity, blockedIds: Set<String>): Boolean {
        if (chat.type != ChatType.PRIVATE.name) return false
        val peerId = chat.id.removePrefix("dm_")
        return peerId.startsWith("peer_") && peerId in blockedIds
    }

    private suspend fun backfillGroupInviteCodes() {
        val codes = mapOf(
            "group_i95" to "I95ROAD",
            "group_fuel" to "FUELNOW",
            "group_help" to "ROADHELP",
        )
        codes.forEach { (groupId, code) ->
            val chat = chatDao.getChat(groupId) ?: return@forEach
            if (chat.inviteCode.isBlank()) {
                chatDao.upsert(chat.copy(inviteCode = code))
            }
        }
    }

    private fun SocialPeerEntity.toPeerProfile() = SocialPeerProfile(
        id = id,
        displayName = displayName,
        rating = rating,
        weeklyMiles = weeklyMiles,
        weeklyRevenue = weeklyRevenue,
        weeklyLoads = weeklyLoads,
        weeklyRpm = weeklyRpm,
    )

    private suspend fun updateFollowCounts() {
        val existing = profileDao.getProfile() ?: return
        profileDao.upsert(
            existing.copy(
                followers = followDao.countFollowers(DriverProfileEntity.LOCAL_USER_ID),
                following = followDao.countFollowing(DriverProfileEntity.LOCAL_USER_ID),
            ),
        )
    }

    private fun summarizeReactions(reactions: List<MessageReactionEntity>): List<ReactionSummary> =
        reactions.groupBy { it.reaction }.map { (emoji, list) ->
            ReactionSummary(
                reaction = emoji,
                count = list.size,
                includesMe = list.any { it.userId == DriverProfileEntity.LOCAL_USER_ID },
            )
        }

    private fun SocialChatEntity.toDomain(isMember: Boolean = true) = SocialChat(
        id = id,
        title = title,
        type = runCatching { ChatType.valueOf(type) }.getOrDefault(ChatType.GROUP),
        participantCount = participantCount,
        lastMessage = lastMessage,
        lastMessageAt = lastMessageAt,
        unreadCount = unreadCount,
        avatarEmoji = avatarEmoji,
        onlineCount = onlineCount,
        category = category,
        archived = archived,
        description = description,
        rating = rating,
        isPublic = isPublic,
        creatorId = creatorId,
        inviteCode = inviteCode,
        isMember = isMember,
    )

    private fun SocialMessageEntity.toDomain(
        isMine: Boolean,
        reactions: List<ReactionSummary> = emptyList(),
        replyPreview: String? = null,
    ) = SocialMessage(
        id = id,
        chatId = chatId,
        senderId = senderId,
        senderName = senderName,
        text = text,
        sentAt = sentAt,
        messageType = runCatching { MessageType.valueOf(messageType) }.getOrDefault(MessageType.TEXT),
        attachmentUrl = attachmentUrl,
        isMine = isMine,
        replyToId = replyToId,
        replyPreview = replyPreview,
        locationLabel = locationLabel,
        isAnnouncement = isAnnouncement,
        reactions = reactions,
        hashtags = ContentModerator.extractHashtags(text),
        durationMs = durationMs,
    )

    private fun DriverStatusEntity.toDomain() = DriverStatusPost(
        id = id,
        userId = userId,
        displayName = displayName,
        type = runCatching { StatusType.valueOf(type) }.getOrDefault(StatusType.TEXT),
        text = text,
        mediaPath = mediaPath,
        createdAt = createdAt,
        expiresAt = expiresAt,
        viewed = viewed,
        durationMs = durationMs,
    )

    companion object {
        const val MESSAGE_PAGE_SIZE = 50
        const val STATUS_TTL_MS = 24 * 60 * 60_000L
        const val WEEKLY_CHALLENGE_ID = "miles_week"
        private const val LOCAL_SENDER_ID = "me"
    }
}
