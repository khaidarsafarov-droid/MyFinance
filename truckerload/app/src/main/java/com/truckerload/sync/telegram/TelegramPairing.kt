package com.truckerload.sync.telegram

import android.content.SharedPreferences

/** `/start`, `/start@bot`, or `/start <code>` — the Telegram Start button sends the first form. */
internal fun isStartCommand(text: String): Boolean {
    val trimmed = text.trim()
    return trimmed.equals("/start", ignoreCase = true) ||
        trimmed.startsWith("/start@", ignoreCase = true) ||
        trimmed.startsWith("/start ", ignoreCase = true)
}

/** `/start ABC` or `/start@bot ABC` → `ABC`; null when it is not `/start` or has no argument. */
internal fun telegramStartPayload(text: String): String? {
    if (!isStartCommand(text)) return null
    return text.trim().substringAfter(' ', missingDelimiterValue = "").trim().takeIf { it.isNotEmpty() }
}

/**
 * An unbound private chat pairs on `/start` alone. Telegram's Start button sends that
 * form, with no pairing code. Group chats and callback taps do not pair.
 */
internal fun shouldPairOnStart(text: String, isCallbackQuery: Boolean, chatType: String): Boolean {
    if (isCallbackQuery) return false
    val isPrivate = chatType == "private" || chatType.isBlank()
    return isPrivate && isStartCommand(text)
}

/**
 * The pair instruction is sent at most once per chat. Replaying a backlog of old
 * updates (offset reset after an update) must not flood the chat with the same text.
 */
internal object TelegramPairHint {
    fun markIfFirst(prefs: SharedPreferences, chatId: String): Boolean {
        if (chatId.isBlank()) return false
        val key = "pair_hint_$chatId"
        if (prefs.getBoolean(key, false)) return false
        prefs.edit().putBoolean(key, true).apply()
        return true
    }
}
