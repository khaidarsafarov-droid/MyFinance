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
}
