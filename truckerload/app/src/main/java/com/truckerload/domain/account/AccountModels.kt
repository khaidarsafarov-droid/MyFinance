package com.truckerload.domain.account

/**
 * Split identity: authentication and professional (private).
 * These types must not be mixed in a single API response.
 */

enum class DriverRole {
    OWNER_OPERATOR,
    HIRED_DRIVER,
    DISPATCHER,
    ;

    companion object {
        fun fromStored(value: String): DriverRole =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OWNER_OPERATOR
    }
}

data class UserAccount(
    val id: String,
    val phone: String?,
    val email: String?,
    val authProvider: String,
    val displayName: String,
    val createdAt: Long,
    val isVerified: Boolean,
    val ageConfirmed: Boolean,
    val acceptedTosAt: Long?,
    val analyticsConsentAt: Long?,
)

data class DriverProfessionalProfile(
    val userId: String,
    val role: DriverRole,
    val companyName: String?,
    val cdlNumber: String?,
    val cdlDocumentUrl: String?,
    val vehicleType: String,
    val primaryRegion: String,
    val dispatcherUserId: String?,
    val skipped: Boolean,
    val updatedAt: Long,
) {
    /** Owner-only secrets stripped for dispatcher / company viewers. */
    fun withoutSecrets(): DriverProfessionalProfile = copy(
        cdlNumber = null,
        cdlDocumentUrl = null,
    )

    val isComplete: Boolean
        get() = !skipped && (vehicleType.isNotBlank() || !cdlNumber.isNullOrBlank() || !companyName.isNullOrBlank())
}

sealed class ProfessionalAccess {
    data class Allowed(val profile: DriverProfessionalProfile) : ProfessionalAccess()
    data object Denied : ProfessionalAccess()
    data object NotFound : ProfessionalAccess()
}

object DriverProfileAccess {
    fun resolve(
        profile: DriverProfessionalProfile?,
        viewerId: String,
    ): ProfessionalAccess {
        if (profile == null) return ProfessionalAccess.NotFound
        val ownerId = profile.userId
        return when {
            viewerId == ownerId -> ProfessionalAccess.Allowed(profile)
            !profile.dispatcherUserId.isNullOrBlank() &&
                profile.dispatcherUserId == viewerId ->
                ProfessionalAccess.Allowed(profile.withoutSecrets())
            else -> ProfessionalAccess.Denied
        }
    }

    fun canView(profile: DriverProfessionalProfile?, viewerId: String): Boolean =
        resolve(profile, viewerId) is ProfessionalAccess.Allowed
}

object AgeGate {
    const val MINIMUM_AGE_YEARS = 18

    fun isAtLeast18(dateOfBirthEpochDay: Long, todayEpochDay: Long): Boolean {
        val minEpochDay = todayEpochDay - (MINIMUM_AGE_YEARS * 365L) - 5 // leap-day slack
        return dateOfBirthEpochDay <= minEpochDay
    }
}

enum class RegistrationStep {
    CREDENTIALS,
    VERIFICATION,
    BASIC_PROFILE,
    PROFESSIONAL,
    DONE,
}

data class RegistrationProgress(
    val credentialsComplete: Boolean = false,
    val verificationComplete: Boolean = false,
    val basicComplete: Boolean = false,
    val professionalComplete: Boolean = false,
    val professionalSkipped: Boolean = false,
) {
    val professionalPending: Boolean
        get() = basicComplete && !professionalComplete

    val nextRequired: RegistrationStep = when {
        !credentialsComplete -> RegistrationStep.CREDENTIALS
        !verificationComplete -> RegistrationStep.VERIFICATION
        !basicComplete -> RegistrationStep.BASIC_PROFILE
        else -> RegistrationStep.DONE
    }

    fun withProfessionalDone(skipped: Boolean): RegistrationProgress = copy(
        professionalComplete = !skipped,
        professionalSkipped = skipped,
    )
}

data class AccountConsents(
    val tosAccepted: Boolean = false,
    val analyticsAccepted: Boolean = false,
    val ageConfirmed: Boolean = false,
    val acceptedTosAt: Long? = null,
    val analyticsConsentAt: Long? = null,
)
