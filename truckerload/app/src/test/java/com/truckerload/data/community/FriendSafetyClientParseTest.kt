package com.truckerload.data.community

import com.truckerload.domain.friends.FriendRequestDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class FriendSafetyClientParseTest {

    @Test
    fun parsesIncomingAndOutgoingRequests() {
        val json = """
            [
              {
                "request_id": "r1",
                "peer_id": "u2",
                "peer_nickname": "DriverTwo",
                "direction": "incoming",
                "created_at": "2026-08-19T12:00:00Z"
              },
              {
                "request_id": "r2",
                "peer_id": "u3",
                "peer_nickname": "DriverThree",
                "direction": "outgoing",
                "created_at": "2026-08-19T13:00:00Z"
              }
            ]
        """.trimIndent()
        val parsed = FriendSafetyClient.parseRequests(json)
        assertEquals(2, parsed.size)
        assertEquals("r1", parsed[0].id)
        assertEquals(FriendRequestDirection.INCOMING, parsed[0].direction)
        assertEquals("DriverTwo", parsed[0].peerNickname)
        assertEquals(FriendRequestDirection.OUTGOING, parsed[1].direction)
    }

    @Test
    fun fallsBackToDirectAddWhenRequestRpcIsMissing() {
        assertEquals(true, FriendSafetyClient.shouldFallbackToDirectAdd("schema_friend_safety_missing"))
        assertEquals(true, FriendSafetyClient.shouldFallbackToDirectAdd("offline"))
        assertEquals(true, FriendSafetyClient.shouldFallbackToDirectAdd("Could not find the function send_friend_request"))
        assertEquals(false, FriendSafetyClient.shouldFallbackToDirectAdd("invalid request"))
    }
}
