package com.truckerload.domain.friends

/**
 * Validates Truck Log nicknames used to find / add friends.
 * Rules: 3–24 chars, starts with a letter, then letters/digits/underscore.
 */
object NicknameValidator {
    private val PATTERN = Regex("^[A-Za-z][A-Za-z0-9_]{2,23}$")

    fun normalize(raw: String): String = raw.trim().removePrefix("@")

    fun isValid(raw: String): Boolean = PATTERN.matches(normalize(raw))

    fun sanitizeOrNull(raw: String): String? {
        val n = normalize(raw)
        return n.takeIf { isValid(it) }
    }
}

data class FriendProfileHit(
    val userId: String,
    val nickname: String,
    val displayName: String,
)

/**
 * People I share my location/route with (outgoing share list).
 */
data class FriendShareLink(
    val friendUserId: String,
    val friendNickname: String,
    val friendDisplayName: String,
    val shareMyLocation: Boolean,
    val shareMyRoute: Boolean,
)
