package com.truckerload.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.truckerload.data.preferences.PrivacyStore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Saves files into app-private storage by default (privacy lockdown).
 * Public Downloads only when [PrivacyStore.allowPublicDownloads] is explicitly enabled.
 */
class StorageHelper(private val context: Context) {

    /** Save result: shareable URI plus user-facing display path. */
    data class SaveResult(val uri: Uri, val displayPath: String)

    /**
     * Saves finance/export file. Uses public Downloads only if privacy allows;
     * otherwise writes to app-private `exports/` and returns a FileProvider URI.
     */
    fun saveExport(
        fileName: String,
        relativeSubDir: String = BrandConstants.DOWNLOADS_FOLDER,
        mimeType: String = "application/octet-stream",
        writeBlock: (OutputStream) -> Unit,
    ): SaveResult? {
        val privacy = PrivacyStore(context)
        if (privacy.allowPublicDownloads()) {
            return saveToPublicDownloads(fileName, relativeSubDir, mimeType, writeBlock)
                ?: savePrivateExport(fileName, writeBlock)
        }
        return savePrivateExport(fileName, writeBlock)
    }

    private fun savePrivateExport(fileName: String, writeBlock: (OutputStream) -> Unit): SaveResult? {
        return try {
            val file = saveToAppStorage(fileName, "exports", writeBlock)
            SaveResult(getShareableUri(file), "private/exports/$fileName")
        } catch (e: Exception) {
            android.util.Log.e("StorageHelper", "savePrivateExport failed", e)
            null
        }
    }

    /**
     * Saves file into Downloads (or Downloads/TruckLog for reports).
     * Prefer [saveExport] which respects privacy lockdown.
     */
    fun saveToPublicDownloads(
        fileName: String,
        relativeSubDir: String = BrandConstants.DOWNLOADS_FOLDER,
        mimeType: String = "application/octet-stream",
        writeBlock: (OutputStream) -> Unit
    ): SaveResult? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveViaMediaStore(fileName, relativeSubDir, mimeType, writeBlock)
            } else {
                saveViaLegacyFile(fileName, relativeSubDir, writeBlock)
            }
        } catch (e: Exception) {
            android.util.Log.e("StorageHelper", "saveToPublicDownloads failed", e)
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun saveViaLegacyFile(fileName: String, subDir: String, writeBlock: (OutputStream) -> Unit): SaveResult? {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val targetDir = File(downloadsDir, subDir).apply { mkdirs() }
        val file = File(targetDir, fileName)
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
        // Always use internal app storage (not browsable via USB file managers as easily).
        val targetDir = File(context.filesDir, subDir).apply { mkdirs() }
        val file = File(targetDir, fileName)
        FileOutputStream(file).use { writeBlock(it) }
        return file
    }

    /** Shareable URI for a file from app storage (via FileProvider). */
    fun getShareableUri(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
