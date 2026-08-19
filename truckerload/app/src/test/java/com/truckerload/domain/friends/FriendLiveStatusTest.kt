package com.truckerload.domain.friends

import org.junit.Assert.assertEquals
import org.junit.Test

class FriendLiveStatusTest {

    private val now = 1_700_000_000_000L

    @Test
    fun fromUpdatedAt_mapsWindows() {
        assertEquals(FriendLiveStatus.OFFLINE, FriendLiveStatus.fromUpdatedAt(null, now))
        assertEquals(FriendLiveStatus.OFFLINE, FriendLiveStatus.fromUpdatedAt(0L, now))
        assertEquals(
            FriendLiveStatus.ONLINE,
            FriendLiveStatus.fromUpdatedAt(now - FriendLiveStatus.ONLINE_WITHIN_MS, now),
        )
        assertEquals(
            FriendLiveStatus.RECENT,
            FriendLiveStatus.fromUpdatedAt(now - FriendLiveStatus.ONLINE_WITHIN_MS - 1, now),
        )
        assertEquals(
            FriendLiveStatus.RECENT,
            FriendLiveStatus.fromUpdatedAt(now - FriendLiveStatus.RECENT_WITHIN_MS, now),
        )
        assertEquals(
            FriendLiveStatus.OFFLINE,
            FriendLiveStatus.fromUpdatedAt(now - FriendLiveStatus.RECENT_WITHIN_MS - 1, now),
        )
    }

    @Test
    fun fromUpdatedAt_futurePingCountsAsOnline() {
        assertEquals(FriendLiveStatus.ONLINE, FriendLiveStatus.fromUpdatedAt(now + 5_000L, now))
    }

    @Test
    fun countsOnlineAndRecent() {
        val stamps = listOf(
            now,
            now - 60_000L,
            now - 5 * 60_000L,
            now - 20 * 60_000L,
            null,
        )
        assertEquals(2, FriendLiveStatus.onlineCount(stamps, now))
        assertEquals(1, FriendLiveStatus.recentCount(stamps, now))
    }

    @Test
    fun sortShareLinksByLiveStatus_onlineFirst() {
        fun link(id: String, nick: String) = FriendShareLink(
            friendUserId = id,
            friendNickname = nick,
            friendDisplayName = nick,
            shareMyLocation = true,
            shareMyRoute = true,
        )
        val links = listOf(link("off", "Zed"), link("on", "Ann"), link("rec", "Bob"))
        val sorted = sortShareLinksByLiveStatus(
            links = links,
            updatedAtByUserId = mapOf(
                "on" to now,
                "rec" to now - 5 * 60_000L,
            ),
            nowMillis = now,
        )
        assertEquals(listOf("Ann", "Bob", "Zed"), sorted.map { it.friendNickname })
    }
}
