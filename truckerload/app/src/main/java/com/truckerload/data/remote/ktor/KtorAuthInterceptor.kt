package com.truckerload.data.remote.ktor

import android.content.Context
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.sync.DeviceIdentity
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Attaches a Bearer token and device id on every Ktor request.
 *
 * Journal sync keeps the Supabase JWT from [AuthStore.accessTokenOrNull].
 * `POST /v1/voice/token` prefers the Google ID token because the droplet
 * verifies RS256 Google tokens, not the project's Supabase HS256 secret.
 */
@Singleton
class KtorAuthInterceptor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val authStore: AuthStore,
) {
    fun install(config: HttpClientConfig<*>) {
        val app = context.applicationContext
        val deviceId = DeviceIdentity(app).id()
        config.defaultRequest {
            headers.append("X-Device-Id", deviceId)
        }
        config.install(
            createClientPlugin("KtorAuthBearer") {
                onRequest { request, _ ->
                    val token = KtorBearerToken.select(
                        encodedPath = request.url.pathSegments.joinToString("/"),
                        googleIdToken = authStore.googleIdTokenOrNull(),
                        accessToken = authStore.accessTokenOrNull(),
                    )
                    if (!token.isNullOrBlank()) {
                        request.headers.remove(HttpHeaders.Authorization)
                        request.headers.append(HttpHeaders.Authorization, "Bearer $token")
                    }
                }
            },
        )
    }
}

/** Chooses the Bearer secret for a Ktor path. Voice minting is Google-only. */
internal object KtorBearerToken {
    fun select(
        encodedPath: String,
        googleIdToken: String?,
        accessToken: String?,
    ): String? {
        val google = googleIdToken?.trim()?.takeIf { it.isNotBlank() }
        val access = accessToken?.trim()?.takeIf { it.isNotBlank() }
        if (google != null && encodedPath.contains("v1/voice/token")) return google
        return access
    }
}
