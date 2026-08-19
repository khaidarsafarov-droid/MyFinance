package com.truckerload.data.repository.social

import android.content.Context
import android.graphics.Bitmap
import com.truckerload.R
import com.truckerload.data.community.CommunityRemoteClient
import com.truckerload.data.community.FriendSafetyClient
import com.truckerload.data.local.dao.BlockedUserDao
import com.truckerload.data.local.dao.ChallengeParticipationDao
import com.truckerload.data.local.dao.DriverFollowDao
import com.truckerload.data.local.dao.DriverProfileDao
import com.truckerload.data.local.dao.SocialPeerDao
import com.truckerload.data.local.entities.BlockedUserEntity
import com.truckerload.data.local.entities.ChallengeParticipationEntity
import com.truckerload.data.local.entities.DriverFollowEntity
import com.truckerload.data.local.entities.DriverProfileEntity
import com.truckerload.data.local.entities.SocialPeerEntity
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.toPeerProfile
import com.truckerload.data.social.AvatarStorage
import com.truckerload.data.social.SocialMediaOptimizer
import com.truckerload.di.UserScope
import com.truckerload.domain.geo.CountryCatalog
import com.truckerload.domain.social.Challenge
import com.truckerload.domain.social.ChallengeType
import com.truckerload.domain.social.CommunityReportReason
import com.truckerload.domain.social.CommunityWeekWindow
import com.truckerload.domain.social.DriverProfile
import com.truckerload.domain.social.DriverStatus
import com.truckerload.domain.social.EnhancedDriverProfile
import com.truckerload.domain.social.LeaderboardCategory
import com.truckerload.domain.social.LeaderboardEntry
import com.truckerload.domain.social.SocialPeerProfile
import com.truckerload.domain.social.SocialResult
import com.truckerload.domain.social.toLegacyProfile
import com.truckerload.utils.getCurrentWeekNumberAndYear
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@UserScope
class ProfileRepositoryImpl(
    private val profileDao: DriverProfileDao,
    private val blockedUserDao: BlockedUserDao,
    private val followDao: DriverFollowDao,
    private val challengeDao: ChallengeParticipationDao,
    private val peerDao: SocialPeerDao,
    private val loadRepository: LoadRepository,
    private val userProfileStore: UserProfileStore,
    private val avatarStorage: AvatarStorage,
    private val appContext: Context,
    private val onPeerBlocked: suspend (String) -> Unit,
    private val actorId: () -> String,
    private val remote: CommunityRemoteClient,
    private val safety: FriendSafetyClient,
) : ProfileRepository {

    override suspend fun syncIdentityFromUserProfile() {
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

    override suspend fun needsProfileSetup(): Boolean {
        if (userProfileStore.setupComplete.value) return false
        val entity = profileDao.getProfile()
        val nameOk = !entity?.displayName.isNullOrBlank() &&
            entity?.displayName !in setOf("Водитель", "Driver", "User")
        val phoneOk = !entity?.phoneNumber.isNullOrBlank()
        val countryOk = CountryCatalog.byIso2(entity?.homeState) != null
        return !(nameOk && phoneOk && countryOk)
    }

    override suspend fun maybeMarkSetupCompleteFromExistingProfile() {
        if (userProfileStore.setupComplete.value) return
        val entity = profileDao.getProfile() ?: return
        val nameOk = entity.displayName.isNotBlank() &&
            entity.displayName !in setOf("Водитель", "Driver", "User")
        val phoneOk = !entity.phoneNumber.isNullOrBlank()
        val countryOk = CountryCatalog.byIso2(entity.homeState) != null
        if (nameOk && phoneOk && countryOk) {
            userProfileStore.setSetupComplete(true)
        }
    }

    override suspend fun completeProfileSetup(
        displayName: String,
        phoneNumber: String,
        homeCountryIso2: String,
        truckType: String,
        dateOfBirthEpochDay: Long?,
        licenseClass: String,
        cdlNumber: String,
        axleCount: Int,
        homeHubCity: String,
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
                dateOfBirthEpochDay = dateOfBirthEpochDay ?: existing.dateOfBirthEpochDay,
                licenseClass = licenseClass.trim().ifBlank { existing.licenseClass },
                cdlNumber = cdlNumber.trim().ifBlank { existing.cdlNumber },
                axleCount = if (axleCount > 0) axleCount else existing.axleCount,
                homeHubCity = homeHubCity.trim().ifBlank { existing.homeHubCity },
                about = existing.about.takeIf { !it.contains("Дальнобойщик") && !it.contains("открытые дороги") }.orEmpty(),
                ratingCount = 0,
                languagesJson = existing.languagesJson.takeIf { it != "Русский,Английский" }.orEmpty(),
                status = "ONLINE",
                lastActive = System.currentTimeMillis(),
            ),
        )
        runCatching {
            com.truckerload.sync.OutboundSyncQueue.enqueueProfileUpsert(
                appContext,
                existing.id,
                org.json.JSONObject()
                    .put("displayName", name)
                    .put("homeHubCity", homeHubCity.trim())
                    .put("licenseClass", licenseClass.trim()),
            )
        }
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
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_save_profile, it), it) }

    override suspend fun clearLocalIdentity() {
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
                licenseClass = "",
                cdlNumber = "",
                axleCount = 0,
                homeHubCity = "",
                dateOfBirthEpochDay = null,
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

    override fun watchMyEnhancedProfile(): Flow<EnhancedDriverProfile> =
        combine(
            profileDao.watchProfile(),
            loadRepository.watchTotalLoadStats(),
            userProfileStore.profile,
        ) { entity, stats, userProfile ->
            ProfileMapper.buildEnhancedProfile(
                entity,
                stats.totalLoads,
                stats.totalMiles.toInt(),
                stats.totalRevenue,
                userProfile?.photoUrl,
                userProfileStore,
            )
        }.flowOn(Dispatchers.IO)

    override fun watchMyProfile(): Flow<DriverProfile> =
        watchMyEnhancedProfile().map { it.toLegacyProfile() }.flowOn(Dispatchers.IO)

    override suspend fun updateProfile(
        displayName: String,
        truckType: String,
        experienceYears: Int,
        homeState: String,
        routes: List<String>,
        about: String,
        status: DriverStatus,
        licenseClass: String,
        endorsements: List<String>,
        specialties: List<String>,
        phoneNumber: String?,
        telegramUsername: String?,
        whatsappNumber: String?,
        maxRadius: Int,
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
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_save_profile, it), it) }

    override suspend fun updateStatus(status: DriverStatus): SocialResult<Unit> = runCatching {
        val existing = profileDao.getProfile() ?: DriverProfileEntity()
        profileDao.upsert(existing.copy(status = status.name))
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_update_status, it), it) }

    override suspend fun uploadAvatar(bitmap: Bitmap): SocialResult<String> = runCatching {
        val compressed = SocialMediaOptimizer.compressImage(bitmap)
        val path = avatarStorage.saveAvatar(DriverProfileEntity.LOCAL_USER_ID, compressed)
        val existing = profileDao.getProfile() ?: DriverProfileEntity()
        avatarStorage.deleteAvatar(existing.avatarUrl)
        profileDao.upsert(existing.copy(avatarUrl = path))
        SocialResult.Success(path)
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_upload_avatar, it), it) }

    override suspend fun removeAvatar(): SocialResult<Unit> = runCatching {
        val existing = profileDao.getProfile() ?: DriverProfileEntity()
        avatarStorage.deleteAvatar(existing.avatarUrl)
        profileDao.upsert(existing.copy(avatarUrl = null))
        userProfileStore.profile.value?.let { profile ->
            if (!profile.photoUrl.isNullOrBlank()) {
                userProfileStore.saveProfile(profile.copy(photoUrl = null))
            }
        }
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_upload_avatar, it), it) }

    override suspend fun blockUser(blockedId: String): SocialResult<Unit> = runCatching {
        val me = actorId()
        blockedUserDao.block(
            BlockedUserEntity(
                blockerId = me,
                blockedId = blockedId,
                blockedAt = System.currentTimeMillis(),
            ),
        )
        followDao.unfollow(me, blockedId)
        onPeerBlocked(blockedId)
        if (remote.isReady()) remote.blockUser(blockedId)
        updateFollowCounts()
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_block_user, it), it) }

    override suspend fun unblockUser(blockedId: String): SocialResult<Unit> = runCatching {
        val me = actorId()
        blockedUserDao.unblock(me, blockedId)
        if (remote.isReady()) remote.unblockUser(blockedId)
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_unblock_user, it), it) }

    override suspend fun isBlocked(targetId: String): Boolean =
        blockedUserDao.isBlocked(actorId(), targetId)

    override fun watchIsBlocked(targetId: String): Flow<Boolean> =
        blockedUserDao.watchBlockedIds(actorId())
            .map { blockedIds -> targetId in blockedIds }.flowOn(Dispatchers.IO)

    override suspend fun reportUser(
        reportedUserId: String,
        reason: CommunityReportReason,
        details: String,
        chatId: String?,
    ): SocialResult<Unit> = runCatching {
        if (reportedUserId == actorId()) {
            return SocialResult.Error(appContext.getString(R.string.social_error_cannot_report_self))
        }
        safety.submitReport(
            reportedUserId = reportedUserId,
            reason = reason,
            details = details,
            chatId = chatId,
        ).getOrThrow()
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_report_user, it), it) }

    override suspend fun followDriver(targetId: String): SocialResult<Unit> = runCatching {
        val me = actorId()
        if (targetId == me) {
            return SocialResult.Error(appContext.getString(R.string.social_error_cannot_follow_self))
        }
        followDao.follow(
            DriverFollowEntity(
                followerId = me,
                followingId = targetId,
                followedAt = System.currentTimeMillis(),
            ),
        )
        updateFollowCounts()
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_follow, it), it) }

    override suspend fun unfollowDriver(targetId: String): SocialResult<Unit> = runCatching {
        followDao.unfollow(actorId(), targetId)
        updateFollowCounts()
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_unfollow, it), it) }

    override fun watchIsFollowing(targetId: String): Flow<Boolean> =
        followDao.watchIsFollowing(actorId(), targetId).flowOn(Dispatchers.IO)

    override fun watchPeer(peerId: String): Flow<SocialPeerProfile?> =
        peerDao.watchById(peerId).map { entity -> entity?.toPeerProfile() }.flowOn(Dispatchers.IO)

    override suspend fun getPeer(peerId: String): SocialPeerProfile? =
        peerDao.getById(peerId)?.toPeerProfile()

    override fun watchLeaderboard(category: LeaderboardCategory): Flow<List<LeaderboardEntry>> {
        val (week, year) = getCurrentWeekNumberAndYear()
        return combine(
            loadRepository.watchWeeklyLoadStats(week, year),
            peerDao.watchAll(),
            blockedUserDao.watchBlockedIds(actorId()),
        ) { weekStats, peers, blockedIds ->
            val blockedSet = blockedIds.toSet()
            buildLeaderboard(weekStats, peers.filter { it.id !in blockedSet }, category)
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getLeaderboard(category: LeaderboardCategory): List<LeaderboardEntry> {
        val (week, year) = getCurrentWeekNumberAndYear()
        val weekStats = loadRepository.getWeeklyLoadStatsOnce(week, year)
        val peers = peerDao.getAll()
        val blockedIds = blockedUserDao.watchBlockedIds(actorId()).first().toSet()
        return buildLeaderboard(weekStats, peers.filter { it.id !in blockedIds }, category)
    }

    override suspend fun hasJoinedWeeklyChallenge(): Boolean =
        challengeDao.getParticipation(SocialConstants.WEEKLY_CHALLENGE_ID, actorId()) != null

    override suspend fun refreshMyChallengeScore() {
        val window = CommunityWeekWindow.current()
        val stats = loadRepository.getWeeklyLoadStatsOnce(window.week, window.year)
        val me = actorId()
        val existing = challengeDao.getParticipation(SocialConstants.WEEKLY_CHALLENGE_ID, me)
        if (existing != null) {
            challengeDao.updateScore(SocialConstants.WEEKLY_CHALLENGE_ID, me, stats.totalMiles)
        }
        val rpm = if (stats.totalMiles > 0) stats.totalRevenue / stats.totalMiles else 0.0
        if (remote.isReady()) {
            remote.upsertWeeklyStats(
                window = window,
                miles = stats.totalMiles,
                loads = stats.loadCount,
                revenue = stats.totalRevenue,
                rpm = rpm,
                shareEnabled = existing != null,
            )
        }
    }

    override suspend fun joinWeeklyChallenge(): SocialResult<Unit> = runCatching {
        val window = CommunityWeekWindow.current()
        val stats = loadRepository.getWeeklyLoadStatsOnce(window.week, window.year)
        val me = actorId()
        challengeDao.join(
            ChallengeParticipationEntity(
                challengeId = SocialConstants.WEEKLY_CHALLENGE_ID,
                userId = me,
                score = stats.totalMiles,
                joinedAt = System.currentTimeMillis(),
            ),
        )
        if (remote.isReady()) {
            remote.setShareWeeklyStats(true)
            remote.joinChallenge(SocialConstants.WEEKLY_CHALLENGE_ID, stats.totalMiles)
                .getOrElse { err ->
                    return SocialResult.Error(
                        socialError(
                            appContext,
                            R.string.social_error_join_chat,
                            err
                        ), err
                    )
                }
            remote.upsertWeeklyStats(
                window = window,
                miles = stats.totalMiles,
                loads = stats.loadCount,
                revenue = stats.totalRevenue,
                rpm = if (stats.totalMiles > 0) stats.totalRevenue / stats.totalMiles else 0.0,
                shareEnabled = true,
            )
        }
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_join_chat, it), it) }

    override suspend fun weeklyChallenge(): Challenge {
        val window = CommunityWeekWindow.current()
        val weekStats = loadRepository.getWeeklyLoadStatsOnce(window.week, window.year)
        val myMiles = weekStats.totalMiles
        val myName = profileDao.getProfile()?.displayName
            ?: userProfileStore.profile.value?.displayName
            ?: "You"
        refreshMyChallengeScore()
        val me = actorId()
        val participation = challengeDao.getParticipation(SocialConstants.WEEKLY_CHALLENGE_ID, me)
        val score = participation?.score ?: myMiles
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
            rating = 0.0,
            trend = if (score > 0) "⬆" else "—",
            isMe = true,
            userId = me,
        )
        val merged = (peerBoard + myEntry).sortedByDescending { it.score }
            .mapIndexed { index, entry -> entry.copy(rank = index + 1) }
        val myPosition = merged.indexOfFirst { it.isMe }.let { if (it >= 0) it + 1 else merged.size }
        return Challenge(
            id = SocialConstants.WEEKLY_CHALLENGE_ID,
            title = appContext.getString(R.string.social_challenge_king_miles_title),
            description = appContext.getString(
                R.string.social_challenge_king_miles_desc,
                window.week,
                window.year
            ),
            type = ChallengeType.MILES,
            goal = 3000.0,
            startDate = window.startMillis,
            endDate = window.endMillis,
            leaderboard = merged.take(10),
            myPosition = myPosition,
            myScore = score,
        )
    }

    private fun buildLeaderboard(
        weekStats: com.truckerload.data.local.entities.WeeklyLoadStatsAgg,
        peers: List<SocialPeerEntity>,
        category: LeaderboardCategory,
    ): List<LeaderboardEntry> {
        val localName = userProfileStore.profile.value?.displayName ?: "You"
        return ProfileMapper.buildLeaderboard(weekStats, peers, category, localName)
    }

    private suspend fun updateFollowCounts() {
        val existing = profileDao.getProfile() ?: return
        profileDao.upsert(
            existing.copy(
                followers = followDao.countFollowers(actorId()),
                following = followDao.countFollowing(actorId()),
            ),
        )
    }
}
