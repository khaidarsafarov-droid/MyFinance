package com.truckerload.data.backup

import com.truckerload.utils.StoredFileNames
import com.truckerload.utils.VerifiedFileCopy
import java.io.File
import java.io.IOException

/**
 * Moves media staged from a [BackupArchive] into the photos/scans folders and points the
 * backup's photo/scan records at them, before the journal is written.
 *
 * - A record whose row already exists here with its file is left alone (no duplicate file).
 * - A file already in the folder with the same name and bytes is reused.
 * - A different file with that name is never overwritten; the restored copy gets `name_1`.
 */
object BackupMediaPlacer {

    /** Destination folders and the ids whose row and file are already on this device. */
    class Targets(
        val photosDir: File,
        val scansDir: File,
        val presentPhotoIds: Set<String> = emptySet(),
        val presentScanIds: Set<String> = emptySet(),
    )

    class Placement(val backup: BackupData, val created: List<File>)

    fun place(contents: BackupArchive.Contents, backup: BackupData, targets: Targets): Placement {
        val created = mutableListOf<File>()
        try {
            val photos = backup.photos?.map { row ->
                val staged = contents.photos[row.id]
                if (staged == null || row.id in targets.presentPhotoIds) return@map row
                val file = settle(staged, targets.photosDir, row.fileName.ifBlank { "${row.id}.jpg" })
                if (file.second) created += file.first
                row.copy(fileName = file.first.name, filePath = file.first.absolutePath)
            }
            val scans = backup.scans?.map { row ->
                val staged = contents.scans[row.id]
                if (staged == null || row.id in targets.presentScanIds) return@map row
                val file = settle(staged, targets.scansDir, row.fileName.ifBlank { "${row.id}.pdf" })
                if (file.second) created += file.first
                row.copy(fileName = file.first.name, filePath = file.first.absolutePath)
            }
            return Placement(backup.copy(photos = photos, scans = scans), created)
        } catch (e: IOException) {
            discard(created)
            throw e
        }
    }

    /** Removes files this restore created (used when the journal write fails). */
    fun discard(files: List<File>) {
        files.forEach { runCatching { it.delete() } }
    }

    /** Returns the file the record should use, and whether this call created it. */
    private fun settle(staged: File, dir: File, fileName: String): Pair<File, Boolean> {
        dir.mkdirs()
        val name = StoredFileNames.sanitize(fileName)
        val existing = File(dir, name)
        if (VerifiedFileCopy.sameContent(staged, existing)) return existing to false
        val target = VerifiedFileCopy.freeName(dir, name)
        VerifiedFileCopy.move(staged, target)
        return target to true
    }
}
