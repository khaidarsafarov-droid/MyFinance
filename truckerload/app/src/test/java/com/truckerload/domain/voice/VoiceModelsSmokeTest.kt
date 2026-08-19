package com.truckerload.domain.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceModelsSmokeTest {

    @Test
    fun voiceRoom_andParticipant_construct() {
        val room = VoiceRoom(
            id = "room-1",
            name = "Dispatch",
            type = VoiceRoomType.PUBLIC,
            creatorId = "user-1",
            participants = listOf(
                VoiceParticipant(
                    userId = "user-1",
                    displayName = "Alex",
                    joinedAt = 1_700_000_000_000L,
                    isMe = true,
                ),
            ),
            createdAt = 1_700_000_000_000L,
            updatedAt = 1_700_000_100_000L,
        )

        assertEquals(VoiceRoomType.PUBLIC, room.type)
        assertTrue(room.participants.single().isMe)
        assertEquals("", room.description)
        assertEquals("", room.moderatorId)
        assertTrue(room.canDelete("user-1"))
        assertFalse(room.canDelete("user-2"))
        assertTrue(room.canManage("user-1"))
        val moderated = room.copy(moderatorId = "user-2")
        assertTrue(moderated.canManage("user-2"))
        assertFalse(moderated.canDelete("user-2"))
    }

    @Test
    fun callState_andSignal_enumsConstruct() {
        val call = CallState(
            callId = "call-1",
            type = CallType.P2P,
            status = CallStatus.RINGING,
            participants = listOf("user-1", "user-2"),
            startedAt = 1_700_000_000_000L,
            isIncoming = true,
            callerId = "user-1",
            callerName = "Alex",
            calleeId = "user-2",
            calleeName = "Sam",
        )
        val signal = Signal(
            type = SignalType.OFFER,
            fromUserId = "user-1",
            sdp = "v=0",
        )

        assertEquals(CallType.P2P, call.type)
        assertEquals(CallStatus.RINGING, call.status)
        assertEquals(SignalType.OFFER, signal.type)
    }

    @Test
    fun voiceRoomSettings_defaultsAreSensible() {
        val settings = VoiceRoomSettings()

        assertEquals(64_000, settings.bitrate)
        assertTrue(settings.echoCancellation)
    }
}
