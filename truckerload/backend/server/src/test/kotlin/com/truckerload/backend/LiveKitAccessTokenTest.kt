package com.truckerload.backend

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiveKitAccessTokenTest {
    @Test
    fun `mints hs256 jwt with livekit video grants`() {
        val now = Instant.parse("2026-08-19T04:00:00Z")
        val jwt = LiveKitAccessToken("devkey", "super-secret").mint(
            identity = "11111111-1111-4111-8111-111111111111",
            name = "Alex",
            roomName = "tl-voice-room-1",
            canPublish = true,
            now = now,
        )
        val decoded = JWT.require(Algorithm.HMAC256("super-secret"))
            .withIssuer("devkey")
            .acceptLeeway(TimeUnit.DAYS.toSeconds(1))
            .build()
            .verify(jwt)
        assertEquals("11111111-1111-4111-8111-111111111111", decoded.subject)
        assertEquals("devkey", decoded.issuer)
        assertEquals("Alex", decoded.getClaim("name").asString())
        val video = decoded.getClaim("video").asMap()
        assertEquals(true, video["roomJoin"])
        assertEquals("tl-voice-room-1", video["room"])
        assertEquals(true, video["canPublish"])
        assertEquals(true, video["canSubscribe"])
        assertEquals(false, video["canPublishData"])
        assertEquals(now.plusSeconds(LiveKitAccessToken.DEFAULT_TTL_SECONDS).epochSecond, decoded.expiresAtAsInstant.epochSecond)
    }

    @Test
    fun `listener token cannot publish`() {
        val jwt = LiveKitAccessToken("devkey", "super-secret").mint(
            identity = "user-1",
            name = "Sam",
            roomName = LiveKitAccessToken.roomName("abc"),
            canPublish = false,
        )
        val video = JWT.decode(jwt).getClaim("video").asMap()
        assertFalse(video["canPublish"] as Boolean)
        assertTrue(video["canSubscribe"] as Boolean)
        assertEquals("tl-voice-abc", video["room"])
    }
}
