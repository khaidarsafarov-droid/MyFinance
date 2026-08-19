package com.truckerload.data.voice

import com.truckerload.contract.VoiceTokenRequest
import com.truckerload.contract.VoiceTokenResponse
import com.truckerload.data.remote.ktor.HttpClientProvider
import com.truckerload.domain.voice.VoiceRoomRole
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class VoiceTokenClient(
    private val http: HttpClientProvider,
) {
    fun isAvailable(): Boolean = http.isBackendConfigured()

    suspend fun fetch(
        roomId: String,
        displayName: String,
        role: VoiceRoomRole,
    ): VoiceTokenResponse? {
        if (!isAvailable()) return null
        val response = http.client.post("v1/voice/token") {
            contentType(ContentType.Application.Json)
            setBody(
                VoiceTokenRequest(
                    roomId = roomId,
                    displayName = displayName,
                    role = role.apiValue(),
                ),
            )
        }
        if (response.status.value == 503) return null
        if (!response.status.isSuccess()) {
            val detail = runCatching { response.bodyAsText() }.getOrDefault("")
            error("voice_token_failed HTTP ${response.status.value} $detail")
        }
        return response.body()
    }
}
