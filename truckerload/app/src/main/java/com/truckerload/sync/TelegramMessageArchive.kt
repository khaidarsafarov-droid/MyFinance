package com.truckerload.sync

import android.content.Context
import com.truckerload.data.preferences.AccountIds
import org.json.JSONObject
import java.io.File

/**
 * Локальный архив текстов, присланных в Telegram-бот.
 * Нужен для команды «восстанови» — Telegram API не отдаёт старую историю чата.
 *
 * File is scoped per [userId] so account B cannot read account A’s bot history.
 */
class TelegramMessageArchive(
    context: Context,
    userId: String,
) {

    private val file = File(
        context.filesDir,
        // FIX: per-account archive path — shared file leaked history across users
        "telegram_message_archive_${AccountIds.sanitizeFilePart(userId)}.jsonl",
    )

    fun append(text: String, messageDateSeconds: Long?, updateId: Long? = null) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        val hash = trimmed.hashCode()
        synchronized(lock) {
            if (file.exists()) {
                file.readLines().forEach { line ->
                    runCatching {
                        val obj = JSONObject(line)
                        if (updateId != null && obj.optLong("updateId") == updateId) return
                        if (obj.optInt("hash") == hash) return
                    }
                }
            }
            val entry = JSONObject()
                .put("text", trimmed)
                .put("hash", hash)
                .put("dateSeconds", messageDateSeconds ?: 0L)
                .put("archivedAt", System.currentTimeMillis())
            updateId?.let { entry.put("updateId", it) }
            file.appendText(entry.toString() + "\n")
        }
    }

    fun getAll(): List<ArchivedTelegramMessage> = synchronized(lock) {
        if (!file.exists()) return emptyList()
        file.readLines()
            .mapNotNull { line ->
                runCatching {
                    val obj = JSONObject(line)
                    ArchivedTelegramMessage(
                        text = obj.getString("text"),
                        messageDateSeconds = obj.optLong("dateSeconds").takeIf { it > 0L }
                    )
                }.getOrNull()
            }
            .sortedWith(compareBy({ it.messageDateSeconds ?: Long.MAX_VALUE }, { it.text }))
    }

    fun count(): Int = synchronized(lock) {
        if (!file.exists()) return 0
        file.readLines().count { it.isNotBlank() }
    }

    data class ArchivedTelegramMessage(
        val text: String,
        val messageDateSeconds: Long?
    )

    companion object {
        private val lock = Any()
    }
}
