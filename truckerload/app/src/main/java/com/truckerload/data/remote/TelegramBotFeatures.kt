package com.truckerload.data.remote

import org.json.JSONArray
import org.json.JSONObject

/**
 * Telegram Bot API helpers: commands menu, reply keyboard, inline buttons.
 */
object TelegramBotFeatures {

    fun mainMenuKeyboard(): JSONObject = JSONObject().apply {
        put("keyboard", JSONArray().apply {
            put(JSONArray().apply {
                put("📋 Помощь")
                put("📊 Статус")
            })
            put(JSONArray().apply {
                put("📦 Лоуд")
                put("💰 Зарплата")
            })
            put(JSONArray().apply {
                put("📥 Импорт")
                put("🔄 Восстановить")
            })
        })
        put("resize_keyboard", true)
        put("is_persistent", true)
    }

    fun defaultCommandsJson(): String {
        val commands = JSONArray().apply {
            put(command("start", "Запуск и меню"))
            put(command("help", "Как пользоваться ботом"))
            put(command("status", "Сколько данных в приложении"))
            put(command("stats", "Лоуды: всего и за неделю"))
            put(command("import", "Массовый импорт лоудов"))
            put(command("cancel", "Отменить импорт"))
        put(command("restore", "Восстановить лоуды из сообщений"))
        }
        return commands.toString()
    }

    fun commandsArray(): JSONArray = JSONArray().apply {
        put(command("start", "Запуск и меню"))
        put(command("help", "Как пользоваться ботом"))
        put(command("status", "Сколько данных в приложении"))
        put(command("stats", "Лоуды: всего и за неделю"))
        put(command("import", "Массовый импорт лоудов"))
        put(command("cancel", "Отменить импорт"))
        put(command("restore", "Восстановить лоуды из сообщений"))
    }

    private fun command(name: String, description: String): JSONObject =
        JSONObject().apply {
            put("command", name)
            put("description", description)
        }

    fun isMenuButtonText(text: String): Boolean = when (text.trim()) {
        "📋 Помощь", "📊 Статус", "📦 Лоуд", "💰 Зарплата", "📥 Импорт", "🔄 Восстановить" -> true
        else -> false
    }

    fun menuButtonToCommand(text: String): String? = when (text.trim()) {
        "📋 Помощь" -> "/help"
        "📊 Статус" -> "/stats"
        "📦 Лоуд" -> "/help_load"
        "💰 Зарплата" -> "/help_pay"
        "📥 Импорт" -> "/import"
        "🔄 Восстановить" -> "/restore"
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
