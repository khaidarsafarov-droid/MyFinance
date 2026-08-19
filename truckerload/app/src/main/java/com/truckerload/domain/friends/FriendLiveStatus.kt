package com.truckerload.domain.friends

/**
 * Messenger-style presence for a friend on the live map.
 *
 * Green / online — GPS ping within [ONLINE_WITHIN_MS].
 * Yellow / recently — ping within [RECENT_WITHIN_MS].
 * Gray / offline — older, missing, or never seen.
 */
enum class FriendLiveStatus {
    ONLINE,
    RECENT,
    OFFLINE,
    ;

    companion object {
        const val ONLINE_WITHIN_MS = 2 * 60 * 1000L
        const val RECENT_WITHIN_MS = 15 * 60 * 1000L

        fun fromUpdatedAt(
            updatedAtMillis: Long?,
            nowMillis: Long,
        ): FriendLiveStatus {
            if (updatedAtMillis == null || updatedAtMillis <= 0L) return OFFLINE
            val age = nowMillis - updatedAtMillis
            if (age <= ONLINE_WITHIN_MS) return ONLINE
            if (age <= RECENT_WITHIN_MS) return RECENT
            return OFFLINE
        }

        fun onlineCount(
            updatedAtMillis: Collection<Long?>,
            nowMillis: Long,
        ): Int = updatedAtMillis.count { fromUpdatedAt(it, nowMillis) == ONLINE }

        fun recentCount(
            updatedAtMillis: Collection<Long?>,
            nowMillis: Long,
        ): Int = updatedAtMillis.count { fromUpdatedAt(it, nowMillis) == RECENT }
    }
}

/** Online first, then recently online, then offline; nickname as a stable tie-breaker. */
fun sortShareLinksByLiveStatus(
    links: List<FriendShareLink>,
    updatedAtByUserId: Map<String, Long>,
    nowMillis: Long,
): List<FriendShareLink> = links.sortedWith(
    compareBy<FriendShareLink> { link ->
        FriendLiveStatus.fromUpdatedAt(updatedAtByUserId[link.friendUserId], nowMillis).ordinal
    }.thenBy { it.friendNickname.lowercase() },
)
