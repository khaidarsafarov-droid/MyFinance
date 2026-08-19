package com.truckerload.domain.voice

/**
 * Call UX constants. Group size is a configuration value, not a magic number in UI.
 */
object CallConfig {
    /** Auto-miss if nobody answers (30–45s window). */
    const val RING_TIMEOUT_MS = 40_000L

    /** Outgoing ring delay before offering a voice message instead. */
    const val OFFLINE_HINT_MS = 8_000L

    const val DEFAULT_MAX_GROUP_PARTICIPANTS = 8

    fun groupRoomId(chatId: String): String = "group_${chatId.trim().take(80)}"
}
