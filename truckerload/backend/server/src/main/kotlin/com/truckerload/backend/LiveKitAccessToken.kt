package com.truckerload.backend

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.time.Instant

/**
 * Mints a LiveKit access token (HS256). The API secret never leaves the server.
 *
 * Claim shape matches LiveKit server: `iss`/`kid` = API key, `sub` = identity,
 * `video.roomJoin` + `video.room` + publish/subscribe grants.
 */
class LiveKitAccessToken(
    private val apiKey: String,
    private val apiSecret: String,
) {
    fun mint(
        identity: String,
        name: String,
        roomName: String,
        canPublish: Boolean,
        ttlSeconds: Long = DEFAULT_TTL_SECONDS,
        now: Instant = Instant.now(),
    ): String {
        require(identity.isNotBlank()) { "identity required" }
        require(roomName.isNotBlank()) { "roomName required" }
        val expires = now.plusSeconds(ttlSeconds.coerceIn(60L, MAX_TTL_SECONDS))
        val video = linkedMapOf<String, Any>(
            "roomJoin" to true,
            "room" to roomName,
            "canPublish" to canPublish,
            "canSubscribe" to true,
            "canPublishData" to false,
        )
        return JWT.create()
            .withIssuer(apiKey)
            .withKeyId(apiKey)
            .withSubject(identity)
            .withJWTId(identity)
            .withIssuedAt(now)
            .withNotBefore(now)
            .withExpiresAt(expires)
            .withClaim("name", name.take(64))
            .withClaim("video", video)
            .sign(Algorithm.HMAC256(apiSecret))
    }

    companion object {
        const val DEFAULT_TTL_SECONDS = 6L * 60 * 60
        const val MAX_TTL_SECONDS = 12L * 60 * 60
        const val ROOM_PREFIX = "tl-voice-"

        fun roomName(communityRoomId: String): String = ROOM_PREFIX + communityRoomId
    }
}
