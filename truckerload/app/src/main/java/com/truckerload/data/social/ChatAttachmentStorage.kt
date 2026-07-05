package com.truckerload.data.social

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.util.UUID

class ChatAttachmentStorage(context: Context) {
    private val root = File(context.filesDir, "chat_attachments").apply { mkdirs() }
    private val statusRoot = File(context.filesDir, "statuses").apply { mkdirs() }

    fun saveImage(chatId: String, bitmap: Bitmap): String {
        val dir = File(root, chatId).apply { mkdirs() }
        val file = File(dir, "img_${UUID.randomUUID()}.jpg")
        SocialMediaOptimizer.compressImage(bitmap, file)
        return file.absolutePath
    }

    fun saveVoice(chatId: String, source: File): String {
        val dir = File(root, chatId).apply { mkdirs() }
        val dest = File(dir, "voice_${UUID.randomUUID()}.m4a")
        source.copyTo(dest, overwrite = true)
        source.delete()
        return dest.absolutePath
    }

    fun saveStatusPhoto(bitmap: Bitmap): String {
        val file = File(statusRoot, "status_${UUID.randomUUID()}.jpg")
        SocialMediaOptimizer.compressImage(bitmap, file)
        return file.absolutePath
    }

    fun saveStatusVoice(source: File): String {
        val dest = File(statusRoot, "status_voice_${UUID.randomUUID()}.m4a")
        source.copyTo(dest, overwrite = true)
        source.delete()
        return dest.absolutePath
    }

    fun resolve(path: String?): File? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        return file.takeIf { it.exists() }
    }
}
