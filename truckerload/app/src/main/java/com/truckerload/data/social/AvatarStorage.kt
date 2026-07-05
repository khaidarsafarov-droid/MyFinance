package com.truckerload.data.social

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class AvatarStorage(context: Context) {
    private val dir = File(context.filesDir, "avatars").apply { mkdirs() }

    fun saveAvatar(userId: String, bitmap: Bitmap): String {
        val file = File(dir, "${userId}_${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        dir.listFiles()
            ?.filter { it.name.startsWith("${userId}_") && it.absolutePath != file.absolutePath }
            ?.forEach { it.delete() }
        return file.absolutePath
    }

    fun deleteAvatar(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }
}
