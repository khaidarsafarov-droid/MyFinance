package com.truckerload.data.repository.social

import android.content.Context
import android.graphics.Bitmap
import com.truckerload.data.social.AvatarStorage
import com.truckerload.data.social.ChatAttachmentStorage
import com.truckerload.data.social.SocialMediaOptimizer
import com.truckerload.di.UserScope
import java.io.File
import javax.inject.Inject

@UserScope
class MediaRepositoryImpl @Inject constructor(
    context: Context,
) : MediaRepository {
    private val avatarStorage = AvatarStorage(context)
    private val attachmentStorage = ChatAttachmentStorage(context)

    override suspend fun saveAvatar(userId: String, bitmap: Bitmap): String =
        avatarStorage.saveAvatar(userId, bitmap)

    override suspend fun deleteAvatar(path: String?) {
        avatarStorage.deleteAvatar(path)
    }

    override suspend fun compressImage(bitmap: Bitmap): Bitmap =
        SocialMediaOptimizer.compressImage(bitmap)

    override suspend fun saveChatImage(chatId: String, bitmap: Bitmap): String =
        attachmentStorage.saveImage(chatId, bitmap)

    override suspend fun saveChatVoice(chatId: String, audioFile: File): String =
        attachmentStorage.saveVoice(chatId, audioFile)

    override suspend fun saveStatusPhoto(bitmap: Bitmap): String =
        attachmentStorage.saveStatusPhoto(bitmap)

    override suspend fun saveStatusVoice(audioFile: File): String =
        attachmentStorage.saveStatusVoice(audioFile)
}
