package com.truckerload.backend

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import java.net.URI
import java.security.MessageDigest
import java.security.interfaces.RSAPublicKey
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Accepts Supabase HS256 JWTs and Google Sign-In ID tokens (RS256).
 * Google `sub` maps to the same `google_<hex>` identity the Android client uses.
 */
object TokenAuth {
    private val googleJwks: JwkProvider = JwkProviderBuilder(
        URI("https://www.googleapis.com/oauth2/v3/certs").toURL(),
    ).cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    fun principal(token: String, config: AppConfig, supabaseVerifier: JWTVerifier): AppPrincipal? {
        if (config.environment == AppEnvironment.TEST && config.testAuthEnabled && token.startsWith("test.")) {
            val id = runCatching { UUID.fromString(token.removePrefix("test.")) }.getOrNull() ?: return null
            return AppPrincipal(AuthenticatedUser(id, null))
        }
        runCatching {
            val jwt = supabaseVerifier.verify(token)
            val id = UUID.fromString(jwt.subject)
            val email = jwt.getClaim("email").asString()
            return AppPrincipal(AuthenticatedUser(id, email))
        }
        return googlePrincipal(token, config)
    }

    internal fun googleAccountId(googleSub: String): String {
        val sub = googleSub.trim()
        require(sub.isNotBlank()) { "google sub required" }
        val digest = MessageDigest.getInstance("SHA-256").digest(sub.toByteArray(Charsets.UTF_8))
        val hex = digest.take(16).joinToString("") { b -> "%02x".format(b) }
        return "google_$hex"
    }

    private fun googlePrincipal(token: String, config: AppConfig): AppPrincipal? {
        val audience = config.googleWebClientId.trim()
        if (audience.isBlank()) return null
        return runCatching {
            val decoded = JWT.decode(token)
            if (!decoded.algorithm.equals("RS256", ignoreCase = true)) return@runCatching null
            val keyId = decoded.keyId ?: return@runCatching null
            val publicKey = googleJwks.get(keyId).publicKey as RSAPublicKey
            val jwt = JWT.require(Algorithm.RSA256(publicKey, null))
                .withAudience(audience)
                .acceptLeeway(60)
                .build()
                .verify(token)
            val issuer = jwt.issuer.orEmpty()
            if (issuer != "https://accounts.google.com" && issuer != "accounts.google.com") {
                return@runCatching null
            }
            val sub = jwt.subject?.trim().orEmpty()
            if (sub.isBlank()) return@runCatching null
            val accountId = googleAccountId(sub)
            val userId = UUID.nameUUIDFromBytes(accountId.toByteArray(Charsets.UTF_8))
            val email = jwt.getClaim("email").asString()
            AppPrincipal(AuthenticatedUser(userId, email, voiceIdentity = accountId))
        }.getOrNull()
    }
}
