package com.truckerload.data.backup

import com.truckerload.utils.BrandConstants
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Backup JSON format version — independent of Room's schemaVersion. */
object BackupSchema {
    const val CURRENT = 2
    const val V1 = 1
    const val V2 = 2

    const val JSON_MIME = "application/json"
    const val JSON_UTF8_MIME = "application/json; charset=UTF-8"

    /** MIME types offered to the Storage Access Framework restore picker. */
    val RESTORE_OPEN_MIME_TYPES = arrayOf(
        JSON_MIME,
        "text/json",
        "application/octet-stream",
        "text/plain",
        "*/*",
    )

    fun jsonFileName(exportedAt: Long): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(exportedAt))
        return "${BrandConstants.FILE_PREFIX}_Backup_$stamp.json"
    }
}
