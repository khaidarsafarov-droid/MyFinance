package com.truckerload.data.social

import com.truckerload.domain.social.ModerationCodes
import com.truckerload.domain.social.ModerationResult

/**
 * In-app chat filters: keep conversation inside Truck Log and keep
 * phone/email off public messages (Play/App Store UGC safety).
 */
object ContentModerator {
    private val blockedPatterns = listOf(
        Regex("(?i)\\bspam\\b") to ModerationCodes.SPAM,
        Regex("(?i)\\bscam\\b") to ModerationCodes.SPAM,
        Regex("(?i)https?://") to ModerationCodes.LINK,
        Regex("(?i)(t\\.me/|telegram\\.me|wa\\.me/|whatsapp\\.com)") to ModerationCodes.OFF_APP,
        Regex("(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}") to ModerationCodes.PII_EMAIL,
        Regex("(?<![\\dA-Za-z])(?:\\+\\d[\\d\\s().-]{8,}\\d|\\(?\\d{3}\\)?[\\s.-]\\d{3}[\\s.-]\\d{4}|\\d{10,})") to
            ModerationCodes.PII_PHONE,
    )

    fun moderateText(text: String): ModerationResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ModerationResult(false, ModerationCodes.EMPTY)
        if (trimmed.length > 4000) return ModerationResult(false, ModerationCodes.TOO_LONG)
        blockedPatterns.forEach { (pattern, code) ->
            if (pattern.containsMatchIn(trimmed)) {
                return ModerationResult(false, code)
            }
        }
        return ModerationResult(true)
    }

    fun extractHashtags(text: String): List<String> =
        Regex("#[\\w\\u0400-\\u04FF]+").findAll(text).map { it.value.removePrefix("#") }.toList()

    fun extractMentions(text: String): List<String> =
        Regex("@[\\w\\u0400-\\u04FF]+").findAll(text).map { it.value.removePrefix("@") }.toList()
}
