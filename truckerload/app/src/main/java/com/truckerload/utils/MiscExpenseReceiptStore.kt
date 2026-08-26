package com.truckerload.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * Copies a camera/gallery image into app-private storage so misc-expense
 * receipts survive cache cleanup and can be shared later with a company.
 */
object MiscExpenseReceiptStore {
    private const val DIR = "misc_receipts"

    fun dir(context: Context): File =
        File(context.applicationContext.filesDir, DIR).also { it.mkdirs() }

    fun createCameraCaptureFile(context: Context): File =
        File(dir(context), "capture_${UUID.randomUUID()}.jpg")

    /**
     * Persists [source] into [DIR] and returns the absolute path, or null on failure.
     * Does not delete [previousPath] — caller decides when to remove the old file.
     */
    fun persistFromUri(
        context: Context,
        source: Uri,
        previousPath: String? = null,
    ): String? {
        val app = context.applicationContext
        return runCatching {
            val ext = guessExtension(app, source)
            val dest = File(dir(app), "receipt_${UUID.randomUUID()}.$ext")
            app.contentResolver.openInputStream(source)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            if (dest.length() <= 0L) {
                dest.delete()
                return null
            }
            deleteIfManaged(app, previousPath)
            dest.absolutePath
        }.getOrNull()
    }

    fun deleteIfManaged(context: Context, path: String?) {
        if (path.isNullOrBlank()) return
        val file = File(path)
        val root = dir(context).canonicalFile
        runCatching {
            if (file.canonicalFile.startsWith(root) && file.isFile) {
                file.delete()
            }
        }
    }

    private fun guessExtension(context: Context, uri: Uri): String {
        val mime = context.contentResolver.getType(uri)?.lowercase().orEmpty()
        return when {
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            mime.contains("heic") || mime.contains("heif") -> "jpg"
            else -> "jpg"
        }
    }
}
