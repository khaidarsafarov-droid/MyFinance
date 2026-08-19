package com.truckerload.backend

import com.auth0.jwt.JWT
import com.truckerload.contract.ApiError
import com.truckerload.contract.ContractJson
import com.truckerload.contract.VoiceTokenRequest
import com.truckerload.contract.VoiceTokenResponse
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VoiceRoutesTest {
    private val userOne = UUID.fromString("11111111-1111-4111-8111-111111111111")

    @Test
    fun `voice token is unavailable when livekit is not configured`() = testApplication {
        val backend = InMemoryBackend()
        application {
            configureApplication(AppConfig.test(), AppDependencies(backend.repositories, FakeObjectStorage()))
        }
        val response = jsonClient().post("/v1/voice/token") {
            bearerAuth(token(userOne))
            contentType(ContentType.Application.Json)
            setBody(VoiceTokenRequest("room-1", "Alex", "speaker"))
        }
        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertEquals("voice_sfu_disabled", response.body<ApiError>().code)
    }

    @Test
    fun `voice token requires authentication`() = testApplication {
        val backend = InMemoryBackend()
        application {
            configureApplication(liveKitConfig(), AppDependencies(backend.repositories, FakeObjectStorage()))
        }
        val response = jsonClient().post("/v1/voice/token") {
            contentType(ContentType.Application.Json)
            setBody(VoiceTokenRequest("room-1", "Alex", "speaker"))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `voice token mints speaker and listener grants`() = testApplication {
        val backend = InMemoryBackend()
        application {
            configureApplication(liveKitConfig(), AppDependencies(backend.repositories, FakeObjectStorage()))
        }
        val client = jsonClient()
        val speaker = client.post("/v1/voice/token") {
            bearerAuth(token(userOne))
            contentType(ContentType.Application.Json)
            setBody(VoiceTokenRequest("room-1", "Alex", "speaker"))
        }.body<VoiceTokenResponse>()
        assertEquals("wss://voice.example.livekit.cloud", speaker.url)
        assertEquals("tl-voice-room-1", speaker.roomName)
        assertEquals(userOne.toString(), speaker.identity)
        assertEquals("speaker", speaker.role)
        assertEquals(16_000, speaker.audioBitrate)
        val speakerVideo = JWT.decode(speaker.token).getClaim("video").asMap()
        assertEquals(true, speakerVideo["canPublish"])

        val listener = client.post("/v1/voice/token") {
            bearerAuth(token(userOne))
            contentType(ContentType.Application.Json)
            setBody(VoiceTokenRequest("room-1", "Alex", "listener"))
        }.body<VoiceTokenResponse>()
        assertEquals("listener", listener.role)
        assertEquals(0, listener.audioBitrate)
        assertEquals(false, JWT.decode(listener.token).getClaim("video").asMap()["canPublish"])
    }

    @Test
    fun `voice token rejects invalid role`() = testApplication {
        val backend = InMemoryBackend()
        application {
            configureApplication(liveKitConfig(), AppDependencies(backend.repositories, FakeObjectStorage()))
        }
        val response = jsonClient().post("/v1/voice/token") {
            bearerAuth(token(userOne))
            contentType(ContentType.Application.Json)
            setBody(VoiceTokenRequest("room-1", "Alex", "admin"))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("invalid_role", response.body<ApiError>().code)
        assertTrue(response.body<ApiError>().message.contains("speaker"))
    }

    private fun io.ktor.server.testing.ApplicationTestBuilder.jsonClient() = createClient {
        install(ContentNegotiation) {
            json(ContractJson)
        }
    }

    private fun token(userId: UUID): String = "test.$userId"

    private fun liveKitConfig(): AppConfig = AppConfig.test().copy(
        liveKitUrl = "wss://voice.example.livekit.cloud",
        liveKitApiKey = "devkey",
        liveKitApiSecret = "super-secret",
    )
}
