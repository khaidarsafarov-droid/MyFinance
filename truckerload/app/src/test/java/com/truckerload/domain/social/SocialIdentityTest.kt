package com.truckerload.domain.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialIdentityTest {

    @Test
    fun uuidDetection() {
        assertTrue(SocialIdentity.isUuid("550e8400-e29b-41d4-a716-446655440000"))
        assertFalse(SocialIdentity.isUuid("local_user"))
        assertFalse(SocialIdentity.isUuid("local_abc"))
        assertFalse(SocialIdentity.isUuid(""))
    }

    @Test
    fun actorIdUsesUuidWhenPresent() {
        assertEquals(
            "550e8400-e29b-41d4-a716-446655440000",
            SocialIdentity.actorId("550e8400-e29b-41d4-a716-446655440000"),
        )
        assertEquals(SocialIdentity.LOCAL_PROFILE_ID, SocialIdentity.actorId("local_dev"))
        assertEquals(SocialIdentity.LOCAL_PROFILE_ID, SocialIdentity.actorId(null))
    }

    @Test
    fun isMineAcceptsLegacySender() {
        val me = "550e8400-e29b-41d4-a716-446655440000"
        assertTrue(SocialIdentity.isMine(me, me))
        assertTrue(SocialIdentity.isMine("me", me))
        assertTrue(SocialIdentity.isMine("local_user", me))
        assertFalse(SocialIdentity.isMine("someone-else", me))
    }

    @Test
    fun privateChatIdRoundTrip() {
        val peer = "550e8400-e29b-41d4-a716-446655440000"
        val chatId = SocialIdentity.privateChatIdForPeer(peer)
        assertEquals(peer, SocialIdentity.peerIdFromPrivateChat(chatId))
        assertEquals(null, SocialIdentity.peerIdFromPrivateChat("group_abc"))
    }
}
