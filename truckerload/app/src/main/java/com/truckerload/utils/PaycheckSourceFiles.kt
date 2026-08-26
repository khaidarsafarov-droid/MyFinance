package com.truckerload.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.truckerload.R
import com.truckerload.domain.paycheck.PaycheckSourceFileNames
import java.io.File
import java.util.UUID

/** Copies settlement PDFs/photos into app-private storage so the journal can reopen them. */
object PaycheckSourceFiles {

    const val DIR = "paychecks"

    fun copyFromUri(context: Context, uri: Uri, displayName: String?): String? {
        val name = PaycheckSourceFileNames.sanitize(
            displayName?.takeIf { it.isNotBlank() } ?: displayName(context, uri),
        )
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                writeBytes(context, input.readBytes(), name)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun copyFromBytes(context: Context, bytes: ByteArray, displayName: String?): String? {
        if (bytes.isEmpty()) return null
        val name = PaycheckSourceFileNames.sanitize(displayName)
        return writeBytes(context, bytes, name)
    }

    private fun writeBytes(context: Context, bytes: ByteArray, sanitizedName: String): String? {
        if (bytes.isEmpty()) return null
        val dir = File(context.filesDir, DIR).apply { mkdirs() }
        val dest = File(dir, "${UUID.randomUUID()}_$sanitizedName")
        return try {
            dest.outputStream().use { output -> output.write(bytes) }
            if (!dest.exists() || dest.length() == 0L) {
                dest.delete()
                return null
            }
            "$DIR/${dest.name}"
        } catch (_: Exception) {
            dest.delete()
            null
        }
    }

    fun displayName(context: Context, uri: Uri): String {
        val fromProvider = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst() && cursor.columnCount > 0) cursor.getString(0) else null
                }
        }.getOrNull()
        return fromProvider ?: uri.lastPathSegment?.substringAfterLast('/').orEmpty()
    }

    fun file(context: Context, relativePath: String?): File? {
        val path = relativePath?.trim().orEmpty()
        if (path.isEmpty() || path.contains("..")) return null
        val file = File(context.filesDir, path)
        val root = File(context.filesDir, DIR).canonicalFile
        val canonical = runCatching { file.canonicalFile }.getOrNull() ?: return null
        if (!canonical.path.startsWith(root.path)) return null
        return canonical
    }

    fun exists(context: Context, relativePath: String?): Boolean =
        file(context, relativePath)?.exists() == true

    fun delete(context: Context, relativePath: String?) {
        file(context, relativePath)?.delete()
    }

    fun open(context: Context, relativePath: String?, displayName: String?): Boolean {
        val file = file(context, relativePath)?.takeIf { it.exists() } ?: return false
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return openUri(context, uri, displayName ?: file.name)
    }

    fun openUri(context: Context, uri: Uri, displayName: String?): Boolean {
        val mime = context.contentResolver.getType(uri)
            ?: PaycheckSourceFileNames.mimeType(displayName.orEmpty())
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(
                Intent.createChooser(intent, context.getString(R.string.paycheck_open_file)),
            )
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    fun takePersistableRead(context: Context, uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }
}
