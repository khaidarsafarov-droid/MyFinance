package com.truckerload.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "user_accounts")
data class UserAccountEntity(
    @PrimaryKey val id: String,
    val phone: String? = null,
    val email: String? = null,
    val authProvider: String = "EMAIL",
    val displayName: String = "",
    val createdAt: Long = 0L,
    val isVerified: Boolean = false,
    val ageConfirmed: Boolean = false,
    val acceptedTosAt: Long? = null,
    val analyticsConsentAt: Long? = null,
)

@Entity(
    tableName = "driver_professional_profiles",
    indices = [Index(value = ["dispatcherUserId"])],
)
data class DriverProfessionalEntity(
    @PrimaryKey val userId: String,
    val role: String = "OWNER_OPERATOR",
    val companyName: String? = null,
    /** AES-GCM ciphertext (`v1:`) or SQL-migration `plain:` prefix. Never send to community APIs. */
    val cdlNumberCiphertext: String? = null,
    val cdlDocumentUrlCiphertext: String? = null,
    val vehicleType: String = "",
    val primaryRegion: String = "",
    val dispatcherUserId: String? = null,
    val skipped: Boolean = false,
    val updatedAt: Long = 0L,
)
