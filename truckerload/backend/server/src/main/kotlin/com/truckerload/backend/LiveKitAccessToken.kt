package com.truckerload.backend

import io.livekit.server.AccessToken
import io.livekit.server.CanPublish
import io.livekit.server.CanPublishData
import io.livekit.server.CanSubscribe
import io.livekit.server.RoomJoin
import io.livekit.server.RoomName
import java.time.Instant
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Mints a LiveKit access token with [io.livekit.server.AccessToken].
 * The API secret never leaves the server.
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
        val ttl = ttlSeconds.coerceIn(60L, MAX_TTL_SECONDS)
        val token = AccessToken(apiKey, apiSecret)
        token.identity = identity
        token.name = name.take(64)
        token.ttl = TimeUnit.MILLISECONDS.convert(ttl, TimeUnit.SECONDS)
        token.notBefore = Date.from(now)
        token.expiration = Date.from(now.plusSeconds(ttl))
        token.addGrants(
            RoomJoin(true),
            RoomName(roomName),
            CanPublish(canPublish),
            CanSubscribe(true),
            CanPublishData(false),
        )
        return token.toJwt()
    }

    companion object {
        const val DEFAULT_TTL_SECONDS = 6L * 60 * 60
        const val MAX_TTL_SECONDS = 12L * 60 * 60
        const val ROOM_PREFIX = "tl-voice-"

        fun roomName(communityRoomId: String): String = ROOM_PREFIX + communityRoomId
    }
}
