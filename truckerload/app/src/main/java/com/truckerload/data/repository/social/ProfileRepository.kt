package com.truckerload.data.repository.social

import com.truckerload.domain.social.DriverProfile
import com.truckerload.domain.social.DriverStatus
import com.truckerload.domain.social.EnhancedDriverProfile
import com.truckerload.domain.social.SocialResult
import kotlinx.coroutines.flow.Flow

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
    suspend fun updateStatus(status: DriverStatus): SocialResult<Unit>
    suspend fun uploadAvatar(bitmap: android.graphics.Bitmap): SocialResult<String>
    suspend fun removeAvatar(): SocialResult<Unit>
}
