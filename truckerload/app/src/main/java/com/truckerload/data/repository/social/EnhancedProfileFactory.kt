package com.truckerload.data.repository.social

import com.truckerload.data.local.entities.DriverProfileEntity
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.domain.social.BadgeEngine
import com.truckerload.domain.social.DriverStatus
import com.truckerload.domain.social.EnhancedDriverProfile
import com.truckerload.domain.social.TruckType

internal object EnhancedProfileFactory {
    fun build(
        entity: DriverProfileEntity?,
        totalLoads: Int,
        totalMiles: Int,
        totalRevenue: Double,
        avatarUrl: String?,
        userProfileStore: UserProfileStore,
    ): EnhancedDriverProfile {
        val base = entity ?: DriverProfileEntity()
        val routes = base.routesJson.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val endorsements = base.endorsementsJson.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val specialties = base.specialtiesJson.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val languages = base.languagesJson
            .takeIf { it != "Русский,Английский" }
            .orEmpty()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val status = runCatching { DriverStatus.valueOf(base.status) }.getOrDefault(DriverStatus.OFFLINE)
        val averageRpm = if (totalMiles > 0) totalRevenue / totalMiles else 0.0
        val onTimePercentage = 0.0
        val about = base.about.takeIf {
            !it.contains("Дальнобойщик") && !it.contains("открытые дороги")
        }.orEmpty()
        val badges = BadgeEngine.compute(
            totalLoads = totalLoads,
            totalMiles = totalMiles,
            totalRevenue = totalRevenue,
            averageRpm = averageRpm,
            experienceYears = base.experienceYears,
            endorsements = endorsements,
            onTimePercentage = onTimePercentage,
        )
        val reputation = if (totalLoads > 0) badges.size * 25 + totalLoads.coerceAtMost(500) else 0
        val loginName = userProfileStore.profile.value?.displayName?.takeIf {
            it.isNotBlank() && it != userProfileStore.profile.value?.email
        }
        val displayName = base.displayName
            .takeIf { it.isNotBlank() && it !in setOf("Водитель", "Driver", "User") }
            ?: loginName.orEmpty()
        return EnhancedDriverProfile(
            id = base.id,
            displayName = displayName,
            avatarUrl = base.avatarUrl ?: avatarUrl,
            coverImageUrl = base.coverImageUrl,
            truckType = TruckType.fromLabel(base.truckType),
            experienceYears = base.experienceYears,
            licenseClass = base.licenseClass,
            endorsements = endorsements,
            homeState = base.homeState,
            preferredRoutes = routes,
            maxRadius = base.maxRadius,
            totalLoads = totalLoads,
            totalMiles = totalMiles,
            totalRevenue = totalRevenue,
            averageRpm = averageRpm,
            onTimePercentage = onTimePercentage,
            rating = 0.0,
            ratingCount = 0,
            reputation = reputation,
            badges = badges,
            followers = base.followers,
            following = base.following,
            status = status,
            currentRoute = base.currentRoute ?: routes.firstOrNull(),
            about = about,
            specialties = specialties,
            languages = languages,
            phoneNumber = base.phoneNumber,
            telegramUsername = base.telegramUsername,
            whatsappNumber = base.whatsappNumber,
            joinedDate = base.joinedDate,
            lastActive = base.lastActive.takeIf { it > 0 } ?: System.currentTimeMillis(),
            dateOfBirthEpochDay = base.dateOfBirthEpochDay,
            cdlNumber = base.cdlNumber,
            axleCount = base.axleCount,
            homeHubCity = base.homeHubCity,
        )
    }
}
