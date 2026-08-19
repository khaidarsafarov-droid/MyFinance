package com.truckerload.data.social

import com.truckerload.domain.social.Challenge
import com.truckerload.domain.social.ChatType
import com.truckerload.domain.social.EnhancedDriverProfile
import com.truckerload.domain.social.SocialChat

class RecommendationService {
    fun recommendGroups(chats: List<SocialChat>): List<SocialChat> =
        chats.filter { it.type != ChatType.PRIVATE && it.participantCount >= 2 }
            .sortedByDescending { it.participantCount }

    fun recommendChallenges(challenges: List<Challenge>): List<Challenge> =
        challenges.sortedByDescending { it.leaderboard.size }

    fun recommendDrivers(profiles: List<EnhancedDriverProfile>): List<EnhancedDriverProfile> =
        profiles.sortedByDescending { it.rating }
}
