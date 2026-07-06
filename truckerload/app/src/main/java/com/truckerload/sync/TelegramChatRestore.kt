package com.truckerload.sync

import androidx.core.content.edit
import android.content.SharedPreferences
import android.util.Log
import com.truckerload.data.local.dao.TelegramInboxDao
import com.truckerload.data.local.entities.TelegramInboxEntity
import com.truckerload.data.remote.TelegramApi
import com.truckerload.data.remote.TelegramBotFeatures
import com.truckerload.data.remote.TelegramUpdate
import com.truckerload.domain.parser.MessageClassifier
import com.truckerload.domain.parser.MessageType
import kotlin.math.abs

/**
 * Сохраняет входящие тексты бота и при «восстанови» ищет лоуды/платёжки/дизель
 * по шаблону (Trip ID, PU#, Total Rate и т.д.) в inbox + архиве.
 */
class TelegramChatRestore(
    private val inboxDao: TelegramInboxDao,
    private val messageArchive: TelegramMessageArchive
) {

    suspend fun persistIncoming(update: TelegramUpdate) {
        if (update.isCallbackQuery) return
        val text = update.text.trim()
        if (text.isBlank()) return
        inboxDao.insert(
            TelegramInboxEntity(
                updateId = update.updateId,
                chatId = update.chatId,
                text = text,
                messageDateSeconds = update.messageDateSeconds,
                receivedAt = System.currentTimeMillis()
            )
        )
        messageArchive.append(text, update.messageDateSeconds, update.updateId)
    }

    /** Забирает необработанную очередь getUpdates в inbox (сообщения ещё в Telegram). */
    suspend fun drainPendingUpdatesIntoInbox(
        telegramApi: TelegramApi,
        chatId: String,
        prefs: SharedPreferences
    ): Int {
        var saved = 0
        var offset = prefs.getLong(TelegramSyncWorker.KEY_LAST_OFFSET, 0L).takeIf { it > 0L }
        var round = 0
        while (round < MAX_DRAIN_ROUNDS) {
            round++
            val result = telegramApi.getUpdates(offset = offset, timeoutSeconds = 0, limit = 100).getOrNull()
                ?: break
            if (result.rawMaxUpdateId == 0L && result.updates.isEmpty()) break

            for (update in result.updates) {
                if (update.isCallbackQuery) continue
                val text = update.text.trim()
                if (text.isBlank()) continue
                inboxDao.insert(
                    TelegramInboxEntity(
                        updateId = update.updateId,
                        chatId = update.chatId,
                        text = text,
                        messageDateSeconds = update.messageDateSeconds,
                        receivedAt = System.currentTimeMillis()
                    )
                )
                messageArchive.append(text, update.messageDateSeconds, update.updateId)
                if (update.chatId == chatId) saved++
            }
            prefs.edit(commit = true) {putLong(TelegramSyncWorker.KEY_LAST_OFFSET, result.nextOffset)}
            offset = result.nextOffset
        }
        Log.i(TAG, "drainPendingUpdates saved=$saved chatId=$chatId offset=$offset")
        return saved
    }

    suspend fun collectRestorableMessages(chatId: String): List<RestorableChatMessage> {
        val byHash = LinkedHashMap<Int, RestorableChatMessage>()

        fun add(text: String, messageDateSeconds: Long?, source: String) {
            val trimmed = text.trim()
            if (!isRestorableDataMessage(trimmed)) return
            val hash = trimmed.hashCode()
            val existing = byHash[hash]
            val entry = RestorableChatMessage(trimmed, messageDateSeconds, source)
            if (existing == null || (messageDateSeconds ?: 0L) < (existing.messageDateSeconds ?: Long.MAX_VALUE)) {
                byHash[hash] = entry
            }
        }

        inboxDao.getAllForChat(chatId).forEach { row ->
            add(row.text, row.messageDateSeconds, "inbox")
        }
        messageArchive.getAll().forEach { row ->
            add(row.text, row.messageDateSeconds, "archive")
        }

        return byHash.values.sortedWith(
            compareBy({ it.messageDateSeconds ?: Long.MAX_VALUE }, { it.text })
        )
    }

    suspend fun seedInboxFromRawTexts(chatId: String, entries: List<Pair<String, Long?>>) {
        for ((text, dateSeconds) in entries) {
            val trimmed = text.trim()
            if (trimmed.isBlank() || !isRestorableDataMessage(trimmed)) continue
            val syntheticId = syntheticUpdateId(trimmed)
            inboxDao.insert(
                TelegramInboxEntity(
                    updateId = syntheticId,
                    chatId = chatId,
                    text = trimmed,
                    messageDateSeconds = dateSeconds,
                    receivedAt = System.currentTimeMillis()
                )
            )
            messageArchive.append(trimmed, dateSeconds, syntheticId)
        }
    }

    fun isRestorableDataMessage(text: String): Boolean {
        val t = text.trim()
        if (t.isBlank()) return false
        if (TelegramBotFeatures.isRestoreRequest(t)) return false
        if (t.startsWith("/")) return false
        if (BOT_REPLY_PREFIXES.any { t.startsWith(it) }) return false
        if (MessageClassifier.isLoadLike(t)) return true
        return when (MessageClassifier.classify(t)) {
            MessageType.LOAD, MessageType.PAYCHECK, MessageType.DIESEL -> true
            MessageType.UNKNOWN -> false
        }
    }

    data class RestorableChatMessage(
        val text: String,
        val messageDateSeconds: Long?,
        val source: String
    )

    companion object {
        private const val TAG = "TelegramChatRestore"
        private const val MAX_DRAIN_ROUNDS = 200

        private val BOT_REPLY_PREFIXES = listOf(
            "✅", "🆗", "📊", "⏳", "📭", "❓", "📥", "🔄", "👋", "📋", "📦", "💰", "ℹ️", "❌"
        )

        fun syntheticUpdateId(text: String): Long = -abs(text.hashCode().toLong()) - 1L
    }
}
