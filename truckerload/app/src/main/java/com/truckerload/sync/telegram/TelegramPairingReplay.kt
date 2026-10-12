package com.truckerload.sync.telegram

import android.content.SharedPreferences
import androidx.core.content.edit
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.sync.TelegramChatRestore

/**
 * Texts that arrived before `/start` are stored, the Telegram offset still advances,
 * and pairing imports only those saved update ids.
 */
internal object TelegramPairingReplay {
    private const val KEY = "unpaired_pending_update_ids"

    fun remember(prefs: SharedPreferences, updateId: Long) {
        val next = prefs.getStringSet(KEY, emptySet()).orEmpty().toMutableSet()
        next += updateId.toString()
        prefs.edit { putStringSet(KEY, next) }
    }

    suspend fun importPending(
        chatId: String,
        chatRestore: TelegramChatRestore,
        messageParser: TelegramMessageParser,
        loadRepository: LoadRepository,
        paycheckRepository: PaycheckRepository,
        dieselRepository: DieselRepository,
        prefs: SharedPreferences,
    ): Int {
        val ids = prefs.getStringSet(KEY, emptySet()).orEmpty()
        if (ids.isEmpty()) return 0
        prefs.edit { remove(KEY) }
        val wanted = ids.mapNotNull { it.toLongOrNull() }.toSet()
        val rows = chatRestore.pendingRows(chatId, wanted)
        for (row in rows) {
            messageParser.processMessage(
                text = row.text,
                messageDateSeconds = row.messageDateSeconds,
                loadRepository = loadRepository,
                paycheckRepository = paycheckRepository,
                dieselRepository = dieselRepository,
                prefs = prefs,
            )
        }
        return rows.size
    }
}
