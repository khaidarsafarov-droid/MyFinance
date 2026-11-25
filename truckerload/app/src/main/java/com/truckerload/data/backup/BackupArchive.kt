package com.truckerload.data.backup

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Backup with media: a zip holding `backup.json` (the same JSON as a plain backup) first,
 * then `media/photos/<photoId>` and `media/scans/<scanId>` with each file's original bytes.
 * Bytes are copied unchanged, so a JPEG keeps its EXIF (GPS, date, time).
 *
 * Writing and reading stream one entry at a time; memory use does not grow with the library.
 * A plain JSON backup (older builds, auto and companion copies, Drive) still restores as before.
 */
object BackupArchive {
    const val MIME = "application/zip"
    const val JSON_ENTRY = "backup.json"
    private const val PHOTO_PREFIX = "media/photos/"
    private const val SCAN_PREFIX = "media/scans/"
    private const val BUFFER = 64 * 1024

    /** Ids become staging file names: a letter or digit first, then letters, digits, `-`, `_`, `.`. */
    private val SAFE_ID = Regex("^[A-Za-z0-9][A-Za-z0-9_.-]{0,127}$")

    /** Free space kept on the device while extracting, so a restore cannot fill the disk. */
    const val MIN_FREE_BYTES = 64L * 1024 * 1024

    data class MediaEntry(val name: String, val file: File)

    class Contents(val json: String, val photos: Map<String, File>, val scans: Map<String, File>)

    fun fileName(exportedAt: Long): String =
        BackupSchema.jsonFileName(exportedAt).removeSuffix(".json") + ".zip"

    /** Zip local-file signature `PK\u0003\u0004`. */
    val ZIP_MAGIC = "PK\u0003\u0004".toByteArray(Charsets.ISO_8859_1)

    /** True when [head] starts with [ZIP_MAGIC]. */
    fun isZip(head: ByteArray): Boolean =
        head.size >= ZIP_MAGIC.size && ZIP_MAGIC.indices.all { head[it] == ZIP_MAGIC[it] }

    /** Photo and scan files of [backup] that exist on this device. */
    fun mediaEntries(backup: BackupData): List<MediaEntry> {
        val photos = backup.photos.orEmpty().mapNotNull { row ->
            entry(PHOTO_PREFIX, row.id, row.filePath)
        }
        val scans = backup.scans.orEmpty().mapNotNull { row ->
            entry(SCAN_PREFIX, row.id, row.filePath)
        }
        return photos + scans
    }

    private fun entry(prefix: String, id: String, path: String): MediaEntry? {
        if (!SAFE_ID.matches(id) || path.isBlank()) return null
        val file = File(path)
        return if (file.isFile) MediaEntry(prefix + id, file) else null
    }

    /**
     * Writes the archive to [out] and finishes the zip without closing [out].
     * A file deleted between listing and writing is skipped, not an error.
     */
    fun write(out: OutputStream, jsonBytes: ByteArray, media: List<MediaEntry>) {
        val buffered = BufferedOutputStream(out, BUFFER)
        val zip = ZipOutputStream(buffered)
        zip.setLevel(Deflater.DEFAULT_COMPRESSION)
        zip.putNextEntry(ZipEntry(JSON_ENTRY))
        zip.write(jsonBytes)
        zip.closeEntry()
        // JPEG and PDF are already compressed.
        zip.setLevel(Deflater.NO_COMPRESSION)
        media.forEach { item ->
            val input = runCatching { FileInputStream(item.file) }.getOrNull() ?: return@forEach
            input.use { source ->
                zip.putNextEntry(ZipEntry(item.name).apply { time = item.file.lastModified() })
                source.copyTo(zip, BUFFER)
                zip.closeEntry()
            }
        }
        zip.finish()
        buffered.flush()
    }

    /**
     * Reads an archive from [input]. Media go to `staging/p/<id>` and `staging/s/<id>`; only an id
     * that passes [SAFE_ID] becomes a file name, never the entry path. Unknown entries are skipped.
     *
     * @throws BackupRestoreException.InvalidFormat when there is no `backup.json`
     * @throws BackupRestoreException.TooLarge when `backup.json` is over [maxJsonBytes]
     * @throws IOException when the device runs low on space or the stream breaks
     */
    fun read(input: InputStream, staging: File, maxJsonBytes: Int, minFreeBytes: Long = MIN_FREE_BYTES): Contents {
        staging.mkdirs()
        val reader = EntryReader(staging, maxJsonBytes, minFreeBytes)
        ZipInputStream(BufferedInputStream(input, BUFFER)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) reader.accept(zip, entry.name)
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val text = reader.json ?: throw BackupRestoreException.InvalidFormat()
        return Contents(text, reader.photos, reader.scans)
    }

    private class EntryReader(val staging: File, val maxJsonBytes: Int, val minFreeBytes: Long) {
        var json: String? = null
        val photos = HashMap<String, File>()
        val scans = HashMap<String, File>()

        fun accept(zip: ZipInputStream, name: String) {
            when {
                name == JSON_ENTRY -> json = String(readCapped(zip, maxJsonBytes), Charsets.UTF_8)

                name.startsWith(PHOTO_PREFIX) ->
                    stage(zip, name.removePrefix(PHOTO_PREFIX), File(staging, "p"), minFreeBytes)
                        ?.let { photos[it.name] = it }

                name.startsWith(SCAN_PREFIX) ->
                    stage(zip, name.removePrefix(SCAN_PREFIX), File(staging, "s"), minFreeBytes)
                        ?.let { scans[it.name] = it }
            }
        }
    }

    /** Streams one entry to `dir/<id>`; the staged file's name is the record id. */
    private fun stage(zip: ZipInputStream, id: String, dir: File, minFreeBytes: Long): File? {
        if (!SAFE_ID.matches(id)) return null
        dir.mkdirs()
        val target = File(dir, id)
        val buffer = ByteArray(BUFFER)
        var sinceCheck = 0L
        FileOutputStream(target).use { out ->
            var read = zip.read(buffer)
            while (read >= 0) {
                out.write(buffer, 0, read)
                sinceCheck += read
                if (sinceCheck >= SPACE_CHECK_BYTES) {
                    sinceCheck = 0
                    requireSpace(dir, minFreeBytes)
                }
                read = zip.read(buffer)
            }
        }
        requireSpace(dir, minFreeBytes)
        return target
    }

    private fun requireSpace(dir: File, minFreeBytes: Long) {
        if (dir.usableSpace < minFreeBytes) throw IOException("not enough free space")
    }

    private fun readCapped(input: InputStream, maxBytes: Int): ByteArray {
        val buffer = ByteArrayOutputStream()
        val chunk = ByteArray(BUFFER)
        var total = 0
        var read = input.read(chunk)
        while (read >= 0) {
            total += read
            if (total > maxBytes) throw BackupRestoreException.TooLarge()
            buffer.write(chunk, 0, read)
            read = input.read(chunk)
        }
        return buffer.toByteArray()
    }

    private const val SPACE_CHECK_BYTES = 8L * 1024 * 1024
}
