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
 * Journal sync uses the Supabase JWT from [AuthStore.accessTokenOrNull].
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

/** Chooses the Bearer secret for a Ktor path. */
internal object KtorBearerToken {
    fun select(accessToken: String?): String? =
        accessToken?.trim()?.takeIf { it.isNotBlank() }
}
