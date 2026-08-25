package com.truckerload.voice

import com.truckerload.presentation.navigation.Routes

/**
 * Voice commands map to real TruckoRig screens — the same destinations as
 * [Routes], the drawer, and the bottom tabs.
 */
sealed class AppVoiceAction {
    data class OpenScreen(val route: String) : AppVoiceAction()
    data class AddDiesel(
        val amount: Double?,
        val gallons: Double?,
        val date: String?,
    ) : AppVoiceAction()
    data class AddPaycheck(
        val amount: Double?,
        val weekNumber: Int?,
        val year: Int?,
    ) : AppVoiceAction()
    data class QueryWeeklyGross(
        val weekNumber: Int?,
        val year: Int?,
    ) : AppVoiceAction()
}

enum class VoiceFailReason {
    UNKNOWN,
}

object AppVoiceActions {
    const val SCHEME = "truckerload"
    const val HOST = "app"

    /**
     * User-reachable screens. Labels copy [R.string] nav/drawer/titles.
     * Longer phrases win so «цель недели» does not collide with shorter stems.
     */
    internal val screens: List<Pair<String, List<String>>> = listOf(
        Routes.HOME to listOf("грузы", "журнал", "loads", "logbook", "journal", "home"),
        Routes.ADD_LOAD to listOf("добавить груз", "новый груз", "add load", "new load"),
        Routes.STATS to listOf("цель недели", "недельная цель", "weekly goal", "цель", "goal"),
        Routes.ANALYTICS to listOf(
            "подробная статистика",
            "статистика",
            "аналитика",
            "отчёты",
            "отчеты",
            "analytics",
            "reports",
        ),
        Routes.MAP to listOf("карта штатов", "heatmap", "rpm map", "карта", "map"),
        Routes.PROFILE to listOf("профиль", "profile"),
        Routes.SETTINGS to listOf("настройки", "settings"),
        Routes.SCAN_GALLERY to listOf("документы", "галерея сканов", "documents"),
        Routes.PHOTO_GALLERY to listOf("галерея фото", "фото", "photo gallery"),
        Routes.MAINTENANCE to listOf("обслуживание", "maintenance", "то"),
        Routes.TAX_TRACKER to listOf("налоги", "налог", "taxes", "tax tracker", "tax"),
        Routes.ADD_PAYCHECK to listOf("добавить зарплату", "зарплата", "add paycheck", "paycheck"),
        Routes.DIESEL to listOf("дизель", "diesel"),
        Routes.ADD_DIESEL to listOf("добавить дизель", "add diesel"),
        Routes.SCANNER to listOf("сканер", "scanner"),
        Routes.CAMERA to listOf("камера", "camera"),
        Routes.ABOUT to listOf("о приложении", "about app", "about"),
        Routes.IMPROVE to listOf(
            "что улучшить",
            "предложить улучшение",
            "написать в поддержку",
            "what to improve",
            "suggest improvement",
        ),
        Routes.VOICE_ASSISTANT to listOf(
            "голосовой ассистент",
            "ассистент",
            "voice assistant",
            "assistant",
        ),
        "attach_pick/scanner" to listOf("скан к грузу", "scan load"),
        "attach_pick/camera" to listOf("фото к грузу", "photo load"),
    )

    fun parseUri(raw: String): AppVoiceAction? {
        val uri = raw.trim()
        if (!uri.startsWith("$SCHEME://")) return matchSpoken(uri)
        val rest = uri.removePrefix("$SCHEME://")
        val host = rest.substringBefore('/').substringBefore('?').lowercase()
        if (host != HOST && host != "assistant") return null
        val pathAndQuery = rest.substringAfter('/', missingDelimiterValue = "")
        val path = pathAndQuery.substringBefore('?').trim('/')
        val query = parseQuery(pathAndQuery.substringAfter('?', missingDelimiterValue = ""))
        val feature = query.firstOf("feature", "featurename")
        AppVoiceJournal.actionFromPath(path, query)?.let { return it }
        return when (path.lowercase()) {
            "open", "" -> {
                val spoken = feature ?: path
                AppVoiceJournal.fromSpokenQuery(spoken)
                    ?: matchSpoken(spoken)
                    ?: open(Routes.HOME)
            }
            else -> routeFromPath(path) ?: matchSpoken(path.replace('_', ' '))
        }
    }

    fun matchSpoken(spoken: String): AppVoiceAction? {
        val key = normalize(spoken)
        if (key.isBlank()) return null
        AppVoiceJournal.fromSpokenQuery(key)?.let { return it }
        val stripped = OPENER.replaceFirst(key, "").trim().ifBlank { key }
        AppVoiceJournal.fromSpokenQuery(stripped)?.let { return it }
        return matchScreen(stripped) ?: matchScreen(key)
    }

    internal fun phraseHits(spoken: String, phrase: String): Boolean {
        if (phrase.length <= 3) {
            return spoken == phrase || spoken.split(' ').contains(phrase)
        }
        if (spoken == phrase || spoken.contains(phrase)) return true
        return tokensInOrder(spoken.split(' '), phrase.split(' '))
    }

    private fun matchScreen(key: String): AppVoiceAction? {
        val route = screens
            .flatMap { (route, phrases) -> phrases.map { phrase -> route to normalize(phrase) } }
            .filter { (_, phrase) -> phrase.isNotBlank() && phraseHits(key, phrase) }
            .maxByOrNull { it.second.length }
            ?.first
        return route?.let { open(it) }
    }

    private fun routeFromPath(path: String): AppVoiceAction? {
        val route = path.trim('/')
        if (route.isBlank()) return open(Routes.HOME)
        val known = screens.map { it.first }.toSet()
        return if (route in known || route.startsWith("attach_pick/")) open(route) else null
    }

    private fun open(route: String) = AppVoiceAction.OpenScreen(route)

    fun normalize(raw: String): String =
        raw.trim().lowercase()
            .removePrefix("@")
            .replace(Regex("[\\p{Punct}&&[^_]]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    internal fun parseQuery(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        return raw.split("&").mapNotNull { pair ->
            if (pair.isBlank()) return@mapNotNull null
            val key = percentDecode(pair.substringBefore('=')).lowercase()
            val value = percentDecode(pair.substringAfter('=', missingDelimiterValue = ""))
            if (key.isBlank()) null else key to value
        }.toMap()
    }

    private fun Map<String, String>.firstOf(vararg keys: String): String? {
        keys.forEach { key ->
            val value = this[key]?.trim().orEmpty()
            if (value.isNotBlank()) return value
        }
        return null
    }

    internal fun percentDecode(value: String): String {
        val plus = value.replace('+', ' ')
        val bytes = ArrayList<Byte>(plus.length)
        var i = 0
        while (i < plus.length) {
            val ch = plus[i]
            if (ch == '%' && i + 2 < plus.length) {
                val parsed = plus.substring(i + 1, i + 3).toIntOrNull(16)
                if (parsed != null) {
                    bytes.add(parsed.toByte())
                    i += 3
                    continue
                }
            }
            val code = ch.code
            if (code <= 0x7F) {
                bytes.add(code.toByte())
            } else {
                ch.toString().encodeToByteArray().forEach { bytes.add(it) }
            }
            i++
        }
        return bytes.toByteArray().decodeToString()
    }

    private fun tokensInOrder(spoken: List<String>, phrase: List<String>): Boolean {
        if (phrase.isEmpty()) return false
        var i = 0
        for (token in phrase) {
            var found = false
            while (i < spoken.size) {
                if (tokenEq(spoken[i], token)) {
                    found = true
                    i++
                    break
                }
                i++
            }
            if (!found) return false
        }
        return true
    }

    private fun tokenEq(a: String, b: String): Boolean {
        if (a == b) return true
        val sa = stem(a)
        val sb = stem(b)
        return sa.length >= 3 && sa == sb
    }

    private fun stem(word: String): String {
        var current = word
        val endings = listOf("ами", "ах", "ой", "ей", "ом", "ам", "а", "у", "ы", "е", "и", "я", "ю", "ь")
        var changed = true
        while (changed) {
            changed = false
            for (ending in endings) {
                if (current.length - ending.length >= 3 && current.endsWith(ending)) {
                    current = current.removeSuffix(ending)
                    changed = true
                    break
                }
            }
        }
        return current
    }

    private val OPENER = Regex(
        "^(?:открой(?:те)?|открыть|open(?: the)?|show(?: me)?|покажи(?:те)?|go to|перейди(?:те)?|перейти)\\s+",
    )
}
