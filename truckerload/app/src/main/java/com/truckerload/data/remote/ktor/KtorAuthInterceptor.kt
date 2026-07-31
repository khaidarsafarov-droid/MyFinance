package com.truckerload.data.remote.ktor

import android.content.Context
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.sync.DeviceIdentity
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Attaches Supabase JWT (`Authorization: Bearer`) and device id on every Ktor request.
 * Token is read from encrypted [AuthStore] (SecurePreferences-backed).
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
            val token = authStore.accessTokenOrNull()?.trim().orEmpty()
            if (token.isNotBlank()) {
                headers.append(HttpHeaders.Authorization, "Bearer $token")
            }
            headers.append("X-Device-Id", deviceId)
        }
    }
}
