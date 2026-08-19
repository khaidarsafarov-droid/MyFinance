package com.truckerload.backend

import com.truckerload.contract.VoiceTokenRequest
import com.truckerload.contract.VoiceTokenResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

private const val SPEAKER_BITRATE = 16_000
private const val LISTENER_BITRATE = 0
private val ROOM_ID = Regex("""^[A-Za-z0-9._-]{1,128}$""")

fun Route.voiceRoutes(config: AppConfig, repositories: Repositories) {
    route("/voice") {
        post("/token") {
            val user = call.requireUser(repositories)
            if (!config.liveKitEnabled) {
                throw ApiException(
                    HttpStatusCode.ServiceUnavailable,
                    "voice_sfu_disabled",
                    "LiveKit is not configured on this server",
                )
            }
            val request = call.receive<VoiceTokenRequest>()
            val roomId = request.roomId.trim()
            if (!ROOM_ID.matches(roomId)) {
                throw ApiException(HttpStatusCode.BadRequest, "invalid_room_id", "roomId is invalid")
            }
            val role = request.role.trim().lowercase()
            if (role !in setOf("speaker", "listener")) {
                throw ApiException(HttpStatusCode.BadRequest, "invalid_role", "role must be speaker or listener")
            }
            val canPublish = role == "speaker"
            val identity = user.id.toString()
            val displayName = request.displayName.trim().ifBlank { identity.take(8) }
            val roomName = LiveKitAccessToken.roomName(roomId)
            val token = LiveKitAccessToken(config.liveKitApiKey, config.liveKitApiSecret).mint(
                identity = identity,
                name = displayName,
                roomName = roomName,
                canPublish = canPublish,
            )
            call.respond(
                VoiceTokenResponse(
                    url = config.liveKitUrl,
                    token = token,
                    roomName = roomName,
                    identity = identity,
                    role = role,
                    audioBitrate = if (canPublish) SPEAKER_BITRATE else LISTENER_BITRATE,
                ),
            )
        }
    }
}

private suspend fun ApplicationCall.requireUser(repositories: Repositories): AuthenticatedUser {
    val user = principal<AppPrincipal>()?.user
        ?: throw ApiException(HttpStatusCode.Unauthorized, "unauthorized", "Authentication required")
    repositories.users.upsert(user)
    return user
}
