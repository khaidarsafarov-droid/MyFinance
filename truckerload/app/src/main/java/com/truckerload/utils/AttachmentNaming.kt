package com.truckerload.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds attachment file names from a load's Trip ID and date,
 * e.g. `114RS221Y_2026-07-21_143052.jpg`.
 */
object AttachmentNaming {

    private val fallbackDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormat = SimpleDateFormat("HHmmss", Locale.US)

    fun sanitize(part: String): String =
        part.trim()
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .ifBlank { "file" }

    fun buildFileName(
        tripId: String,
        loadDate: String,
        timestamp: Long = System.currentTimeMillis(),
        extension: String,
    ): String {
        val datePart = loadDate.trim().ifBlank {
            fallbackDateFormat.format(Date(timestamp))
        }
        val ext = extension.trimStart('.').ifBlank { "bin" }
        val base = "${sanitize(tripId)}_${sanitize(datePart)}"
        val time = timeFormat.format(Date(timestamp))
        return "${base}_${time}.$ext"
    }
}
