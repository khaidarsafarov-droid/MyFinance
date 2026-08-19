package com.truckerload.data.preferences

/** First-visit community explainers; dismissed after the user starts using that area. */
enum class CommunityHintArea(internal val prefKey: String) {
    CHATS("community_hint_used_chats"),
    LEADERBOARD("community_hint_used_leaderboard"),
    CHALLENGES("community_hint_used_challenges"),
    STATUS("community_hint_used_status"),
    GROUPS("community_hint_used_groups"),
}
