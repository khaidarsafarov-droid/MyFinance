package com.truckerload.data.repository.social

import android.graphics.Bitmap
import com.truckerload.data.social.ChatAttachmentStorage
import com.truckerload.domain.social.MessageType
import com.truckerload.domain.social.SocialResult
import java.io.File

/**
 * Chat attachment persistence (image / voice) that delegates send to [ChatRepository].
 *
 * Avatar upload lives on [ProfileRepository]; status media on [StatusRepository].
 */
class MediaRepositoryImpl(
    private val chatRepository: ChatRepository,
    private val attachmentStorage: ChatAttachmentStorage,
) : MediaRepository {
    override suspend fun sendImageMessage(
        chatId: String,
        bitmap: Bitmap,
        caption: String,
        senderName: String,
    ): SocialResult<Unit> {
        val path = attachmentStorage.saveImage(chatId, bitmap)
        return chatRepository.sendMessage(chatId, caption, senderName, MessageType.IMAGE, path)
    }

    override suspend fun sendVoiceMessage(
        chatId: String,
        audioFile: File,
        durationMs: Long,
        senderName: String,
    ): SocialResult<Unit> {
        val path = attachmentStorage.saveVoice(chatId, audioFile)
        return chatRepository.sendMessage(
            chatId = chatId,
            text = "",
            senderName = senderName,
            messageType = MessageType.VOICE,
            attachmentUrl = path,
            durationMs = durationMs,
        )
    }
}
