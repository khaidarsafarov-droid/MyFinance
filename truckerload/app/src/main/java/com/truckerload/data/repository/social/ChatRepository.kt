package com.truckerload.data.repository.social

import com.truckerload.domain.social.MessageType
import com.truckerload.domain.social.SocialChat
import com.truckerload.domain.social.SocialMessage
import com.truckerload.domain.social.SocialPeerProfile
import com.truckerload.domain.social.SocialResult
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun watchChats(): Flow<List<SocialChat>>
    fun watchPublicGroups(): Flow<List<SocialChat>>
    fun watchPeers(): Flow<List<SocialPeerProfile>>
    fun watchChatsSearch(query: String): Flow<List<SocialChat>>
    fun watchTotalUnread(): Flow<Int>
    fun watchMessages(chatId: String, limit: Int = SocialConstants.MESSAGE_PAGE_SIZE): Flow<List<SocialMessage>>
    suspend fun loadMoreMessages(
        chatId: String,
        beforeSentAt: Long,
        limit: Int = SocialConstants.MESSAGE_PAGE_SIZE,
    ): SocialResult<List<SocialMessage>>
    suspend fun getChat(chatId: String): SocialChat?
    suspend fun sendMessage(
        chatId: String,
        text: String,
        senderName: String,
        messageType: MessageType = MessageType.TEXT,
        attachmentUrl: String? = null,
        replyToId: String? = null,
        locationLabel: String? = null,
        durationMs: Long = 0,
    ): SocialResult<Unit>
    suspend fun addReaction(messageId: String, reaction: String): SocialResult<Unit>
    fun recommendGroups(): Flow<List<SocialChat>>
    suspend fun markChatRead(chatId: String)
    suspend fun createPrivateChat(peerName: String): SocialResult<String>
    suspend fun createPrivateChatWithPeer(peerId: String): SocialResult<String>
    suspend fun archivePrivateChatForPeer(peerId: String)
}
