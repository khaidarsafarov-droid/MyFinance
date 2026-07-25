package com.truckerload.backend

import java.net.URI

enum class AppEnvironment { DEV, TEST, PROD }
enum class StorageKind { LOCAL, S3 }

data class AppConfig(
    val environment: AppEnvironment,
    val host: String,
    val port: Int,
    val version: String,
    val databaseUrl: String,
    val databaseUser: String?,
    val databasePassword: String?,
    val jwtSecret: String,
    val jwtIssuer: String,
    val jwtAudience: String,
    val testAuthEnabled: Boolean,
    val telegramWebhookSecret: String,
    val telegramLinkTokenTtlSeconds: Long,
    val storageKind: StorageKind,
    val uploadExpirySeconds: Long,
    val downloadExpirySeconds: Long,
    val maxUploadBytes: Long,
    val localStoragePath: String,
    val publicBaseUrl: String,
    val localStorageSigningSecret: String,
    val s3Bucket: String?,
    val s3Region: String,
    val s3Endpoint: String?,
    val s3PublicEndpoint: String?,
    val s3PathStyle: Boolean,
    val firebaseProjectId: String?,
    val firebaseCredentialsJson: String?,
    val metricsBearerToken: String?,
) {
    init {
        require(host.isNotBlank()) { "HOST must not be blank" }
        require(port in 1..65535) { "PORT must be between 1 and 65535" }
        require(databaseUrl.startsWith("jdbc:postgresql:")) { "DATABASE_URL must be a PostgreSQL JDBC URL" }
        require(isHttpUrl(jwtIssuer)) { "SUPABASE_JWT_ISSUER must be an absolute HTTP(S) URL" }
        require(jwtAudience.isNotBlank()) { "SUPABASE_JWT_AUDIENCE is required" }
        require(jwtSecret.isNotBlank()) { "SUPABASE_JWT_SECRET is required" }
        require(telegramWebhookSecret.isNotBlank()) { "TELEGRAM_WEBHOOK_SECRET is required" }
        require(telegramLinkTokenTtlSeconds in 60..86_400) { "TELEGRAM_LINK_TOKEN_TTL_SECONDS is invalid" }
        require(uploadExpirySeconds in 60..86_400) { "UPLOAD_EXPIRY_SECONDS is invalid" }
        require(downloadExpirySeconds in 60..86_400) { "DOWNLOAD_EXPIRY_SECONDS is invalid" }
        require(maxUploadBytes > 0) { "MAX_UPLOAD_BYTES must be positive" }
        require(isHttpUrl(publicBaseUrl)) {
            "PUBLIC_BASE_URL must be an absolute HTTP(S) URL"
        }
        require(!testAuthEnabled || environment == AppEnvironment.TEST) {
            "TEST_AUTH_ENABLED may only be enabled when APP_ENV=test"
        }
        if (environment == AppEnvironment.PROD) {
            require(jwtSecret.length >= 32) { "SUPABASE_JWT_SECRET must be at least 32 characters in production" }
            require(telegramWebhookSecret.length >= 16) {
                "TELEGRAM_WEBHOOK_SECRET must be at least 16 characters in production"
            }
            require(metricsBearerToken == null || metricsBearerToken.length >= 32) {
                "METRICS_BEARER_TOKEN must be at least 32 characters in production"
            }
        }
        if (storageKind == StorageKind.LOCAL) {
            require(localStorageSigningSecret.isNotBlank()) { "LOCAL_STORAGE_SIGNING_SECRET is required" }
            if (environment == AppEnvironment.PROD) {
                require(localStorageSigningSecret.length >= 32) {
                    "LOCAL_STORAGE_SIGNING_SECRET must be at least 32 characters in production"
                }
            }
        } else {
            require(!s3Bucket.isNullOrBlank()) { "S3_BUCKET is required for S3 storage" }
            require(s3Endpoint == null || isHttpUrl(s3Endpoint)) { "S3_ENDPOINT must be an absolute HTTP(S) URL" }
            require(s3PublicEndpoint == null || isHttpUrl(s3PublicEndpoint)) {
                "S3_PUBLIC_ENDPOINT must be an absolute HTTP(S) URL"
            }
        }
    }

    companion object {
        fun fromEnvironment(env: Map<String, String> = System.getenv()): AppConfig {
            fun required(name: String): String =
                env[name]?.takeIf { it.isNotBlank() } ?: error("$name is required")

            fun long(name: String, default: Long): Long =
                env[name]?.let { value -> value.toLongOrNull() ?: error("$name must be an integer") } ?: default

            fun boolean(name: String, default: Boolean = false): Boolean =
                env[name]?.let { value ->
                    when (value.lowercase()) {
                        "true" -> true
                        "false" -> false
                        else -> error("$name must be true or false")
                    }
                } ?: default

            val environment = when (env["APP_ENV"]?.lowercase() ?: "dev") {
                "dev", "development" -> AppEnvironment.DEV
                "test" -> AppEnvironment.TEST
                "prod", "production" -> AppEnvironment.PROD
                else -> error("APP_ENV must be dev, test, or prod")
            }
            val issuer = env["SUPABASE_JWT_ISSUER"]
                ?: env["SUPABASE_URL"]?.trimEnd('/')?.plus("/auth/v1")
                ?: error("SUPABASE_JWT_ISSUER or SUPABASE_URL is required")
            val storageKind = when (env["STORAGE_KIND"]?.lowercase() ?: "local") {
                "local" -> StorageKind.LOCAL
                "s3" -> StorageKind.S3
                else -> error("STORAGE_KIND must be local or s3")
            }
            val localSigningSecret = env["LOCAL_STORAGE_SIGNING_SECRET"]
                ?: if (storageKind == StorageKind.LOCAL) {
                    error("LOCAL_STORAGE_SIGNING_SECRET is required")
                } else {
                    ""
                }

            return AppConfig(
                environment = environment,
                host = env["HOST"] ?: "0.0.0.0",
                port = long("PORT", 8080).toInt(),
                version = env["APP_VERSION"] ?: "dev",
                databaseUrl = required("DATABASE_URL"),
                databaseUser = env["DATABASE_USER"],
                databasePassword = env["DATABASE_PASSWORD"],
                jwtSecret = required("SUPABASE_JWT_SECRET"),
                jwtIssuer = issuer,
                jwtAudience = env["SUPABASE_JWT_AUDIENCE"] ?: "authenticated",
                testAuthEnabled = boolean("TEST_AUTH_ENABLED"),
                telegramWebhookSecret = required("TELEGRAM_WEBHOOK_SECRET"),
                telegramLinkTokenTtlSeconds = long("TELEGRAM_LINK_TOKEN_TTL_SECONDS", 600),
                storageKind = storageKind,
                uploadExpirySeconds = long("UPLOAD_EXPIRY_SECONDS", 900),
                downloadExpirySeconds = long("DOWNLOAD_EXPIRY_SECONDS", 900),
                maxUploadBytes = long("MAX_UPLOAD_BYTES", 25L * 1024 * 1024),
                localStoragePath = env["LOCAL_STORAGE_PATH"] ?: "./data/media",
                publicBaseUrl = env["PUBLIC_BASE_URL"] ?: "http://localhost:8080",
                localStorageSigningSecret = localSigningSecret,
                s3Bucket = env["S3_BUCKET"],
                s3Region = env["S3_REGION"] ?: "us-east-1",
                s3Endpoint = env["S3_ENDPOINT"],
                s3PublicEndpoint = env["S3_PUBLIC_ENDPOINT"],
                s3PathStyle = boolean("S3_PATH_STYLE"),
                firebaseProjectId = env["FIREBASE_PROJECT_ID"]?.takeIf { it.isNotBlank() },
                firebaseCredentialsJson = env["FIREBASE_CREDENTIALS_JSON"]?.takeIf { it.isNotBlank() },
                metricsBearerToken = env["METRICS_BEARER_TOKEN"]?.takeIf { it.isNotBlank() },
            )
        }

        fun test(): AppConfig = AppConfig(
            environment = AppEnvironment.TEST,
            host = "127.0.0.1",
            port = 8080,
            version = "test",
            databaseUrl = "jdbc:postgresql://unused/test",
            databaseUser = null,
            databasePassword = null,
            jwtSecret = "test-jwt-secret-not-for-production",
            jwtIssuer = "https://test.supabase.co/auth/v1",
            jwtAudience = "authenticated",
            testAuthEnabled = true,
            telegramWebhookSecret = "telegram-test-secret",
            telegramLinkTokenTtlSeconds = 600,
            storageKind = StorageKind.LOCAL,
            uploadExpirySeconds = 900,
            downloadExpirySeconds = 900,
            maxUploadBytes = 25L * 1024 * 1024,
            localStoragePath = "./build/test-media",
            publicBaseUrl = "http://localhost",
            localStorageSigningSecret = "local-test-signing-secret",
            s3Bucket = null,
            s3Region = "us-east-1",
            s3Endpoint = null,
            s3PublicEndpoint = null,
            s3PathStyle = false,
            firebaseProjectId = null,
            firebaseCredentialsJson = null,
            metricsBearerToken = null,
        )

        private fun isHttpUrl(value: String): Boolean {
            val uri = runCatching { URI(value) }.getOrNull() ?: return false
            return uri.isAbsolute && uri.host != null && uri.scheme.lowercase() in setOf("http", "https")
        }
    }
}
