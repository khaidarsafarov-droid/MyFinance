package com.truckerload.data.remote.ktor

import com.truckerload.BuildConfig
import com.truckerload.contract.ContractJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide Ktor [HttpClient] for the sync backend.
 * Base URL comes from [BuildConfig.SYNC_BACKEND_URL]; blank URL → client still builds
 * but APIs no-op / throw when invoked without a configured backend.
 *
 * Uses the CIO engine so we do not pull a second OkHttp major onto the classpath
 * (legacy [okhttp3.OkHttpClient] remotes stay on 4.x).
 */
@Singleton
class HttpClientProvider @Inject constructor(
    private val authInterceptor: KtorAuthInterceptor,
) {
    val client: HttpClient by lazy { create() }

    fun baseUrlOrNull(): String? =
        BuildConfig.SYNC_BACKEND_URL.trim().trimEnd('/').takeIf { it.isNotBlank() }

    fun isBackendConfigured(): Boolean = baseUrlOrNull() != null && !BuildConfig.LOCAL_ONLY_MODE

    private fun create(): HttpClient {
        val base = baseUrlOrNull()
        return HttpClient(CIO) {
            expectSuccess = false
            install(ContentNegotiation) {
                json(ContractJson)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 45_000
                connectTimeoutMillis = 20_000
                socketTimeoutMillis = 45_000
            }
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 2)
                exponentialDelay()
                retryIf { _, response -> response.status.value == 429 }
            }
            if (BuildConfig.DEBUG) {
                install(Logging) {
                    level = LogLevel.HEADERS
                }
            }
            authInterceptor.install(this)
            if (base != null) {
                defaultRequest {
                    url.takeFrom("$base/")
                    headers.append("Accept", ContentType.Application.Json.toString())
                }
            }
        }
    }
}
