package com.truckerload.voice

import com.truckerload.presentation.navigation.Routes

/**
 * Voice commands are the real Truck Log screens and social actions —
 * the same destinations as [Routes], the drawer, the tabs, and community.
 * Phrases are the in-app labels (RU + EN), not a generic assistant catalog.
 */
sealed class AppVoiceAction {
    data class OpenScreen(val route: String) : AppVoiceAction()
    data class ChatWithFriend(val peerQuery: String) : AppVoiceAction()
    data class MessageFriend(val peerQuery: String, val text: String) : AppVoiceAction()
    data class CallFriend(val peerQuery: String) : AppVoiceAction()
}

data class VoicePeerRef(val id: String, val displayName: String)

sealed class VoicePeerMatch {
    data object None : VoicePeerMatch()
    data class Unique(val peer: VoicePeerRef) : VoicePeerMatch()
    data class Ambiguous(val candidates: List<VoicePeerRef>) : VoicePeerMatch()
}

enum class VoiceFailReason {
    UNKNOWN,
    PEER_NOT_FOUND,
    NOT_SIGNED_IN,
}

object AppVoiceActions {
    const val SCHEME = "truckerload"
    const val HOST = "app"

    /**
     * User-reachable screens. Labels copy [R.string] nav/drawer/titles.
     * Longer phrases win so «карта друзей» does not open drawer «Карта».
     */
    internal val screens: List<Pair<String, List<String>>> = listOf(
        Routes.HOME to listOf("грузы", "журнал", "loads", "logbook", "journal", "home"),
        Routes.ADD_LOAD to listOf("добавить груз", "новый груз", "add load", "new load"),
        Routes.STATS to listOf("цель недели", "недельная цель", "weekly goal", "цель", "goal"),
        Routes.ANALYTICS to listOf("аналитика", "отчёты", "отчеты", "analytics", "reports"),
        Routes.ADVANCED_STATS to listOf(
            "подробная статистика",
            "расширенная статистика",
            "advanced stats",
        ),
        Routes.FRIENDS_LIVE to listOf(
            "друзья на карте",
            "карта друзей",
            "friends live",
            "friends on map",
            "friends map",
            "live map",
        ),
        Routes.MAP to listOf("карта штатов", "heatmap", "rpm map", "карта", "map"),
        Routes.COMMUNITY to listOf(
            "сообщество",
            "чаты",
            "таблица лидеров",
            "челленджи",
            "community",
            "chats",
            "leaderboard",
            "challenges",
        ),
        Routes.PROFILE to listOf("профиль", "profile"),
        Routes.PROFILE_EDIT to listOf("редактировать профиль", "edit profile"),
        Routes.SETTINGS to listOf("настройки", "settings"),
        Routes.SCAN_GALLERY to listOf("документы", "галерея сканов", "documents"),
        Routes.PHOTO_GALLERY to listOf("галерея фото", "фото", "photo gallery"),
        Routes.MAINTENANCE to listOf("обслуживание", "maintenance", "то"),
        Routes.ADD_PAYCHECK to listOf("добавить зарплату", "зарплата", "add paycheck", "paycheck"),
        Routes.ADD_DIESEL to listOf("добавить дизель", "дизель", "add diesel", "diesel"),
        Routes.SCANNER to listOf("сканер", "scanner"),
        Routes.CAMERA to listOf("камера", "camera"),
        Routes.ABOUT to listOf("о приложении", "about app", "about"),
        Routes.VOICE_ROOMS to listOf("голосовые комнаты", "voice rooms"),
        Routes.STATUS to listOf("статусы", "statuses"),
        Routes.GROUPS to listOf("группы", "groups"),
        Routes.FINANCIAL_ADVISOR to listOf(
            "финансовый советник",
            "советник",
            "financial advisor",
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
        val peer = query.firstOf("peer", "recipientname", "calleename", "name", "q")
        val text = query.firstOf("text", "messagetext", "message")
        val feature = query.firstOf("feature", "featurename")
        return when (path.lowercase()) {
            "chat" -> peer?.let { AppVoiceAction.ChatWithFriend(it) } ?: open(Routes.COMMUNITY)
            "message" -> if (!peer.isNullOrBlank()) {
                AppVoiceAction.MessageFriend(peer, text.orEmpty())
            } else {
                open(Routes.COMMUNITY)
            }
            "call" -> peer?.let { AppVoiceAction.CallFriend(it) } ?: open(Routes.VOICE_ROOMS)
            "open", "" -> matchSpoken(feature ?: path) ?: open(Routes.HOME)
            else -> routeFromPath(path) ?: matchSpoken(path.replace('_', ' '))
        }
    }

    fun matchSpoken(spoken: String): AppVoiceAction? {
        val key = normalize(spoken)
        if (key.isBlank()) return null
        parameterized(key)?.let { return it }
        val stripped = OPENER.replaceFirst(key, "").trim().ifBlank { key }
        return matchScreen(stripped) ?: matchScreen(key)
    }

    fun matchPeers(query: String, peers: List<VoicePeerRef>): VoicePeerMatch {
        val needle = normalize(query)
        if (needle.isBlank() || peers.isEmpty()) return VoicePeerMatch.None
        val hits = peers.filter { peer ->
            val name = normalize(peer.displayName)
            name == needle || name.contains(needle) || needle.contains(name)
        }.distinctBy { it.id }
        return when {
            hits.isEmpty() -> VoicePeerMatch.None
            hits.size == 1 -> VoicePeerMatch.Unique(hits.first())
            else -> VoicePeerMatch.Ambiguous(hits)
        }
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

    private fun parameterized(key: String): AppVoiceAction? {
        CALL_PREFIX.find(key)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }?.let {
            return AppVoiceAction.CallFriend(it)
        }
        WRITE_PREFIX.find(key)?.let { match ->
            val peer = match.groupValues.getOrNull(1)?.trim().orEmpty()
            val text = match.groupValues.getOrNull(2)?.trim().orEmpty()
            if (peer.isBlank()) return@let
            return if (text.isBlank()) {
                AppVoiceAction.ChatWithFriend(peer)
            } else {
                AppVoiceAction.MessageFriend(peer, text)
            }
        }
        CHAT_WITH.find(key)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }?.let {
            return AppVoiceAction.ChatWithFriend(it)
        }
        ADD_FRIEND_PREFIX.find(key)?.let { return open(Routes.COMMUNITY) }
        return null
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
    private val CALL_PREFIX = Regex("^(?:позвони|позвонить|call)\\s+(.+)$")
    private val WRITE_PREFIX = Regex(
        "^(?:напиши|написать|отправь сообщение|отправь|write|send message|message)\\s+(\\S+)(?:\\s+(.+))?$",
    )
    private val CHAT_WITH = Regex("^(?:чат с|chat with)\\s+(.+)$")
    private val ADD_FRIEND_PREFIX = Regex(
        "^(?:добав(?:ь|ить) дру(?:га|зей|г)|add friends?)(?:\\s+.+)?$",
    )
}
