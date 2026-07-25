package com.truckerload.backend

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.truckerload.contract.AccountCloudSnapshot
import com.truckerload.contract.ContractJson
import com.truckerload.contract.HealthResponse
import com.truckerload.contract.MediaUploadRequest
import com.truckerload.contract.MediaUploadResponse
import com.truckerload.contract.TelegramInboxListResponse
import com.truckerload.contract.TelegramLinkTokenResponse
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

class ApplicationTest {
    private val userOne = UUID.fromString("11111111-1111-4111-8111-111111111111")
    private val userTwo = UUID.fromString("22222222-2222-4222-8222-222222222222")

    @Test
    fun `live and ready health endpoints work without authentication`() = testApplication {
        val backend = InMemoryBackend()
        application {
            configureApplication(AppConfig.test(), AppDependencies(backend.repositories, FakeObjectStorage()))
        }
        val client = jsonClient()

        val live = client.get("/health/live")
        assertEquals(HttpStatusCode.OK, live.status)
        assertEquals("ok", live.body<HealthResponse>().status)

        val ready = client.get("/health/ready")
        assertEquals(HttpStatusCode.OK, ready.status)
        assertEquals("ok", ready.body<HealthResponse>().status)
    }

    @Test
    fun `protected endpoints reject missing and malformed credentials`() = testApplication {
        val backend = InMemoryBackend()
        application {
            configureApplication(AppConfig.test(), AppDependencies(backend.repositories, FakeObjectStorage()))
        }

        assertEquals(HttpStatusCode.Unauthorized, client.get("/v1/sync/snapshot").status)
        assertEquals(
            HttpStatusCode.Unauthorized,
            client.get("/v1/sync/snapshot") { bearerAuth("definitely-invalid") }.status,
        )
    }

    @Test
    fun `JWT authentication enforces issuer and audience`() = testApplication {
        val backend = InMemoryBackend()
        val config = AppConfig.test().copy(testAuthEnabled = false)
        application {
            configureApplication(config, AppDependencies(backend.repositories, FakeObjectStorage()))
        }
        val valid = JWT.create()
            .withSubject(userOne.toString())
            .withIssuer(config.jwtIssuer)
            .withAudience(config.jwtAudience)
            .sign(Algorithm.HMAC256(config.jwtSecret))
        val wrongAudience = JWT.create()
            .withSubject(userOne.toString())
            .withIssuer(config.jwtIssuer)
            .withAudience("wrong")
            .sign(Algorithm.HMAC256(config.jwtSecret))

        assertEquals(
            HttpStatusCode.NoContent,
            client.get("/v1/sync/snapshot") { bearerAuth(valid) }.status,
        )
        assertEquals(
            HttpStatusCode.Unauthorized,
            client.get("/v1/sync/snapshot") { bearerAuth(wrongAudience) }.status,
        )
    }

    @Test
    fun `snapshots are user scoped and strict timestamp last-write-wins`() = testApplication {
        val backend = InMemoryBackend()
        application {
            configureApplication(AppConfig.test(), AppDependencies(backend.repositories, FakeObjectStorage()))
        }
        val client = jsonClient()
        val newer = snapshot(userOne, updatedAt = 200)
        val stale = snapshot(userOne, updatedAt = 100)

        assertEquals(
            HttpStatusCode.OK,
            client.put("/v1/sync/snapshot") {
                bearerAuth(token(userOne))
                contentType(ContentType.Application.Json)
                setBody(newer)
            }.status,
        )
        val staleResponse = client.put("/v1/sync/snapshot") {
            bearerAuth(token(userOne))
            contentType(ContentType.Application.Json)
            setBody(stale)
        }
        assertEquals(200, staleResponse.body<AccountCloudSnapshot>().updatedAt)

        assertEquals(
            HttpStatusCode.NoContent,
            client.get("/v1/sync/snapshot") { bearerAuth(token(userTwo)) }.status,
        )
        assertEquals(
            HttpStatusCode.NoContent,
            client.get("/v1/sync/snapshot?since=200") { bearerAuth(token(userOne)) }.status,
        )
        assertEquals(
            HttpStatusCode.Forbidden,
            client.put("/v1/sync/snapshot") {
                bearerAuth(token(userTwo))
                contentType(ContentType.Application.Json)
                setBody(newer)
            }.status,
        )
    }

    @Test
    fun `Telegram webhook enforces secret and link tokens are one-time and inbox is idempotent`() =
        testApplication {
            val backend = InMemoryBackend()
            val config = AppConfig.test()
            application {
                configureApplication(config, AppDependencies(backend.repositories, FakeObjectStorage()))
            }
            val client = jsonClient()

            val rejected = client.post("/v1/telegram/webhook") {
                contentType(ContentType.Application.Json)
                setBody(telegramMessage(1, 10, "/start invalid"))
            }
            assertEquals(HttpStatusCode.Unauthorized, rejected.status)

            val link = client.post("/v1/telegram/link-token") {
                bearerAuth(token(userOne))
            }.body<TelegramLinkTokenResponse>()
            assertTrue(link.token.length >= 40)

            val start = client.post("/v1/telegram/webhook") {
                header("X-Telegram-Bot-Api-Secret-Token", config.telegramWebhookSecret)
                contentType(ContentType.Application.Json)
                setBody(telegramMessage(2, 10, "/start ${link.token}"))
            }
            assertEquals(HttpStatusCode.OK, start.status)

            val secondChat = client.post("/v1/telegram/webhook") {
                header("X-Telegram-Bot-Api-Secret-Token", config.telegramWebhookSecret)
                contentType(ContentType.Application.Json)
                setBody(telegramMessage(3, 99, "/start ${link.token}"))
            }
            assertEquals(HttpStatusCode.OK, secondChat.status)
            assertNotEquals(userOne, backend.links[99])

            repeat(2) {
                val response = client.post("/v1/telegram/webhook") {
                    header("X-Telegram-Bot-Api-Secret-Token", config.telegramWebhookSecret)
                    contentType(ContentType.Application.Json)
                    setBody(telegramMessage(50, 10, "Trip ID: T-123"))
                }
                assertEquals(HttpStatusCode.OK, response.status)
            }
            val inbox = client.get("/v1/telegram/inbox") {
                bearerAuth(token(userOne))
            }.body<TelegramInboxListResponse>()
            assertEquals(1, inbox.items.size)
            assertEquals(50, inbox.items.single().updateId)
        }

    @Test
    fun `media metadata cannot be read by another account`() = testApplication {
        val backend = InMemoryBackend()
        application {
            configureApplication(AppConfig.test(), AppDependencies(backend.repositories, FakeObjectStorage()))
        }
        val client = jsonClient()

        val created = client.post("/v1/media/upload-url") {
            bearerAuth(token(userOne))
            contentType(ContentType.Application.Json)
            setBody(MediaUploadRequest("bol.pdf", "application/pdf", 42))
        }
        assertEquals(HttpStatusCode.Created, created.status)
        val upload = created.body<MediaUploadResponse>()

        assertEquals(
            HttpStatusCode.NotFound,
            client.get("/v1/media/${upload.mediaId}") { bearerAuth(token(userTwo)) }.status,
        )
        assertEquals(
            HttpStatusCode.OK,
            client.get("/v1/media/${upload.mediaId}") { bearerAuth(token(userOne)) }.status,
        )
    }

    private fun io.ktor.server.testing.ApplicationTestBuilder.jsonClient() = createClient {
        install(ContentNegotiation) {
            json(ContractJson)
        }
    }

    private fun token(userId: UUID): String = "test.$userId"

    private fun snapshot(userId: UUID, updatedAt: Long) = AccountCloudSnapshot(
        accountId = userId.toString(),
        updatedAt = updatedAt,
        backup = buildJsonObject {
            putJsonArray("loads") { add(buildJsonObject { put("id", "load-1") }) }
            put("paychecks", buildJsonArray { })
            put("diesel", buildJsonArray { })
        },
    )

    private fun telegramMessage(updateId: Long, chatId: Long, text: String): String =
        """
        {
          "update_id": $updateId,
          "message": {
            "message_id": $updateId,
            "chat": {"id": $chatId},
            "from": {"username": "driver"},
            "text": ${ContractJson.encodeToString(text)}
          }
        }
        """.trimIndent()
}
