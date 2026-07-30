package com.truckerload.data.repository.social

import android.content.Context
import android.graphics.Bitmap
import com.truckerload.R
import com.truckerload.data.local.dao.BlockedUserDao
import com.truckerload.data.local.dao.DriverFollowDao
import com.truckerload.data.local.dao.DriverProfileDao
import com.truckerload.data.local.dao.SocialChatDao
import com.truckerload.data.local.dao.SocialPeerDao
import com.truckerload.data.local.entities.BlockedUserEntity
import com.truckerload.data.local.entities.DriverFollowEntity
import com.truckerload.data.local.entities.DriverProfileEntity
import com.truckerload.data.preferences.UserProfile
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.toPeerProfile
import com.truckerload.data.social.AvatarStorage
import com.truckerload.data.social.SocialMediaOptimizer
import com.truckerload.domain.geo.CountryCatalog
import com.truckerload.domain.social.DriverProfile
import com.truckerload.domain.social.DriverStatus
import com.truckerload.domain.social.EnhancedDriverProfile
import com.truckerload.domain.social.SocialPeerProfile
import com.truckerload.domain.social.SocialResult
import com.truckerload.domain.social.toLegacyProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * Driver identity, avatar, presence, blocks, follows, and peer lookups.
 */
class ProfileRepository(
    private val profileDao: DriverProfileDao,
    private val blockedUserDao: BlockedUserDao,
    private val followDao: DriverFollowDao,
    private val peerDao: SocialPeerDao,
    private val chatDao: SocialChatDao,
    private val loadRepository: LoadRepository,
    private val userProfileStore: UserProfileStore,
    private val avatarStorage: AvatarStorage,
    context: Context,
) {
    private val appContext = context.applicationContext

    /** Copies login identity into the Room driver profile without clobbering local edits. */
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

    suspend fun needsProfileSetup(): Boolean {
        if (userProfileStore.setupComplete.value) return false
        val entity = profileDao.getProfile()
        val nameOk = !entity?.displayName.isNullOrBlank() &&
            entity?.displayName !in setOf("Водитель", "Driver", "User")
        val phoneOk = !entity?.phoneNumber.isNullOrBlank()
        val countryOk = CountryCatalog.byIso2(entity?.homeState) != null
        return !(nameOk && phoneOk && countryOk)
    }

    suspend fun maybeMarkSetupCompleteFromExistingProfile() {
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
                languagesJson = existing.languagesJson.takeIf {
                    it != "Русский,Английский"
                }.orEmpty(),
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
                UserProfile(
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
    }.getOrElse { SocialResult.Error(appContext.socialError(R.string.social_error_save_profile, it), it) }

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

    fun watchMyEnhancedProfile(): Flow<EnhancedDriverProfile> =
        combine(
            profileDao.watchProfile(),
            loadRepository.watchTotalLoadStats(),
            userProfileStore.profile,
        ) { entity, stats, userProfile ->
            EnhancedProfileFactory.build(
                entity = entity,
                totalLoads = stats.totalLoads,
                totalMiles = stats.totalMiles.toInt(),
                totalRevenue = stats.totalRevenue,
                avatarUrl = userProfile?.photoUrl,
                userProfileStore = userProfileStore,
            )
        }.flowOn(Dispatchers.IO)

    fun watchMyProfile(): Flow<DriverProfile> =
        watchMyEnhancedProfile().map { it.toLegacyProfile() }.flowOn(Dispatchers.IO)

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
    }.getOrElse { SocialResult.Error(appContext.socialError(R.string.social_error_save_profile, it), it) }

    suspend fun updateStatus(status: DriverStatus): SocialResult<Unit> = runCatching {
        val existing = profileDao.getProfile() ?: DriverProfileEntity()
        profileDao.upsert(existing.copy(status = status.name))
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(appContext.socialError(R.string.social_error_update_status, it), it) }

    suspend fun uploadAvatar(bitmap: Bitmap): SocialResult<String> = runCatching {
        val compressed = SocialMediaOptimizer.compressImage(bitmap)
        val path = avatarStorage.saveAvatar(DriverProfileEntity.LOCAL_USER_ID, compressed)
        val existing = profileDao.getProfile() ?: DriverProfileEntity()
        avatarStorage.deleteAvatar(existing.avatarUrl)
        profileDao.upsert(existing.copy(avatarUrl = path))
        SocialResult.Success(path)
    }.getOrElse { SocialResult.Error(appContext.socialError(R.string.social_error_upload_avatar, it), it) }

    suspend fun removeAvatar(): SocialResult<Unit> = runCatching {
        val existing = profileDao.getProfile() ?: DriverProfileEntity()
        avatarStorage.deleteAvatar(existing.avatarUrl)
        profileDao.upsert(existing.copy(avatarUrl = null))
        userProfileStore.profile.value?.let { profile ->
            if (!profile.photoUrl.isNullOrBlank()) {
                userProfileStore.saveProfile(profile.copy(photoUrl = null))
            }
        }
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(appContext.socialError(R.string.social_error_upload_avatar, it), it) }

    suspend fun blockUser(blockedId: String): SocialResult<Unit> = runCatching {
        blockedUserDao.block(
            BlockedUserEntity(
                blockerId = DriverProfileEntity.LOCAL_USER_ID,
                blockedId = blockedId,
                blockedAt = System.currentTimeMillis(),
            ),
        )
        followDao.unfollow(DriverProfileEntity.LOCAL_USER_ID, blockedId)
        chatDao.findPrivateChatForPeer(peerDao, blockedId)?.let { chatDao.archiveChat(it.id) }
        updateFollowCounts()
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(appContext.socialError(R.string.social_error_block_user, it), it) }

    suspend fun unblockUser(blockedId: String): SocialResult<Unit> = runCatching {
        blockedUserDao.unblock(DriverProfileEntity.LOCAL_USER_ID, blockedId)
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(appContext.socialError(R.string.social_error_unblock_user, it), it) }

    suspend fun isBlocked(targetId: String): Boolean =
        blockedUserDao.isBlocked(DriverProfileEntity.LOCAL_USER_ID, targetId)

    fun watchIsBlocked(targetId: String): Flow<Boolean> =
        blockedUserDao.watchBlockedIds(DriverProfileEntity.LOCAL_USER_ID)
            .map { blockedIds -> targetId in blockedIds }.flowOn(Dispatchers.IO)

    suspend fun followDriver(targetId: String): SocialResult<Unit> = runCatching {
        if (targetId == DriverProfileEntity.LOCAL_USER_ID) {
            return SocialResult.Error(appContext.getString(R.string.social_error_cannot_follow_self))
        }
        followDao.follow(
            DriverFollowEntity(
                followerId = DriverProfileEntity.LOCAL_USER_ID,
                followingId = targetId,
                followedAt = System.currentTimeMillis(),
            ),
        )
        updateFollowCounts()
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(appContext.socialError(R.string.social_error_follow, it), it) }

    suspend fun unfollowDriver(targetId: String): SocialResult<Unit> = runCatching {
        followDao.unfollow(DriverProfileEntity.LOCAL_USER_ID, targetId)
        updateFollowCounts()
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(appContext.socialError(R.string.social_error_unfollow, it), it) }

    fun watchIsFollowing(targetId: String): Flow<Boolean> =
        followDao.watchIsFollowing(DriverProfileEntity.LOCAL_USER_ID, targetId).flowOn(Dispatchers.IO)

    fun watchPeer(peerId: String): Flow<SocialPeerProfile?> =
        peerDao.watchById(peerId).map { entity -> entity?.toPeerProfile() }.flowOn(Dispatchers.IO)

    suspend fun getPeer(peerId: String): SocialPeerProfile? =
        peerDao.getById(peerId)?.toPeerProfile()

    suspend fun displayNameOrBlank(): String =
        profileDao.getProfile()?.displayName.orEmpty()

    private suspend fun updateFollowCounts() {
        val existing = profileDao.getProfile() ?: return
        profileDao.upsert(
            existing.copy(
                followers = followDao.countFollowers(DriverProfileEntity.LOCAL_USER_ID),
                following = followDao.countFollowing(DriverProfileEntity.LOCAL_USER_ID),
            ),
        )
    }
}
