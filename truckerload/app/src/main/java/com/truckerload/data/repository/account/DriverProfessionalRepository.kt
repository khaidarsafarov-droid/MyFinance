package com.truckerload.data.repository.account

import com.truckerload.data.local.dao.DriverProfessionalDao
import com.truckerload.data.local.entities.DriverProfessionalEntity
import com.truckerload.data.privacy.SensitiveFieldCipher
import com.truckerload.domain.account.DriverProfessionalProfile
import com.truckerload.domain.account.DriverProfileAccess
import com.truckerload.domain.account.DriverRole
import com.truckerload.domain.account.ProfessionalAccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Owner / dispatcher professional data. Not the community profile API.
 */
class DriverProfessionalRepository(
    private val dao: DriverProfessionalDao,
    private val cipher: SensitiveFieldCipher,
) {
    suspend fun getOwn(userId: String): DriverProfessionalProfile? =
        dao.get(userId)?.toDomain(cipher)

    fun watchOwn(userId: String): Flow<DriverProfessionalProfile?> =
        dao.watch(userId).map { it?.toDomain(cipher) }

    suspend fun getForViewer(targetUserId: String, viewerId: String): ProfessionalAccess {
        val profile = dao.get(targetUserId)?.toDomain(cipher)
        return DriverProfileAccess.resolve(profile, viewerId)
    }

    suspend fun upsertOwn(profile: DriverProfessionalProfile) {
        dao.upsert(profile.toEntity(cipher))
    }

    suspend fun delete(userId: String) {
        dao.delete(userId)
    }
}

internal fun DriverProfessionalEntity.toDomain(cipher: SensitiveFieldCipher): DriverProfessionalProfile =
    DriverProfessionalProfile(
        userId = userId,
        role = DriverRole.fromStored(role),
        companyName = companyName?.takeIf { it.isNotBlank() },
        cdlNumber = cdlNumberCiphertext?.let { cipher.decrypt(it) }?.takeIf { it.isNotBlank() },
        cdlDocumentUrl = cdlDocumentUrlCiphertext?.let { cipher.decrypt(it) }?.takeIf { it.isNotBlank() },
        vehicleType = vehicleType,
        primaryRegion = primaryRegion,
        dispatcherUserId = dispatcherUserId?.takeIf { it.isNotBlank() },
        skipped = skipped,
        updatedAt = updatedAt,
    )

internal fun DriverProfessionalProfile.toEntity(cipher: SensitiveFieldCipher): DriverProfessionalEntity =
    DriverProfessionalEntity(
        userId = userId,
        role = role.name,
        companyName = companyName?.takeIf { it.isNotBlank() },
        cdlNumberCiphertext = cdlNumber?.takeIf { it.isNotBlank() }?.let { cipher.encrypt(it) },
        cdlDocumentUrlCiphertext = cdlDocumentUrl?.takeIf { it.isNotBlank() }?.let { cipher.encrypt(it) },
        vehicleType = vehicleType,
        primaryRegion = primaryRegion,
        dispatcherUserId = dispatcherUserId?.takeIf { it.isNotBlank() },
        skipped = skipped,
        updatedAt = updatedAt,
    )
