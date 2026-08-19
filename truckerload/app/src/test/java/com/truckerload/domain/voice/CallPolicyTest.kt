package com.truckerload.domain.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallPolicyTest {

    @Test
    fun blockedUserCannotSeeCallButtonOrPlaceCall() {
        assertFalse(CallPolicy.canShowCallButton(blocked = true, CallPrivacy.EVERYONE, isContact = true))
        assertTrue(CallPolicy.canShowCallButton(blocked = false, CallPrivacy.EVERYONE, isContact = false))
        assertFalse(CallPolicy.canPlaceCall(CallPrivacy.NOBODY, isContact = true))
        assertFalse(CallPolicy.canPlaceCall(CallPrivacy.CONTACTS, isContact = false))
        assertTrue(CallPolicy.canPlaceCall(CallPrivacy.CONTACTS, isContact = true))
    }

    @Test
    fun incomingRejectedWhenBlockedOrNobody() {
        assertFalse(CallPolicy.canReceiveCall(CallPrivacy.EVERYONE, callerIsContact = true, callerBlocked = true))
        assertFalse(CallPolicy.canReceiveCall(CallPrivacy.NOBODY, callerIsContact = true, callerBlocked = false))
        assertTrue(CallPolicy.canReceiveCall(CallPrivacy.CONTACTS, callerIsContact = true, callerBlocked = false))
    }

    @Test
    fun ringTimeoutMarksMissed() {
        assertFalse(CallPolicy.shouldAutoMiss(CallStatus.RINGING, 10_000L))
        assertTrue(CallPolicy.shouldAutoMiss(CallStatus.RINGING, CallConfig.RING_TIMEOUT_MS))
        assertFalse(CallPolicy.shouldAutoMiss(CallStatus.ACTIVE, CallConfig.RING_TIMEOUT_MS))
    }

    @Test
    fun overlayHiddenOnCallScreen() {
        val incoming = CallState(
            callId = "c1",
            type = CallType.P2P,
            status = CallStatus.RINGING,
            participants = listOf("a", "b"),
            startedAt = 1L,
            isIncoming = true,
            callerId = "a",
            callerName = "A",
            calleeId = "b",
            calleeName = "B",
        )
        assertTrue(CallPolicy.shouldShowIncomingOverlay(incoming, "community", "b"))
        assertFalse(CallPolicy.shouldShowIncomingOverlay(incoming, "call/c1", "b"))
        assertFalse(CallPolicy.shouldShowIncomingOverlay(incoming, "voice_room/r1", "b"))
        assertFalse(CallPolicy.shouldShowIncomingOverlay(null, "community", "b"))
    }

    @Test
    fun groupStartAndJoinRespectFlagsAndLimit() {
        assertTrue(CallPolicy.canStartGroupCall(callsEnabled = true, adminsOnly = false, isAdmin = false, isMember = true))
        assertFalse(CallPolicy.canStartGroupCall(callsEnabled = true, adminsOnly = true, isAdmin = false, isMember = true))
        assertTrue(CallPolicy.canStartGroupCall(callsEnabled = true, adminsOnly = true, isAdmin = true, isMember = true))
        assertFalse(CallPolicy.canStartGroupCall(callsEnabled = false, adminsOnly = false, isAdmin = true, isMember = true))
        assertTrue(
            CallPolicy.canJoinGroupCall(
                callsEnabled = true,
                isMember = true,
                participantCount = 7,
                maxParticipants = 8,
                alreadyJoined = false,
            ),
        )
        assertFalse(
            CallPolicy.canJoinGroupCall(
                callsEnabled = true,
                isMember = true,
                participantCount = 8,
                maxParticipants = 8,
                alreadyJoined = false,
            ),
        )
        assertTrue(CallPolicy.roomEndsWhenLastLeaves(0))
        assertFalse(CallPolicy.roomEndsWhenLastLeaves(1))
    }

    @Test
    fun secondIncomingOffersSwitchWhenBusy() {
        assertEquals(SecondCallAction.SWITCH, CallPolicy.secondIncomingAction(hasActiveCall = true))
        assertEquals(SecondCallAction.BUSY, CallPolicy.secondIncomingAction(hasActiveCall = false))
    }

    @Test
    fun chatRecordTextForMissedAndEnded() {
        assertEquals("📞 Missed call", CallPolicy.chatRecordText(CallStatus.MISSED, 0, outgoing = true))
        assertTrue(CallPolicy.chatRecordText(CallStatus.ENDED, 45_000, outgoing = true).contains("45"))
    }

    @Test
    fun outgoingRingOffersVoiceMessageAfterHintDelay() {
        assertFalse(
            CallPolicy.shouldPromptVoiceMessage(CallStatus.RINGING, isIncoming = false, ringingElapsedMs = 1_000L),
        )
        assertTrue(
            CallPolicy.shouldPromptVoiceMessage(
                CallStatus.RINGING,
                isIncoming = false,
                ringingElapsedMs = CallConfig.OFFLINE_HINT_MS,
            ),
        )
        assertTrue(CallPolicy.shouldPromptVoiceMessage(CallStatus.MISSED, isIncoming = false, ringingElapsedMs = 0))
        assertFalse(CallPolicy.shouldPromptVoiceMessage(CallStatus.RINGING, isIncoming = true, ringingElapsedMs = 20_000L))
    }

    @Test
    fun groupRoomIdIsStableAndPrefixed() {
        assertEquals("group_abc", CallConfig.groupRoomId("abc"))
        assertEquals(CallConfig.groupRoomId("chat-1"), CallConfig.groupRoomId("chat-1"))
    }
}
