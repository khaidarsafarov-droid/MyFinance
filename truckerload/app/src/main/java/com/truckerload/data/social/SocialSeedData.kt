package com.truckerload.data.social

import com.truckerload.data.local.entities.DriverProfileEntity
import com.truckerload.data.preferences.ProfileIdentity

/**
 * Ensures a local driver profile row exists. Does not seed chats, peers, or messages —
 * community starts empty until real friends join.
 */
object SocialSeedData {

    suspend fun ensureLocalProfile(
        profileDao: com.truckerload.data.local.dao.DriverProfileDao,
        defaultDisplayName: String,
        defaultAvatarUrl: String? = null,
        defaultPhone: String? = null,
        customPhoto: Boolean = false,
    ) {
        val existing = profileDao.getProfile()
        if (existing == null) {
            profileDao.upsert(
                DriverProfileEntity(
                    displayName = defaultDisplayName.takeIf { !ProfileIdentity.isPlaceholderName(it) }.orEmpty(),
                    avatarUrl = ProfileIdentity.mergeRoomAvatar(
                        existingAvatar = null,
                        providerPhotoUrl = defaultAvatarUrl,
                        customPhoto = customPhoto,
                    ),
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
                    displayName = ProfileIdentity.mergeRoomDisplayName(
                        existing.displayName,
                        defaultDisplayName,
                    ),
                    avatarUrl = ProfileIdentity.mergeRoomAvatar(
                        existingAvatar = existing.avatarUrl,
                        providerPhotoUrl = defaultAvatarUrl,
                        customPhoto = customPhoto,
                    ),
                    phoneNumber = existing.phoneNumber?.takeIf { it.isNotBlank() }
                        ?: defaultPhone?.takeIf { it.isNotBlank() },
                ),
            )
        }
    }
}
