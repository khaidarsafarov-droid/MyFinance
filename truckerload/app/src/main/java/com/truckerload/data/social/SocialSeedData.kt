package com.truckerload.data.social

import com.truckerload.data.local.entities.DriverProfileEntity

/**
 * Ensures a local driver profile row exists. Does not seed chats, peers, or messages —
 * community starts empty until real friends join.
 */
object SocialSeedData {

    private val PLACEHOLDER_NAMES = setOf("", "Водитель", "Driver", "User")

    suspend fun ensureLocalProfile(
        profileDao: com.truckerload.data.local.dao.DriverProfileDao,
        defaultDisplayName: String,
        defaultAvatarUrl: String? = null,
        defaultPhone: String? = null,
    ) {
        val existing = profileDao.getProfile()
        if (existing == null) {
            profileDao.upsert(
                DriverProfileEntity(
                    displayName = defaultDisplayName.takeIf { it !in PLACEHOLDER_NAMES }.orEmpty(),
                    avatarUrl = defaultAvatarUrl?.takeIf { it.isNotBlank() },
                    phoneNumber = defaultPhone?.takeIf { it.isNotBlank() },
                    truckType = "",
                    experienceYears = 0,
                    homeState = "",
                    routesJson = "",
                    about = "",
                    status = "ONLINE",
                ),
            )
        } else {
            profileDao.upsert(
                existing.copy(
                    displayName = when {
                        existing.displayName !in PLACEHOLDER_NAMES -> existing.displayName
                        defaultDisplayName !in PLACEHOLDER_NAMES -> defaultDisplayName
                        else -> existing.displayName
                    },
                    avatarUrl = existing.avatarUrl?.takeIf { it.isNotBlank() }
                        ?: defaultAvatarUrl?.takeIf { it.isNotBlank() },
                    phoneNumber = existing.phoneNumber?.takeIf { it.isNotBlank() }
                        ?: defaultPhone?.takeIf { it.isNotBlank() },
                ),
            )
        }
    }
}
