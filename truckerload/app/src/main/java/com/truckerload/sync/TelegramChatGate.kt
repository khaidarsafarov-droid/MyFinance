package com.truckerload.sync

import java.security.SecureRandom

/**
 * Pure Telegram chat authorization + OTP pairing (no Android dependencies).
 */
object TelegramPairingCodes {
    const val CODE_LENGTH = 6
    const val TTL_MS = 10L * 60L * 1000L

    fun generate(random: SecureRandom = SecureRandom()): String {
        val n = random.nextInt(1_000_000)
        return n.toString().padStart(CODE_LENGTH, '0')
    }

    /**
     * Extracts a 6-digit pairing code from `/start 123456`, `/pair 123456`,
     * `/start@bot 123456`, or a bare `123456` message.
     */
    fun extractFromMessage(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val bare = Regex("""^\d{$CODE_LENGTH}$""").find(trimmed)?.value
        if (bare != null) return bare
        val withCmd = Regex(
            """^/(?:start|pair)(?:@[A-Za-z0-9_]+)?(?:\s+|_)(\d{$CODE_LENGTH})\s*$""",
            RegexOption.IGNORE_CASE,
        ).find(trimmed)
        return withCmd?.groupValues?.getOrNull(1)
    }

    fun matches(expected: String?, provided: String?): Boolean {
        if (expected.isNullOrBlank() || provided.isNullOrBlank()) return false
        return expected.trim() == provided.trim()
    }

    fun isActive(expiresAtMillis: Long?, nowMillis: Long): Boolean =
        expiresAtMillis != null && expiresAtMillis > nowMillis
}

sealed class TelegramAuthDecision {
    /** Chat is already paired or just paired — continue handling. */
    data object Allow : TelegramAuthDecision()

    /** Pair succeeded; caller must persist [chatId] then continue. */
    data class PairAndAllow(val chatId: Long) : TelegramAuthDecision()

    /** Wrong chat for an already-paired bot. */
    data object RejectUnauthorized : TelegramAuthDecision()

    /** Unpaired bot and message is not a valid pairing attempt. */
    data object RejectNeedPairCode : TelegramAuthDecision()

    /** Pairing code present but wrong/expired. */
    data object RejectBadPairCode : TelegramAuthDecision()
}

/**
 * Decides whether an incoming Telegram update may touch local data.
 */
object TelegramChatGate {
    fun decide(
        allowedChatId: Long?,
        incomingChatId: Long?,
        text: String,
        expectedPairCode: String?,
        pairCodeExpiresAtMillis: Long?,
        nowMillis: Long = System.currentTimeMillis(),
    ): TelegramAuthDecision {
        if (incomingChatId == null) return TelegramAuthDecision.RejectUnauthorized
        if (allowedChatId != null) {
            return if (incomingChatId == allowedChatId) {
                TelegramAuthDecision.Allow
            } else {
                TelegramAuthDecision.RejectUnauthorized
            }
        }
        val code = TelegramPairingCodes.extractFromMessage(text)
        if (code == null) return TelegramAuthDecision.RejectNeedPairCode
        if (!TelegramPairingCodes.isActive(pairCodeExpiresAtMillis, nowMillis) ||
            !TelegramPairingCodes.matches(expectedPairCode, code)
        ) {
            return TelegramAuthDecision.RejectBadPairCode
        }
        return TelegramAuthDecision.PairAndAllow(incomingChatId)
    }
}
