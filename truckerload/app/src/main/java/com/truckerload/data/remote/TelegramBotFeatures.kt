package com.truckerload.data.remote

import org.json.JSONArray
import org.json.JSONObject

/**
 * Telegram Bot API helpers: commands menu, reply keyboard, inline buttons.
 */
object TelegramBotFeatures {

    private const val BTN_HELP = "📋 Help"
    private const val BTN_STATUS = "📊 Status"
    private const val BTN_LOAD = "📦 Load"
    private const val BTN_PAY = "💰 Paycheck"
    private const val BTN_IMPORT = "📥 Import"
    private const val BTN_RESTORE = "🔄 Restore"

    // Legacy Russian labels — still accepted so existing keyboards keep working.
    private const val LEGACY_BTN_HELP = "📋 Помощь"
    private const val LEGACY_BTN_STATUS = "📊 Статус"
    private const val LEGACY_BTN_LOAD = "📦 Лоуд"
    private const val LEGACY_BTN_PAY = "💰 Зарплата"
    private const val LEGACY_BTN_IMPORT = "📥 Импорт"
    private const val LEGACY_BTN_RESTORE = "🔄 Восстановить"

    fun mainMenuKeyboard(): JSONObject = JSONObject().apply {
        put("keyboard", JSONArray().apply {
            put(JSONArray().apply {
                put(BTN_HELP)
                put(BTN_STATUS)
            })
            put(JSONArray().apply {
                put(BTN_LOAD)
                put(BTN_PAY)
            })
            put(JSONArray().apply {
                put(BTN_IMPORT)
                put(BTN_RESTORE)
            })
        })
        put("resize_keyboard", true)
        put("is_persistent", true)
    }

    fun defaultCommandsJson(): String {
        val commands = JSONArray().apply {
            put(command("start", "Start and menu"))
            put(command("help", "How to use the bot"))
            put(command("status", "Data count in the app"))
            put(command("stats", "Loads: total and this week"))
            put(command("import", "Bulk import loads"))
            put(command("dedup", "Remove duplicates from database"))
            put(command("cancel", "Cancel import"))
            put(command("restore", "Restore loads from messages"))
        }
        return commands.toString()
    }

    fun commandsArray(): JSONArray = JSONArray().apply {
        put(command("start", "Start and menu"))
        put(command("help", "How to use the bot"))
        put(command("status", "Data count in the app"))
        put(command("stats", "Loads: total and this week"))
        put(command("import", "Bulk import loads"))
        put(command("dedup", "Remove duplicates from database"))
        put(command("cancel", "Cancel import"))
        put(command("restore", "Restore loads from messages"))
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

    private fun isRestoreCommand(text: String): Boolean {
        val t = text.trim()
        return t.equals("/restore", ignoreCase = true) ||
            t.startsWith("/restore@", ignoreCase = true) ||
            t.startsWith("/restore ", ignoreCase = true) ||
            t.equals("/восстановить", ignoreCase = true) ||
            t.startsWith("/восстановить@", ignoreCase = true) ||
            t.startsWith("/восстановить ", ignoreCase = true)
    }
}
