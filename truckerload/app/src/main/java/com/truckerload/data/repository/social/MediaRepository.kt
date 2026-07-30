package com.truckerload.data.repository.social

import android.graphics.Bitmap
import java.io.File

/**
 * Local media persistence for social avatars, chat attachments, and status media.
 */
interface MediaRepository {
    suspend fun saveAvatar(userId: String, bitmap: Bitmap): String
    suspend fun deleteAvatar(path: String?)
    suspend fun compressImage(bitmap: Bitmap): Bitmap
    suspend fun saveChatImage(chatId: String, bitmap: Bitmap): String
    suspend fun saveChatVoice(chatId: String, audioFile: File): String
    suspend fun saveStatusPhoto(bitmap: Bitmap): String
    suspend fun saveStatusVoice(audioFile: File): String
}
