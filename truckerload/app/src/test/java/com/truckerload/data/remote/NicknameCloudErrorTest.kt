package com.truckerload.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NicknameCloudErrorTest {

    @Test
    fun detectsTakenNickname() {
        assertTrue(
            SupabaseFriendsRealtimeService.isNicknameTakenError(
                "PATCH profiles HTTP 409: duplicate key value violates unique constraint \"profiles_nickname_lower_uidx\"",
            ),
        )
        assertTrue(SupabaseFriendsRealtimeService.isNicknameTakenError("23505 unique_violation"))
        assertFalse(SupabaseFriendsRealtimeService.isNicknameTakenError("network timeout"))
    }
}
