package com.truckerload.data.repository.account

import com.truckerload.data.local.dao.CommunityProfileDao
import com.truckerload.data.local.entities.CommunityProfileEntity
import com.truckerload.domain.account.CommunityProfile
import com.truckerload.domain.account.CommunityVisibilitySettings
import com.truckerload.domain.account.PublicCommunityProfile
import com.truckerload.domain.account.toPublic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Public community profile. Separate from [DriverProfessionalRepository] —
 * never returns CDL / company / role.
 */
class CommunityProfileRepository(
    private val dao: CommunityProfileDao,
) {
    suspend fun getOwn(userId: String): CommunityProfile? = dao.get(userId)?.toDomain()

    fun watchOwn(userId: String): Flow<CommunityProfile?> =
        dao.watch(userId).map { it?.toDomain() }

    suspend fun getPublic(userId: String): PublicCommunityProfile? =
        dao.get(userId)?.toDomain()?.toPublic()

    fun watchPublic(userId: String): Flow<PublicCommunityProfile?> =
        dao.watch(userId).map { it?.toDomain()?.toPublic() }

    suspend fun upsertOwn(profile: CommunityProfile) {
        dao.upsert(profile.toEntity())
    }

    suspend fun delete(userId: String) {
        dao.delete(userId)
    }
}

internal fun CommunityProfileEntity.toDomain(): CommunityProfile = CommunityProfile(
    userId = userId,
    nickname = nickname,
    avatarUrl = avatarUrl,
    bio = bio,
    visibility = CommunityVisibilitySettings.fromJson(visibilityJson),
    skipped = skipped,
    updatedAt = updatedAt,
)

internal fun CommunityProfile.toEntity(): CommunityProfileEntity = CommunityProfileEntity(
    userId = userId,
    nickname = nickname,
    avatarUrl = avatarUrl,
    bio = bio,
    visibilityJson = visibility.toJson(),
    skipped = skipped,
    updatedAt = updatedAt,
)
