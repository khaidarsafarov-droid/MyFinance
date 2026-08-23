package com.truckerload.data.backup

import com.truckerload.utils.BackupNoteFormatter
import java.nio.charset.StandardCharsets

/**
 * Turns a user-selected backup file into JSON text for [BackupDataCodec].
 * Does not log paths or payload contents.
 */
object BackupRestoreParser {

    fun parseToJson(bytes: ByteArray): Result<String> {
        val stripped = BackupDataCodec.stripUtf8Bom(bytes)
        if (stripped.isEmpty()) {
            return Result.failure(BackupRestoreException.InvalidFormat())
        }
        val extracted = BackupNoteFormatter.extractBackupJson(stripped)
        if (extracted != null) {
            return Result.success(BackupDataCodec.stripBom(extracted).trim())
        }
        val text = String(stripped, StandardCharsets.UTF_8)
        return Result.failure(
            if (looksLikeChartNote(text)) {
                BackupRestoreException.ChartNoteNotBackup()
            } else {
                BackupRestoreException.InvalidFormat()
            },
        )
    }

    internal fun looksLikeChartNote(text: String): Boolean {
        val first = text.trim().lineSequence().firstOrNull().orEmpty()
        if (first.startsWith("{") || first.startsWith("[")) return false
        if (first.contains("BEGIN TRUCKERLOAD BACKUP", ignoreCase = true)) return false
        if (first.startsWith("TL1:")) return false
        return first.split(" | ").size >= 3
    }
}
