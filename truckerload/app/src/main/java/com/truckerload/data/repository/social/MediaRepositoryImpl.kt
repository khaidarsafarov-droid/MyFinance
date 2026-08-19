package com.truckerload.data.repository.social

import android.graphics.Bitmap
import com.truckerload.data.community.CommunityStorageClient
import com.truckerload.data.social.ChatAttachmentStorage
import com.truckerload.di.UserScope
import com.truckerload.domain.social.MessageType
import com.truckerload.domain.social.SocialResult
import java.io.File

@UserScope
class MediaRepositoryImpl(
    private val attachmentStorage: ChatAttachmentStorage,
    private val chatRepository: ChatRepository,
    private val storage: CommunityStorageClient,
    private val actorId: () -> String,
) : MediaRepository {

    override suspend fun sendImageMessage(
        chatId: String,
        bitmap: Bitmap,
        caption: String,
        senderName: String,
    ): SocialResult<Unit> {
        val path = attachmentStorage.saveImage(chatId, bitmap)
        val remotePath = uploadIfRemote(path, "chat/$chatId", "image/jpeg")
        return chatRepository.sendMessage(
            chatId,
            caption,
            senderName,
            MessageType.IMAGE,
            remotePath ?: path,
        )
    }

    override suspend fun sendVoiceMessage(
        chatId: String,
        audioFile: File,
        durationMs: Long,
        senderName: String,
    ): SocialResult<Unit> {
        val path = attachmentStorage.saveVoice(chatId, audioFile)
        val remotePath = uploadIfRemote(path, "chat/$chatId", "audio/mp4")
        return chatRepository.sendMessage(
            chatId,
            "",
            senderName,
            MessageType.VOICE,
            remotePath ?: path,
            durationMs = durationMs,
        )
    }

    private suspend fun uploadIfRemote(localPath: String, folder: String, mime: String): String? {
        if (!storage.isReady()) return null
        val file = File(localPath)
        if (!file.exists()) return null
        val objectPath = "${actorId()}/$folder/${file.name}"
        return storage.upload(file, objectPath, mime).getOrNull()
    }
}
