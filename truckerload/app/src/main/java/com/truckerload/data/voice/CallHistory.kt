package com.truckerload.data.voice

import com.truckerload.data.repository.social.ChatRepository
import com.truckerload.data.repository.social.ProfileRepository
import com.truckerload.domain.social.MessageType
import com.truckerload.domain.social.getOrNull
import com.truckerload.domain.voice.CallPolicy
import com.truckerload.domain.voice.CallState
import com.truckerload.domain.voice.CallStatus
import kotlinx.coroutines.flow.first

internal object CallHistory {
    suspend fun record(
        chatRepository: ChatRepository,
        profileRepository: ProfileRepository,
        call: CallState,
        status: CallStatus,
        durationMs: Long,
    ) {
        val peerId = CallPolicy.peerIdFor(call).trim()
        if (peerId.isBlank()) return
        val chatId = chatRepository.createPrivateChatWithPeer(peerId).getOrNull() ?: return
        val sender = profileRepository.watchMyProfile().first().displayName.ifBlank { "Me" }
        chatRepository.sendMessage(
            chatId = chatId,
            text = CallPolicy.chatRecordText(status, durationMs, outgoing = !call.isIncoming),
            senderName = sender,
            messageType = MessageType.CALL,
            durationMs = durationMs,
        )
    }

    suspend fun recordGroupStart(
        chatRepository: ChatRepository,
        profileRepository: ProfileRepository,
        chatId: String,
    ) {
        val sender = profileRepository.watchMyProfile().first().displayName.ifBlank { "Me" }
        chatRepository.sendMessage(
            chatId = chatId,
            text = "📞 Group call",
            senderName = sender,
            messageType = MessageType.CALL,
        )
    }
}
