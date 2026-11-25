package com.truckerload.utils

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.CRC32

/** Streaming copy and compare helpers. Nothing is held in memory beyond one buffer. */
object VerifiedFileCopy {
    private const val BUFFER = 64 * 1024

    /** CRC32 of [file], read in chunks. */
    fun crc(file: File): Long {
        val crc = CRC32()
        val buffer = ByteArray(BUFFER)
        file.inputStream().use { input ->
            var read = input.read(buffer)
            while (read >= 0) {
                crc.update(buffer, 0, read)
                read = input.read(buffer)
            }
        }
        return crc.value
    }

    /** True when both files exist with the same length and the same CRC32. */
    fun sameContent(a: File, b: File): Boolean =
        a.isFile && b.isFile && a.length() == b.length() && crc(a) == crc(b)

    /**
     * Copies [source] to [target] through a temp file beside it, syncs it to disk, checks
     * length and CRC32, then renames. [target] never holds a partial copy.
     * The caller makes sure [target] does not exist.
     */
    fun copy(source: File, target: File) {
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, target.name + TEMP_SUFFIX)
        try {
            val written = writeSynced(source, tmp)
            if (tmp.length() != source.length() || crc(tmp) != written) {
                throw IOException("copy did not verify")
            }
            if (!tmp.renameTo(target)) throw IOException("could not move the copy into place")
        } finally {
            if (tmp.exists()) tmp.delete()
        }
    }

    /** Streams [source] into [target], syncs it, and returns the CRC32 of what was read. */
    private fun writeSynced(source: File, target: File): Long {
        val crc = CRC32()
        val buffer = ByteArray(BUFFER)
        source.inputStream().use { input ->
            FileOutputStream(target).use { out ->
                var read = input.read(buffer)
                while (read >= 0) {
                    crc.update(buffer, 0, read)
                    out.write(buffer, 0, read)
                    read = input.read(buffer)
                }
                out.flush()
                out.fd.sync()
            }
        }
        return crc.value
    }

    /** Moves [source] to [target]: rename when possible, otherwise a verified copy. */
    fun move(source: File, target: File) {
        target.parentFile?.mkdirs()
        if (source.renameTo(target)) return
        copy(source, target)
        source.delete()
    }

    /** First free name in [dir]: `name`, then `name_1`, `name_2`, … before the extension. */
    fun freeName(dir: File, name: String): File {
        val first = File(dir, name)
        if (!first.exists()) return first
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var index = 1
        while (true) {
            val candidate = File(dir, "${base}_$index$ext")
            if (!candidate.exists()) return candidate
            index++
        }
    }

    const val TEMP_SUFFIX = ".partial"
}
