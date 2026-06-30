package com.truckerload.utils

import com.truckerload.data.backup.BackupData
import com.truckerload.domain.model.Load
import java.util.Locale

/**
 * Текстовая заметка для приложений «Заметки»: только строки грузов по дате.
 * Полный бэкап (зарплата, дизель) хранится отдельно в памяти приложения.
 */
object BackupNoteFormatter {

    private const val HIDDEN_SEPARATOR = "\u001E"

    data class NoteFile(val visibleText: String, val loadCount: Int)

    fun buildNote(backup: BackupData): NoteFile {
        val visible = buildVisibleChart(backup.loads)
        return NoteFile(visibleText = visible, loadCount = backup.loads.size)
    }

    /** Только для старых файлов, где JSON был внутри .txt */
    fun extractBackupJson(bytes: ByteArray): String? {
        val nullIndex = bytes.indexOfFirst { it == 0.toByte() }
        if (nullIndex >= 0 && nullIndex < bytes.lastIndex) {
            return String(bytes.copyOfRange(nullIndex + 1, bytes.size), Charsets.UTF_8).trim()
                .takeIf { it.startsWith("{") }
        }
        return extractBackupJson(String(bytes, Charsets.UTF_8))
    }

    fun extractBackupJson(content: String): String? {
        val trimmed = content.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) return trimmed

        val sepIndex = content.indexOf(HIDDEN_SEPARATOR)
        if (sepIndex >= 0) {
            decodeBase64Payload(content.substring(sepIndex + HIDDEN_SEPARATOR.length))?.let { return it }
        }

        content.lineSequence()
            .map { it.trim() }
            .lastOrNull { it.startsWith("TL1:") }
            ?.let { decodeLegacyLine(it)?.let { json -> return json } }

        val legacyBegin = "-----BEGIN TRUCKERLOAD BACKUP-----"
        val legacyEnd = "-----END TRUCKERLOAD BACKUP-----"
        val start = content.indexOf(legacyBegin)
        val end = content.indexOf(legacyEnd)
        if (start >= 0 && end > start) {
            return content.substring(start + legacyBegin.length, end).trim()
        }
        return null
    }

    fun noteFileName(exportedAt: Long): String {
        val stamp = formatDateTimeForDisplay(exportedAt)
            .replace(":", "-")
            .replace(" ", "_")
        return "Loads_$stamp.txt"
    }

    fun companionFileName(txtFileName: String): String =
        txtFileName.removeSuffix(".txt") + ".tlb"

    private fun buildVisibleChart(loads: List<Load>): String = buildString {
        loads.sortedBy { it.date.ifBlank { "9999-99-99" } }.forEach { load ->
            appendLine(formatLoadLine(load))
        }
    }.trimEnd()

    private fun formatLoadLine(load: Load): String {
        val date = displayDate(load.date)
        val trip = load.tripId.ifBlank { load.id }
        val price = formatMoney(load.totalRate)
        val route = route(load)
        return "$date | $trip | $price | $route"
    }

    private fun route(load: Load): String {
        val route = "${load.pointA} -> ${load.pointB}".trim()
        return route.ifBlank { "-" }
    }

    private fun displayDate(isoDate: String): String {
        val parts = isoDate.split("-")
        if (parts.size == 3) return "${parts[2]}.${parts[1]}.${parts[0]}"
        return isoDate.ifBlank { "-" }
    }

    private fun formatMoney(value: Double): String =
        "$" + String.format(Locale.US, "%,.2f", value)

    private fun decodeBase64Payload(encoded: String): String? = runCatching {
        String(android.util.Base64.decode(encoded.trim(), android.util.Base64.DEFAULT), Charsets.UTF_8)
    }.getOrNull()

    private fun decodeLegacyLine(line: String): String? = runCatching {
        String(
            android.util.Base64.decode(line.removePrefix("TL1:"), android.util.Base64.DEFAULT),
            Charsets.UTF_8
        )
    }.getOrNull()
}
