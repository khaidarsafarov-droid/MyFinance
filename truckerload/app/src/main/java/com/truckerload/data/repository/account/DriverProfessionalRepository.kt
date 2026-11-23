package com.truckerload.data.repository.account

import com.truckerload.data.local.dao.DriverProfessionalDao
import com.truckerload.data.local.entities.DriverProfessionalEntity
import com.truckerload.data.privacy.SensitiveFieldCipher
import com.truckerload.domain.account.DriverProfessionalProfile
import com.truckerload.domain.account.DriverProfileAccess
import com.truckerload.domain.account.DriverRole
import com.truckerload.domain.account.ProfessionalAccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Owner / dispatcher professional data. Not the community profile API.
 */
class DriverProfessionalRepository(
    private val dao: DriverProfessionalDao,
    private val cipher: SensitiveFieldCipher,
) {
    suspend fun getOwn(userId: String): DriverProfessionalProfile? {
        migrateLegacyPlaintext(userId)
        return dao.get(userId)?.toDomain(cipher)
    }

    fun watchOwn(userId: String): Flow<DriverProfessionalProfile?> = flow {
        migrateLegacyPlaintext(userId)
        emitAll(dao.watch(userId).map { it?.toDomain(cipher) })
    }

    suspend fun getForViewer(targetUserId: String, viewerId: String): ProfessionalAccess {
        migrateLegacyPlaintext(targetUserId)
        val profile = dao.get(targetUserId)?.toDomain(cipher)
        return DriverProfileAccess.resolve(profile, viewerId)
    }

    suspend fun upsertOwn(profile: DriverProfessionalProfile) {
        if (!cipher.canEncrypt()) {
            val existing = dao.get(profile.userId)
            val kept = profile.copy(cdlNumber = null, cdlDocumentUrl = null).toEntity(cipher)
            dao.upsert(
                kept.copy(
                    cdlNumberCiphertext = existing?.cdlNumberCiphertext,
                    cdlDocumentUrlCiphertext = existing?.cdlDocumentUrlCiphertext,
                ),
            )
            return
        }
        dao.upsert(profile.toEntity(cipher))
    }

    suspend fun delete(userId: String) {
        dao.delete(userId)
    }

    /**
     * Rewrites `plain:` or raw CDL values to `v1:` once, then the cipher
     * refuses plaintext. Skipped when secure storage cannot hold the key,
     * so an existing ciphertext is left untouched.
     */
    private suspend fun migrateLegacyPlaintext(userId: String) {
        if (!cipher.supportsLegacyMigration() || cipher.isLegacyRetired()) return
        val row = dao.get(userId) ?: return
        val number = settle(row.cdlNumberCiphertext, cipher.reencryptLegacy(row.cdlNumberCiphertext))
        val url = settle(row.cdlDocumentUrlCiphertext, cipher.reencryptLegacy(row.cdlDocumentUrlCiphertext))
        val saved = if (number != row.cdlNumberCiphertext || url != row.cdlDocumentUrlCiphertext) {
            val updated = row.copy(
                cdlNumberCiphertext = number,
                cdlDocumentUrlCiphertext = url,
            )
            dao.upsert(updated)
            updated
        } else {
            row
        }
        if (isSettled(saved.cdlNumberCiphertext) && isSettled(saved.cdlDocumentUrlCiphertext)) {
            cipher.retireLegacyPlaintext()
        }
    }

    private fun settle(stored: String?, rewritten: String?): String? {
        if (rewritten != null) return rewritten
        if (stored.isNullOrBlank() || stored == SensitiveFieldCipherPlain.BLANK) return null
        if (stored.startsWith("plain:") && stored.removePrefix("plain:").isBlank()) return null
        return stored
    }

    private fun isSettled(stored: String?): Boolean {
        if (stored.isNullOrBlank()) return true
        return stored.startsWith("v1:")
    }
}

private object SensitiveFieldCipherPlain {
    const val BLANK = "plain:"
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
