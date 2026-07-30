package com.truckerload.data.repository.social

import android.graphics.Bitmap
import com.truckerload.domain.social.Challenge
import com.truckerload.domain.social.DriverProfile
import com.truckerload.domain.social.DriverStatus
import com.truckerload.domain.social.EnhancedDriverProfile
import com.truckerload.domain.social.LeaderboardCategory
import com.truckerload.domain.social.LeaderboardEntry
import com.truckerload.domain.social.SocialPeerProfile
import com.truckerload.domain.social.SocialResult
import kotlinx.coroutines.flow.Flow

/**
 * Avatar, bio, visibility, follows/blocks, and profile-linked challenges.
 */
interface ProfileRepository {
    suspend fun syncIdentityFromUserProfile()
    suspend fun needsProfileSetup(): Boolean
    suspend fun maybeMarkSetupCompleteFromExistingProfile()
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
    ): SocialResult<Unit>
    suspend fun clearLocalIdentity()
    fun watchMyEnhancedProfile(): Flow<EnhancedDriverProfile>
    fun watchMyProfile(): Flow<DriverProfile>
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
    ): SocialResult<Unit>
    suspend fun uploadAvatar(bitmap: Bitmap): SocialResult<String>
    suspend fun removeAvatar(): SocialResult<Unit>
    suspend fun blockUser(blockedId: String): SocialResult<Unit>
    suspend fun unblockUser(blockedId: String): SocialResult<Unit>
    suspend fun isBlocked(targetId: String): Boolean
    fun watchIsBlocked(targetId: String): Flow<Boolean>
    suspend fun followDriver(targetId: String): SocialResult<Unit>
    suspend fun unfollowDriver(targetId: String): SocialResult<Unit>
    fun watchIsFollowing(targetId: String): Flow<Boolean>
    fun watchPeer(peerId: String): Flow<SocialPeerProfile?>
    suspend fun getPeer(peerId: String): SocialPeerProfile?
    fun watchLeaderboard(category: LeaderboardCategory = LeaderboardCategory.OVERALL): Flow<List<LeaderboardEntry>>
    suspend fun hasJoinedWeeklyChallenge(): Boolean
    suspend fun refreshMyChallengeScore()
    suspend fun joinWeeklyChallenge(): SocialResult<Unit>
    suspend fun weeklyChallenge(): Challenge
    suspend fun getLeaderboard(category: LeaderboardCategory): List<LeaderboardEntry>
}
