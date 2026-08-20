package com.truckerload.data.remote

import com.truckerload.domain.friends.FriendProfileHit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FriendProfileSearchHitParseTest {

    @Test
    fun parsesRpcArray() {
        val hit = SupabaseFriendsRealtimeService.parseSearchHit(
            """[{"user_id":"u-1","nickname":"DriverOne","full_name":"Ivan"}]""",
        )
        assertEquals(FriendProfileHit("u-1", "DriverOne", "DriverOne"), hit)
    }

    @Test
    fun parsesCommunityProfileRow() {
        val hit = SupabaseFriendsRealtimeService.parseSearchHit(
            """[{"user_id":"u-9","nickname":"RoadBoss"}]""",
        )
        assertEquals("u-9", hit?.userId)
        assertEquals("RoadBoss", hit?.nickname)
    }

    @Test
    fun emptyArrayIsNotFound() {
        assertNull(SupabaseFriendsRealtimeService.parseSearchHit("[]"))
        assertNull(SupabaseFriendsRealtimeService.parseSearchHit("  "))
    }
}
