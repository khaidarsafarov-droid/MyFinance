package com.truckerload.data.repository.social

import android.content.Context
import com.truckerload.R
import com.truckerload.data.local.dao.DriverProfileDao
import com.truckerload.data.local.entities.DriverProfileEntity
import com.truckerload.data.preferences.ProfileIdentity
import com.truckerload.data.preferences.UserProfile
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.repository.LoadRepository
import com.truckerload.di.UserScope
import com.truckerload.domain.geo.CountryCatalog
import com.truckerload.domain.social.DriverProfile
import com.truckerload.domain.social.EnhancedDriverProfile
import com.truckerload.domain.social.SocialResult
import com.truckerload.domain.social.toLegacyProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@UserScope
class ProfileRepositoryImpl(
    private val profileDao: DriverProfileDao,
    private val loadRepository: LoadRepository,
    private val userProfileStore: UserProfileStore,
    private val appContext: Context,
) : ProfileRepository {

    override suspend fun syncIdentityFromUserProfile() {
        val user = userProfileStore.profile.value ?: return
        val existing = profileDao.getProfile() ?: DriverProfileEntity()
        val loginName = user.displayName.takeIf { it.isNotBlank() && it != user.email }.orEmpty()
        val mergedName = if (user.customDisplayName && !ProfileIdentity.isPlaceholderName(existing.displayName)) {
            existing.displayName
        } else {
            ProfileIdentity.mergeRoomDisplayName(existing.displayName, loginName)
        }
        val mergedPhone = existing.phoneNumber?.takeIf { it.isNotBlank() }
            ?: user.phoneNumber?.takeIf { it.isNotBlank() }
        val mergedAvatar = ProfileIdentity.mergeRoomAvatar(
            existingAvatar = existing.avatarUrl,
            providerPhotoUrl = user.photoUrl,
            customPhoto = user.customPhoto,
        )
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
                cdlNumber = "",
                axleCount = if (axleCount > 0) axleCount else existing.axleCount,
                homeHubCity = homeHubCity.trim().ifBlank { existing.homeHubCity },
                about = existing.about.takeIf {
                    !it.contains("Дальнобойщик") && !it.contains("открытые дороги")
                }.orEmpty(),
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
        val parts = name.split(" ", limit = 2)
        if (current != null) {
            userProfileStore.saveProfile(
                current.copy(
                    givenName = parts.firstOrNull().orEmpty(),
                    familyName = parts.getOrNull(1).orEmpty(),
                    phoneNumber = phone,
                    customDisplayName = true,
                ),
            )
        } else {
            userProfileStore.saveProfile(
                UserProfile(
                    email = "",
                    givenName = parts.firstOrNull().orEmpty(),
                    familyName = parts.getOrNull(1).orEmpty(),
                    photoUrl = null,
                    phoneNumber = phone,
                    customDisplayName = true,
                ),
            )
        }
        userProfileStore.setSetupComplete(true)
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_save_profile, it), it) }

    override suspend fun updateOwnName(givenName: String, familyName: String) {
        val given = givenName.trim()
        val family = familyName.trim()
        val display = listOf(given, family).filter { it.isNotBlank() }.joinToString(" ")
        if (ProfileIdentity.isPlaceholderName(display)) return
        val existing = profileDao.getProfile() ?: DriverProfileEntity()
        profileDao.upsert(
            existing.copy(
                displayName = display,
                lastActive = System.currentTimeMillis(),
            ),
        )
        val current = userProfileStore.profile.value
        if (current != null) {
            userProfileStore.saveProfile(
                current.copy(
                    givenName = given,
                    familyName = family,
                    customDisplayName = true,
                ),
            )
        }
    }

    override suspend fun clearLocalIdentity() {
        val existing = profileDao.getProfile() ?: return
        existing.avatarUrl
            ?.takeIf { it.isNotBlank() && !it.startsWith("http") }
            ?.let { path -> runCatching { java.io.File(path).takeIf { it.exists() }?.delete() } }
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
}

private fun socialError(context: Context, resId: Int, error: Throwable): String =
    context.getString(resId) + ": " + (error.message ?: error.toString())
