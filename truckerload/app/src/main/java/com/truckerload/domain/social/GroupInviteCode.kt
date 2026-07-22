package com.truckerload.domain.social

/** Pure invite-code helpers for group join-by-code. */
object GroupInviteCode {

    fun normalize(raw: String): String = raw.trim()

    /** Blank / whitespace-only codes are unusable (UI should not call join). */
    fun isBlank(raw: String): Boolean = normalize(raw).isBlank()

    /**
     * Loose format check: non-blank, length 4–16, letters/digits only after normalize.
     * Unknown-but-well-formed codes still fail at the repository lookup.
     */
    fun isWellFormed(raw: String): Boolean {
        val code = normalize(raw)
        if (code.length !in 4..16) return false
        return code.all { it.isLetterOrDigit() }
    }
}
