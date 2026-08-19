package com.truckerload.domain.voice

enum class CallPrivacy {
    EVERYONE,
    CONTACTS,
    NOBODY,
}

enum class SecondCallAction { BUSY, SWITCH, IGNORE }

object CallPolicy {

    fun canShowCallButton(
        blocked: Boolean,
        calleePrivacy: CallPrivacy,
        isContact: Boolean,
    ): Boolean = !blocked && canPlaceCall(calleePrivacy, isContact)

    fun canPlaceCall(calleePrivacy: CallPrivacy, isContact: Boolean): Boolean =
        when (calleePrivacy) {
            CallPrivacy.EVERYONE -> true
            CallPrivacy.CONTACTS -> isContact
            CallPrivacy.NOBODY -> false
        }

    fun canReceiveCall(
        myPrivacy: CallPrivacy,
        callerIsContact: Boolean,
        callerBlocked: Boolean,
    ): Boolean = !callerBlocked && canPlaceCall(myPrivacy, callerIsContact)

    fun canStartGroupCall(
        callsEnabled: Boolean,
        adminsOnly: Boolean,
        isAdmin: Boolean,
        isMember: Boolean,
    ): Boolean {
        if (!callsEnabled || !isMember) return false
        return if (adminsOnly) isAdmin else true
    }

    fun canJoinGroupCall(
        callsEnabled: Boolean,
        isMember: Boolean,
        participantCount: Int,
        maxParticipants: Int,
        alreadyJoined: Boolean,
    ): Boolean {
        if (!callsEnabled || !isMember) return false
        if (alreadyJoined) return true
        return participantCount < maxParticipants.coerceAtLeast(1)
    }

    fun shouldAutoMiss(status: CallStatus, elapsedMs: Long, timeoutMs: Long = CallConfig.RING_TIMEOUT_MS): Boolean =
        status == CallStatus.RINGING && elapsedMs >= timeoutMs

    fun shouldShowIncomingOverlay(
        incoming: CallState?,
        currentRoute: String?,
        myUserId: String,
    ): Boolean {
        if (incoming == null || incoming.status != CallStatus.RINGING || !incoming.isIncoming) return false
        if (incoming.calleeId != null && incoming.calleeId != myUserId && incoming.callerId == myUserId) {
            return false
        }
        val route = currentRoute.orEmpty()
        if (route.startsWith("call/") || route.contains("/call/")) return false
        if (route.startsWith("voice_room/") || route.contains("/voice_room/")) return false
        return true
    }

    fun secondIncomingAction(hasActiveCall: Boolean): SecondCallAction =
        if (hasActiveCall) SecondCallAction.SWITCH else SecondCallAction.BUSY

    fun roomEndsWhenLastLeaves(remainingParticipants: Int): Boolean = remainingParticipants <= 0

    fun shouldPromptVoiceMessage(
        status: CallStatus,
        isIncoming: Boolean,
        ringingElapsedMs: Long,
        hintAfterMs: Long = CallConfig.OFFLINE_HINT_MS,
    ): Boolean {
        if (isIncoming) return false
        if (status == CallStatus.MISSED) return true
        return status == CallStatus.RINGING && ringingElapsedMs >= hintAfterMs
    }

    fun peerIdFor(call: CallState): String =
        if (call.isIncoming) call.callerId else call.calleeId.orEmpty()

    fun peerNameFor(call: CallState): String =
        if (call.isIncoming) call.callerName else call.calleeName.orEmpty()

    fun chatRecordText(status: CallStatus, durationMs: Long, outgoing: Boolean): String {
        val seconds = (durationMs / 1000L).coerceAtLeast(0)
        return when (status) {
            CallStatus.MISSED -> if (outgoing) "📞 Missed call" else "📞 Missed call"
            CallStatus.REJECTED -> "📞 Call declined"
            CallStatus.ENDED -> "📞 Call · ${seconds}s"
            CallStatus.ACTIVE, CallStatus.RINGING -> "📞 Call"
        }
    }
}
