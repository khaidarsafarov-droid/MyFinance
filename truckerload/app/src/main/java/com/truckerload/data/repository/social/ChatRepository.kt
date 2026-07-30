package com.truckerload.data.repository.social

import android.graphics.Bitmap
import com.truckerload.domain.social.MessageType
import com.truckerload.domain.social.SocialChat
import com.truckerload.domain.social.SocialMessage
import com.truckerload.domain.social.SocialPeerProfile
import com.truckerload.domain.social.SocialResult
import java.io.File
import kotlinx.coroutines.flow.Flow

/**
 * Direct messages, threads, reactions, and last-read state.
 */
interface ChatRepository {
    fun watchChats(): Flow<List<SocialChat>>
    fun watchPeers(): Flow<List<SocialPeerProfile>>
    fun watchChatsSearch(query: String): Flow<List<SocialChat>>
    fun watchTotalUnread(): Flow<Int>
    fun watchMessages(chatId: String, limit: Int = MESSAGE_PAGE_SIZE): Flow<List<SocialMessage>>
    suspend fun loadMoreMessages(
        chatId: String,
        beforeSentAt: Long,
        limit: Int = MESSAGE_PAGE_SIZE,
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
    suspend fun sendImageMessage(
        chatId: String,
        bitmap: Bitmap,
        caption: String,
        senderName: String,
    ): SocialResult<Unit>
    suspend fun sendVoiceMessage(
        chatId: String,
        audioFile: File,
        durationMs: Long,
        senderName: String,
    ): SocialResult<Unit>
    suspend fun addReaction(messageId: String, reaction: String): SocialResult<Unit>
    suspend fun markChatRead(chatId: String)
    suspend fun createPrivateChat(peerName: String): SocialResult<String>
    suspend fun createPrivateChatWithPeer(peerId: String): SocialResult<String>
    /** Archives the DM thread for [peerId] if one exists (e.g. after block). */
    suspend fun archivePrivateChatForPeer(peerId: String)

    companion object {
        const val MESSAGE_PAGE_SIZE = 50
    }
}
