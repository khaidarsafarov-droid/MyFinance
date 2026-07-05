package com.truckerload.data.social

import com.truckerload.domain.social.Challenge
import com.truckerload.domain.social.EnhancedDriverProfile
import com.truckerload.domain.social.SocialChat

class RecommendationService {
    fun recommendGroups(chats: List<SocialChat>): List<SocialChat> =
        chats.filter { it.participantCount >= 30 }.sortedByDescending { it.rating }

    fun recommendChallenges(challenges: List<Challenge>): List<Challenge> =
        challenges.sortedByDescending { it.leaderboard.size }

    fun recommendDrivers(profiles: List<EnhancedDriverProfile>): List<EnhancedDriverProfile> =
        profiles.sortedByDescending { it.rating }
}
