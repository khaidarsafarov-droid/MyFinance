package com.truckerload.data.repository.social

import com.truckerload.domain.social.DriverProfile
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
}
