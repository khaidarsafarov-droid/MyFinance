package com.truckerload.data.remote

import org.json.JSONArray
import org.json.JSONObject

/**
 * Telegram Bot API helpers: command Menu (blue Menu button) and leftover
 * reply-keyboard labels. The persistent bottom grid is not used anymore.
 */
object TelegramBotFeatures {

    private const val BTN_HELP = "📋 Help"
    private const val BTN_STATUS = "📊 Status"
    private const val BTN_LOAD = "📦 Load"
    private const val BTN_PAY = "💰 Paycheck"
    private const val BTN_IMPORT = "📥 Import"
    private const val BTN_RESTORE = "🔄 Restore"

    // Legacy Russian labels — still accepted so a leftover keyboard keeps working
    // until the next bot reply hides it.
    private const val LEGACY_BTN_HELP = "📋 Помощь"
    private const val LEGACY_BTN_STATUS = "📊 Статус"
    private const val LEGACY_BTN_LOAD = "📦 Лоуд"
    private const val LEGACY_BTN_PAY = "💰 Зарплата"
    private const val LEGACY_BTN_IMPORT = "📥 Импорт"
    private const val LEGACY_BTN_RESTORE = "🔄 Восстановить"

    /** Hide the old persistent reply keyboard so chat space is free. */
    fun removeReplyKeyboard(): JSONObject = JSONObject().apply {
        put("remove_keyboard", true)
    }

    fun defaultCommandsJson(): String = commandsArray().toString()

    fun commandsArray(): JSONArray = JSONArray().apply {
        put(command("start", "Start"))
        put(command("help", "How to use the bot"))
        put(command("status", "Data count in the app"))
        put(command("load", "How to send a load"))
        put(command("paycheck", "How to send a paycheck"))
        put(command("import", "Bulk import loads"))
        put(command("restore", "Restore loads from messages"))
        put(command("stats", "Loads: total and this week"))
        put(command("cancel", "Cancel import or restore"))
        put(command("dedup", "Remove duplicates from database"))
    }

    private fun command(name: String, description: String): JSONObject =
        JSONObject().apply {
            put("command", name)
            put("description", description)
        }

    fun isMenuButtonText(text: String): Boolean = when (text.trim()) {
        BTN_HELP, BTN_STATUS, BTN_LOAD, BTN_PAY, BTN_IMPORT, BTN_RESTORE,
        LEGACY_BTN_HELP, LEGACY_BTN_STATUS, LEGACY_BTN_LOAD, LEGACY_BTN_PAY,
        LEGACY_BTN_IMPORT, LEGACY_BTN_RESTORE,
        -> true
        else -> false
    }

    fun menuButtonToCommand(text: String): String? = when (text.trim()) {
        BTN_HELP, LEGACY_BTN_HELP -> "/help"
        BTN_STATUS, LEGACY_BTN_STATUS -> "/stats"
        BTN_LOAD, LEGACY_BTN_LOAD -> "/help_load"
        BTN_PAY, LEGACY_BTN_PAY -> "/help_pay"
        BTN_IMPORT, LEGACY_BTN_IMPORT -> "/import"
        BTN_RESTORE, LEGACY_BTN_RESTORE -> "/restore"
        else -> null
    }

    /** Menu slash aliases: /load and /paycheck open the same help as the old buttons. */
    fun aliasCommand(text: String): String {
        val t = text.trim()
        return when {
            isSlash(t, "/load") -> "/help_load"
            isSlash(t, "/paycheck") -> "/help_pay"
            else -> t
        }
    }

    fun isRestoreRequest(text: String): Boolean {
        val trimmed = text.trim()
        if (isRestoreCommand(trimmed)) return true
        val normalized = trimmed.lowercase()
            .replace('ё', 'е')
            .replace(Regex("[^a-zа-я0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (normalized.isBlank()) return false
        val keywords = listOf(
            "востонави",
            "восстанови",
            "восстановить",
            "восстановление",
            "restore"
        )
        return keywords.any { normalized.contains(it) }
    }

    private fun isRestoreCommand(text: String): Boolean =
        isSlash(text, "/restore") || isSlash(text, "/восстановить")

    private fun isSlash(text: String, command: String): Boolean {
        val t = text.trim()
        return t.equals(command, ignoreCase = true) ||
            t.startsWith("$command@", ignoreCase = true) ||
            t.startsWith("$command ", ignoreCase = true)
    }
}
