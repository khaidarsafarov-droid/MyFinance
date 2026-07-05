package com.truckerload.domain.social

fun EnhancedDriverProfile.toLegacyProfile(): DriverProfile = DriverProfile(
    id = id,
    displayName = displayName,
    avatarUrl = avatarUrl,
    truckType = truckType.label,
    experienceYears = experienceYears,
    homeState = homeState,
    routes = preferredRoutes,
    rating = rating,
    totalLoads = totalLoads,
    totalMiles = totalMiles,
    totalRevenue = totalRevenue,
    status = status,
    about = about,
    badges = badges,
    joinedDate = joinedDate,
)
