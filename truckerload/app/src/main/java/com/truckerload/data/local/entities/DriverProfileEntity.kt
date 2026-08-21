package com.truckerload.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "driver_profile")
data class DriverProfileEntity(
    @PrimaryKey val id: String = LOCAL_USER_ID,
    val displayName: String = "",
    val avatarUrl: String? = null,
    val coverImageUrl: String? = null,
    val truckType: String = "",
    val experienceYears: Int = 0,
    val licenseClass: String = "",
    val endorsementsJson: String = "",
    val homeState: String = "",
    val routesJson: String = "",
    val maxRadius: Int = 500,
    val about: String = "",
    val specialtiesJson: String = "",
    val languagesJson: String = "",
    val phoneNumber: String? = null,
    val telegramUsername: String? = null,
    val whatsappNumber: String? = null,
    val reputation: Int = 0,
    val followers: Int = 0,
    val following: Int = 0,
    val ratingCount: Int = 0,
    val currentRoute: String? = null,
    val status: String = "OFFLINE",
    val joinedDate: Long = System.currentTimeMillis(),
    val lastActive: Long = System.currentTimeMillis(),
    /** Epoch day (LocalDate.toEpochDay) for HOS / age checks; null if unset. */
    val dateOfBirthEpochDay: Long? = null,
    /** CDL / commercial license number (optional). */
    val cdlNumber: String = "",
    /** Tractor axle count for weight calculations (0 = unset). */
    val axleCount: Int = 0,
    /** Home terminal / logistics hub city. */
    val homeHubCity: String = "",
) {
    companion object {
        const val LOCAL_USER_ID = "local_user"
    }
}
