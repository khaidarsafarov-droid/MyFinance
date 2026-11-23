package com.truckerload.utils
import kotlinx.coroutines.CancellationException

import android.database.sqlite.SQLiteException
import java.io.IOException


import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Saves files into public device folders (Downloads, Documents/TruckoRig/Reports).
 * Uses MediaStore on Android 10+ to avoid WRITE_EXTERNAL_STORAGE.
 */
class StorageHelper(private val context: Context) {

    /** Save result: shareable URI plus user-facing display path. */
    data class SaveResult(val uri: Uri, val displayPath: String)

    /**
     * Saves file into Downloads (or Downloads/TruckoRig for reports).
     * Returns content URI for sharing and a display path like "Downloads/TruckoRig/Reports/filename".
     */
    fun saveToPublicDownloads(
        fileName: String,
        relativeSubDir: String = BrandConstants.DOWNLOADS_FOLDER,
        mimeType: String = "application/octet-stream",
        writeBlock: (OutputStream) -> Unit
    ): SaveResult? {
        val safeName = StoredFileNames.sanitize(fileName)
        val safeDir = StoredFileNames.downloadsRelativeDir(relativeSubDir)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveViaMediaStore(safeName, safeDir, mimeType, writeBlock)
            } else {
                saveViaLegacyFile(safeName, safeDir, writeBlock)
            }
        } catch (e: SQLiteException) {
            logFailure("StorageHelper", "saveToPublicDownloads failed", e)
            null
        
        } catch (e: IOException) {
            logFailure("StorageHelper", "saveToPublicDownloads failed", e)
            null
        
        } catch (e: IllegalArgumentException) {
            logFailure("StorageHelper", "saveToPublicDownloads failed", e)
            null
        
        } catch (e: CancellationException) {
            throw e
        } catch (e: IllegalStateException) {
            logFailure("StorageHelper", "saveToPublicDownloads failed", e)
            null
        
        }
    }

    @Suppress("DEPRECATION")
    private fun saveViaLegacyFile(fileName: String, subDir: String, writeBlock: (OutputStream) -> Unit): SaveResult? {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val targetDir = File(downloadsDir, subDir)
        val root = File(downloadsDir, BrandConstants.DOWNLOADS_FOLDER).canonicalFile
        val canonicalDir = targetDir.canonicalFile
        if (!canonicalDir.isInside(root)) return null
        canonicalDir.mkdirs()
        val file = File(canonicalDir, fileName)
        if (!file.canonicalFile.isInside(root)) return null
        FileOutputStream(file).use { writeBlock(it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val displayPath = "Downloads/$subDir/$fileName"
        return SaveResult(uri, displayPath)
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun saveViaMediaStore(fileName: String, subDir: String, mimeType: String, writeBlock: (OutputStream) -> Unit): SaveResult? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/$subDir"
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues) ?: return null
        try {
            resolver.openOutputStream(uri)?.use { writeBlock(it) }
        } finally {
            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }
        val displayPath = "Downloads/$subDir/$fileName"
        return SaveResult(uri, displayPath)
    }

    /**
     * Saves into app-private storage (no special permissions required).
     * Used as fallback when MediaStore is unavailable.
     */
    fun saveToAppStorage(fileName: String, subDir: String = "exports", writeBlock: (OutputStream) -> Unit): File {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val safeSub = subDir.split('/', '\\')
            .filter { it.isNotBlank() && it != "." && it != ".." }
            .joinToString("/")
            .ifBlank { "exports" }
        val targetDir = File(dir, safeSub).apply { mkdirs() }
        val file = File(targetDir, StoredFileNames.sanitize(fileName))
        FileOutputStream(file).use { writeBlock(it) }
        return file
    }

    private fun File.isInside(root: File): Boolean {
        val childPath = path
        val rootPath = root.path
        return childPath == rootPath || childPath.startsWith(rootPath + File.separator)
    }

    /** Shareable URI for a file from app storage (via FileProvider). */
    fun getShareableUri(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
