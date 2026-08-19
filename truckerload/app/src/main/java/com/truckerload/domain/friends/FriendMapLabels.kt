package com.truckerload.domain.friends

/** Visible names and initials for friends-map person markers. */
object FriendMapLabels {
    fun friendVisibleName(
        presenceDisplayName: String,
        linkDisplayName: String?,
        nickname: String?,
    ): String {
        val fromLink = linkDisplayName?.trim().orEmpty()
        if (fromLink.isNotBlank()) return fromLink
        val presence = presenceDisplayName.trim()
        if (presence.isNotBlank() && presence !in PLACEHOLDER_NAMES) return presence
        val nick = nickname?.trim()?.removePrefix("@").orEmpty()
        if (nick.isNotBlank()) return nick
        return presence
    }

    fun initials(name: String): String {
        val cleaned = name.trim().removePrefix("@")
        if (cleaned.isEmpty()) return "?"
        val parts = cleaned.split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when {
            parts.size >= 2 ->
                "${parts[0].first()}${parts[1].first()}".uppercase()
            else -> cleaned.take(2).uppercase()
        }
    }

    fun ellipsize(label: String, maxChars: Int = 18): String {
        val trimmed = label.trim()
        if (trimmed.length <= maxChars) return trimmed
        return trimmed.take((maxChars - 1).coerceAtLeast(1)) + "…"
    }

    private val PLACEHOLDER_NAMES = setOf("Driver", "Водитель", "User", "You")
}
