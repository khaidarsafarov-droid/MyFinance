package com.truckerload.utils

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Writes [bytes] to a sibling temp file, forces them to disk, then renames over [this].
 * A crash or a full disk leaves either the old file or the new one, never half of each.
 * Backup files use this: the newest backup is the one a restore picks first.
 */
internal fun File.writeBytesAtomically(bytes: ByteArray) {
    parentFile?.mkdirs()
    val tmp = File(parentFile, "$name.tmp")
    try {
        FileOutputStream(tmp).use { out ->
            out.write(bytes)
            out.flush()
            out.fd.sync()
        }
        if (!tmp.renameTo(this)) throw IOException("could not move $name into place")
    } finally {
        if (tmp.exists()) tmp.delete()
    }
}

internal fun File.writeTextAtomically(text: String) = writeBytesAtomically(text.toByteArray(Charsets.UTF_8))
