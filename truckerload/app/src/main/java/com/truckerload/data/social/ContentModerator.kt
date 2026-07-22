package com.truckerload.data.social

import com.truckerload.domain.social.ModerationResult

object ContentModerator {
    private val blockedPatterns = listOf(
        Regex("(?i)spam"),
        Regex("(?i)scam"),
        Regex("(?i)http://"),
        Regex("(?i)https://"),
    )

    fun moderateText(text: String): ModerationResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ModerationResult(false, "Empty message")
        if (trimmed.length > 4000) return ModerationResult(false, "Message too long")
        blockedPatterns.forEach { pattern ->
            if (pattern.containsMatchIn(trimmed)) {
                return ModerationResult(false, "Message blocked by moderation")
            }
        }
        return ModerationResult(true)
    }

    fun extractHashtags(text: String): List<String> =
        Regex("#[\\w\\u0400-\\u04FF]+").findAll(text).map { it.value.removePrefix("#") }.toList()

    fun extractMentions(text: String): List<String> =
        Regex("@[\\w\\u0400-\\u04FF]+").findAll(text).map { it.value.removePrefix("@") }.toList()
}
