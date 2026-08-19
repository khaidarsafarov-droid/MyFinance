package com.truckerload.data.repository.account

import com.truckerload.data.preferences.ConsentStore
import com.truckerload.data.preferences.RegistrationProgressStore
import com.truckerload.data.preferences.UserProfile
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.domain.account.AccountConsents
import com.truckerload.domain.account.CommunityProfile
import com.truckerload.domain.account.CommunityVisibilitySettings
import com.truckerload.domain.account.DriverProfessionalProfile
import com.truckerload.domain.account.DriverRole
import com.truckerload.domain.account.RegistrationProgress
import com.truckerload.domain.account.UserAccount
import kotlinx.coroutines.flow.StateFlow

class RegistrationService(
    private val userId: String,
    private val identity: AccountIdentityRepository,
    private val professional: DriverProfessionalRepository,
    private val community: CommunityProfileRepository,
    private val progressStore: RegistrationProgressStore,
    private val consentStore: ConsentStore,
    private val userProfileStore: UserProfileStore,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) {
    fun progress(): RegistrationProgress = progressStore.current()

    fun watchProgress(): StateFlow<RegistrationProgress> = progressStore.progress

    fun needsRequiredOnboarding(): Boolean = !progress().basicComplete

    fun consents(): AccountConsents = consentStore.load(userId)

    suspend fun completeCredentials(
        phone: String?,
        email: String?,
        authProvider: String,
        displayName: String,
        consents: AccountConsents,
        isVerified: Boolean,
    ): Result<UserAccount> {
        if (!consents.ageConfirmed) {
            return Result.failure(IllegalArgumentException("AGE_REQUIRED"))
        }
        if (!consents.tosAccepted) {
            return Result.failure(IllegalArgumentException("TOS_REQUIRED"))
        }
        val now = nowMillis()
        consentStore.save(
            userId = userId,
            tosAccepted = true,
            analyticsAccepted = consents.analyticsAccepted,
            ageConfirmed = true,
            nowMillis = now,
        )
        val account = UserAccount(
            id = userId,
            phone = phone?.takeIf { it.isNotBlank() },
            email = email?.takeIf { it.isNotBlank() },
            authProvider = authProvider,
            displayName = displayName.trim(),
            createdAt = now,
            isVerified = isVerified,
            ageConfirmed = true,
            acceptedTosAt = now,
            analyticsConsentAt = now.takeIf { consents.analyticsAccepted },
        )
        identity.upsert(account)
        progressStore.save(
            progress().copy(
                credentialsComplete = true,
                verificationComplete = isVerified,
            ),
        )
        return Result.success(account)
    }

    suspend fun markVerified() {
        val existing = identity.get(userId) ?: return
        identity.upsert(existing.copy(isVerified = true))
        progressStore.save(progress().copy(verificationComplete = true))
    }

    suspend fun skipVerificationForNow() {
        progressStore.save(progress().copy(verificationComplete = true))
    }

    suspend fun completeBasicProfile(
        displayName: String,
        role: DriverRole,
        phoneNumber: String? = null,
    ): Result<Unit> {
        val name = displayName.trim()
        if (name.isBlank()) return Result.failure(IllegalArgumentException("NAME_REQUIRED"))
        val now = nowMillis()
        val existing = identity.get(userId)
        identity.upsert(
            (existing ?: UserAccount(
                id = userId,
                phone = phoneNumber,
                email = null,
                authProvider = "EMAIL",
                displayName = name,
                createdAt = now,
                isVerified = false,
                ageConfirmed = true,
                acceptedTosAt = null,
                analyticsConsentAt = null,
            )).copy(
                displayName = name,
                phone = phoneNumber?.takeIf { it.isNotBlank() } ?: existing?.phone,
            ),
        )
        val pro = professional.getOwn(userId)
        professional.upsertOwn(
            (pro ?: DriverProfessionalProfile(
                userId = userId,
                role = role,
                companyName = null,
                cdlNumber = null,
                cdlDocumentUrl = null,
                vehicleType = "",
                primaryRegion = "",
                dispatcherUserId = null,
                skipped = false,
                updatedAt = now,
            )).copy(role = role, updatedAt = now),
        )
        val current = userProfileStore.profile.value
        val parts = name.split(" ", limit = 2)
        userProfileStore.saveProfile(
            (current ?: UserProfile(
                email = existing?.email.orEmpty(),
                givenName = "",
                familyName = "",
                photoUrl = null,
                phoneNumber = phoneNumber,
            )).copy(
                givenName = parts.firstOrNull().orEmpty(),
                familyName = parts.getOrNull(1).orEmpty(),
                phoneNumber = phoneNumber?.takeIf { it.isNotBlank() } ?: current?.phoneNumber,
            ),
        )
        userProfileStore.setSetupComplete(true)
        progressStore.save(
            progress().copy(
                credentialsComplete = true,
                basicComplete = true,
            ),
        )
        return Result.success(Unit)
    }

    suspend fun completeProfessional(
        companyName: String?,
        cdlNumber: String?,
        cdlDocumentUrl: String?,
        vehicleType: String,
        primaryRegion: String,
        dispatcherUserId: String? = null,
    ) {
        val now = nowMillis()
        val existing = professional.getOwn(userId)
        professional.upsertOwn(
            DriverProfessionalProfile(
                userId = userId,
                role = existing?.role ?: DriverRole.OWNER_OPERATOR,
                companyName = companyName?.takeIf { it.isNotBlank() },
                cdlNumber = cdlNumber?.takeIf { it.isNotBlank() },
                cdlDocumentUrl = cdlDocumentUrl?.takeIf { it.isNotBlank() },
                vehicleType = vehicleType.trim(),
                primaryRegion = primaryRegion.trim(),
                dispatcherUserId = dispatcherUserId?.takeIf { it.isNotBlank() } ?: existing?.dispatcherUserId,
                skipped = false,
                updatedAt = now,
            ),
        )
        progressStore.save(progress().withProfessionalDone(skipped = false))
    }

    suspend fun skipProfessional() {
        val now = nowMillis()
        val existing = professional.getOwn(userId)
        professional.upsertOwn(
            (existing ?: DriverProfessionalProfile(
                userId = userId,
                role = DriverRole.OWNER_OPERATOR,
                companyName = null,
                cdlNumber = null,
                cdlDocumentUrl = null,
                vehicleType = "",
                primaryRegion = "",
                dispatcherUserId = null,
                skipped = true,
                updatedAt = now,
            )).copy(skipped = true, updatedAt = now),
        )
        progressStore.save(progress().withProfessionalDone(skipped = true))
    }

    suspend fun completeCommunity(
        nickname: String,
        avatarUrl: String?,
        bio: String?,
        visibility: CommunityVisibilitySettings,
    ): Result<Unit> {
        val nick = nickname.trim()
        if (nick.isBlank()) return Result.failure(IllegalArgumentException("NICKNAME_REQUIRED"))
        val now = nowMillis()
        community.upsertOwn(
            CommunityProfile(
                userId = userId,
                nickname = nick,
                avatarUrl = avatarUrl,
                bio = bio?.takeIf { it.isNotBlank() },
                visibility = visibility,
                skipped = false,
                updatedAt = now,
            ),
        )
        userProfileStore.profile.value?.let {
            userProfileStore.saveProfile(it.copy(nickname = nick))
        }
        progressStore.save(progress().withCommunityDone(skipped = false))
        return Result.success(Unit)
    }

    suspend fun skipCommunity() {
        val now = nowMillis()
        val existing = community.getOwn(userId)
        community.upsertOwn(
            (existing ?: CommunityProfile(
                userId = userId,
                nickname = "",
                avatarUrl = null,
                bio = null,
                visibility = CommunityVisibilitySettings(),
                skipped = true,
                updatedAt = now,
            )).copy(skipped = true, updatedAt = now),
        )
        progressStore.save(progress().withCommunityDone(skipped = true))
    }
}
