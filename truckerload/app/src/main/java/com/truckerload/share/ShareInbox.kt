package com.truckerload.share

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.truckerload.domain.share.SharePayloadPolicy
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

internal sealed class ShareCopy {
    data class Ok(
        val file: File,
        val mime: String,
        val hash: String,
        val label: String,
    ) : ShareCopy()

    data object TooLarge : ShareCopy()
    data object Rejected : ShareCopy()
}

/**
 * Copies a content URI into app cache before the temporary read grant ends.
 */
internal class ShareInbox(private val context: Context) {

    private val dir = File(context.cacheDir, "share-inbox")
    private var spentBytes = 0L

    fun prepare() {
        spentBytes = 0L
        dir.mkdirs()
        dir.listFiles()?.forEach { file -> file.delete() }
    }

    fun copy(uri: Uri, index: Int, intentMime: String?): ShareCopy {
        val announced = announcedSize(uri)
        if (announced != null && announced > SharePayloadPolicy.MAX_FILE_BYTES) return ShareCopy.TooLarge
        if (announced != null && spentBytes + announced > SharePayloadPolicy.MAX_TOTAL_BYTES) {
            return ShareCopy.TooLarge
        }
        val mime = chooseMime(intentMime, resolverMime(uri)) ?: return ShareCopy.Rejected
        val dest = File(dir, "${SharePayloadPolicy.storageStem(index)}.${SharePayloadPolicy.extensionFor(mime)}")
        val digest = MessageDigest.getInstance("SHA-256")
        val header = ByteArray(64)
        var headerLen = 0
        var total = 0L
        var overflow = false
        val copied = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > SharePayloadPolicy.MAX_FILE_BYTES ||
                            spentBytes + total > SharePayloadPolicy.MAX_TOTAL_BYTES
                        ) {
                            overflow = true
                            return@use false
                        }
                        if (headerLen < header.size) {
                            val take = minOf(read, header.size - headerLen)
                            buffer.copyInto(header, headerLen, 0, take)
                            headerLen += take
                        }
                        digest.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                    }
                    true
                }
            } ?: false
        }.getOrDefault(false)
        if (!copied || total <= 0L) {
            dest.delete()
            return if (overflow) ShareCopy.TooLarge else ShareCopy.Rejected
        }
        val sniffed = SharePayloadPolicy.sniff(header.copyOf(headerLen))
        if (!SharePayloadPolicy.mimeAgrees(mime, sniffed)) {
            dest.delete()
            return ShareCopy.Rejected
        }
        spentBytes += total
        return ShareCopy.Ok(
            file = dest,
            mime = mime,
            hash = digest.digest().joinToString("") { byte -> "%02x".format(byte) },
            label = SharePayloadPolicy.displayLabel(displayName(uri)),
        )
    }

    private fun chooseMime(intentMime: String?, resolved: String?): String? {
        val fromResolver = resolved?.substringBefore(';')?.trim().orEmpty()
        if (SharePayloadPolicy.isAllowedMime(fromResolver) && !fromResolver.endsWith("/*")) {
            return fromResolver
        }
        val declared = intentMime?.substringBefore(';')?.trim().orEmpty()
        if (SharePayloadPolicy.isAllowedMime(declared)) return declared
        return null
    }

    private fun resolverMime(uri: Uri): String? =
        runCatching { context.contentResolver.getType(uri) }.getOrNull()

    private fun announcedSize(uri: Uri): Long? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index < 0 || cursor.isNull(index)) null else cursor.getLong(index).takeIf { it >= 0L }
            }
    }.getOrNull()

    private fun displayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                cursor.getString(0)
            }
    }.getOrNull()
}
