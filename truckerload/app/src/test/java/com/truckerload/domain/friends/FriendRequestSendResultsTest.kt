package com.truckerload.domain.friends

import org.junit.Assert.assertEquals
import org.junit.Test

class FriendRequestSendResultsTest {

    @Test
    fun mapsRpcStatuses() {
        assertEquals(FriendRequestSendResult.SENT, FriendRequestSendResults.fromRpc("sent"))
        assertEquals(FriendRequestSendResult.SENT, FriendRequestSendResults.fromRpc("\"sent\""))
        assertEquals(FriendRequestSendResult.ALREADY_SENT, FriendRequestSendResults.fromRpc("already_sent"))
        assertEquals(FriendRequestSendResult.ALREADY_FRIENDS, FriendRequestSendResults.fromRpc("already_friends"))
        assertEquals(FriendRequestSendResult.ACCEPTED, FriendRequestSendResults.fromRpc("accepted"))
        assertEquals(FriendRequestSendResult.BLOCKED, FriendRequestSendResults.fromRpc("blocked"))
    }

    @Test
    fun establishesFriendshipOnlyAfterMutualLink() {
        assertEquals(false, FriendRequestSendResult.SENT.establishesFriendship())
        assertEquals(false, FriendRequestSendResult.ALREADY_SENT.establishesFriendship())
        assertEquals(false, FriendRequestSendResult.BLOCKED.establishesFriendship())
        assertEquals(true, FriendRequestSendResult.ADDED_DIRECT.establishesFriendship())
        assertEquals(true, FriendRequestSendResult.ACCEPTED.establishesFriendship())
        assertEquals(true, FriendRequestSendResult.ALREADY_FRIENDS.establishesFriendship())
    }

    @Test
    fun statusKeyAndCommunityLabel() {
        assertEquals("request_sent", FriendRequestSendResult.SENT.statusKey())
        assertEquals("added", FriendRequestSendResult.ADDED_DIRECT.statusKey())
        assertEquals("@Nick_1", friendCommunityLabel("Nick_1", "Ivan"))
        assertEquals("@Nick_1", friendCommunityLabel("@Nick_1"))
        assertEquals("Ivan", friendCommunityLabel("", "Ivan"))
        assertEquals("Driver", friendCommunityLabel("  ", "  "))
    }
}
