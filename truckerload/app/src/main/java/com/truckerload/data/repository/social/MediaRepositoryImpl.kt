package com.truckerload.data.repository.social

import android.graphics.Bitmap
import com.truckerload.data.social.ChatAttachmentStorage
import com.truckerload.di.UserScope
import com.truckerload.domain.social.MessageType
import com.truckerload.domain.social.SocialResult
import java.io.File

@UserScope
class MediaRepositoryImpl(
    private val attachmentStorage: ChatAttachmentStorage,
    private val chatRepository: ChatRepository,
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
        return chatRepository.sendMessage(chatId, "", senderName, MessageType.VOICE, path, durationMs = durationMs)
    }
}
