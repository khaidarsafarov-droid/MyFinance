package com.truckerload.backend

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.truckerload.contract.AccountCloudSnapshot
import com.truckerload.contract.ApiError
import com.truckerload.contract.ContractJson
import com.truckerload.contract.DevicePushTokenRequest
import com.truckerload.contract.HealthResponse
import com.truckerload.contract.MediaKind
import com.truckerload.contract.MediaListResponse
import com.truckerload.contract.MediaMetadata
import com.truckerload.contract.MediaUploadCompleteRequest
import com.truckerload.contract.MediaUploadRequest
import com.truckerload.contract.MediaUploadResponse
import com.truckerload.contract.SyncCursor
import com.truckerload.contract.TelegramInboxListResponse
import com.truckerload.contract.TelegramLinkTokenResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.call
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.bearer
import io.ktor.server.auth.principal
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callId
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.contentType
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.serialization.kotlinx.json.json
import java.util.UUID
import kotlinx.io.readByteArray
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.put
import io.ktor.utils.io.readRemaining
import io.ktor.util.AttributeKey
import org.slf4j.event.Level

fun main(args: Array<String>) = EngineMain.main(args)

data class AppPrincipal(val user: AuthenticatedUser)

class ApiException(
    val status: HttpStatusCode,
    val code: String,
    override val message: String,
) : RuntimeException(message)

data class AppDependencies(
    val repositories: Repositories,
    val objectStorage: ObjectStorage,
    val pushNotifier: PushNotifier = NoOpPushNotifier,
    val metrics: BackendMetrics = BackendMetrics(),
    val close: () -> Unit = {
        objectStorage.close()
        metrics.registry.close()
    },
)

fun Application.module() {
    val config = AppConfig.fromEnvironment()
    val dataSource = createDataSource(config)
    migrateDatabase(dataSource)
    val storage: ObjectStorage = when (config.storageKind) {
        StorageKind.LOCAL -> LocalObjectStorage(
            config.localStoragePath,
            config.publicBaseUrl,
            config.localStorageSigningSecret,
        )
        StorageKind.S3 -> S3ObjectStorage(
            bucket = requireNotNull(config.s3Bucket),
            regionName = config.s3Region,
            endpoint = config.s3Endpoint,
            publicEndpoint = config.s3PublicEndpoint,
            pathStyle = config.s3PathStyle,
        )
    }
    val metrics = BackendMetrics()
    val notifier = FirebasePushNotifier.createOrNoOp(
        config.firebaseProjectId,
        config.firebaseCredentialsJson,
    )
    configureApplication(
        config,
        AppDependencies(jdbcRepositories(dataSource), storage, notifier, metrics) {
            notifier.close()
            storage.close()
            dataSource.close()
            metrics.registry.close()
        },
    )
}

fun Application.configureApplication(config: AppConfig, dependencies: AppDependencies) {
    val appLog = environment.log
    val tokenVerifier = JWT.require(Algorithm.HMAC256(config.jwtSecret))
        .withIssuer(config.jwtIssuer)
        .withAudience(config.jwtAudience)
        .build()

    install(ContentNegotiation) {
        json(ContractJson)
    }
    install(CallId) {
        retrieveFromHeader(HttpHeaders.XRequestId)
        generate { UUID.randomUUID().toString() }
        verify { it.length in 1..128 && it.all { character -> character.isLetterOrDigit() || character in "-_." } }
        replyToHeader(HttpHeaders.XRequestId)
    }
    install(CallLogging) {
        level = if (config.environment == AppEnvironment.PROD) Level.INFO else Level.DEBUG
        mdc("requestId") { it.callId }
        format { call ->
            "HTTP ${call.request.httpMethod.value} ${call.request.path()} " +
                "status=${call.response.status()?.value ?: 0}"
        }
    }
    val requestStartNanos = AttributeKey<Long>("BackendRequestStartNanos")
    install(createApplicationPlugin("BackendHttpMetrics") {
        onCall { call ->
            call.attributes.put(requestStartNanos, System.nanoTime())
        }
        onCallRespond { call, _ ->
            val startedAt = call.attributes.getOrNull(requestStartNanos) ?: return@onCallRespond
            dependencies.metrics.recordHttp(
                method = call.request.httpMethod.value,
                status = call.response.status()?.value ?: HttpStatusCode.OK.value,
                durationNanos = System.nanoTime() - startedAt,
            )
        }
    })
    dependencies.metrics.initialize()
    install(StatusPages) {
        exception<ApiException> { call, error ->
            call.respond(
                error.status,
                ApiError(error.code, error.message, call.callId),
            )
        }
        exception<SerializationException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError("invalid_json", "Request JSON is invalid", call.callId),
            )
        }
        exception<BadRequestException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError("bad_request", "Request is invalid", call.callId),
            )
        }
        exception<Throwable> { call, error ->
            appLog.error("Unhandled request error requestId={}", call.callId, error)
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiError("internal_error", "Internal server error", call.callId),
            )
        }
    }
    install(Authentication) {
        bearer("supabase") {
            realm = "truckerload-api"
            authenticate { credential ->
                authenticateToken(credential.token, config, tokenVerifier)
            }
        }
    }

    routing {
        get("/health/live") {
            call.respond(
                HealthResponse("ok", timestamp = System.currentTimeMillis(), version = config.version),
            )
        }
        get("/health/ready") {
            val databaseReady = dependencies.repositories.health.isReady()
            val storageReady = dependencies.objectStorage.isReady()
            val ready = databaseReady && storageReady
            call.respond(
                if (ready) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable,
                HealthResponse(
                    status = if (ready) "ok" else "unavailable",
                    timestamp = System.currentTimeMillis(),
                    version = config.version,
                ),
            )
        }
        get("/openapi.yaml") {
            val bytes = checkNotNull(javaClass.classLoader.getResourceAsStream("openapi.yaml")) {
                "openapi.yaml is missing"
            }.use { it.readBytes() }
            call.respondBytes(bytes, ContentType.parse("application/yaml"))
        }
        get("/docs") {
            call.respondRedirect("/openapi.yaml")
        }
        get("/metrics") {
            call.authorizeMetrics(config)
            call.respondText(
                dependencies.metrics.scrape(),
                ContentType.parse("text/plain; version=0.0.4; charset=utf-8"),
            )
        }

        telegramWebhook(config, dependencies.repositories, dependencies.metrics)
        localMediaUpload(config, dependencies)
        localMediaDownload(config, dependencies)

        authenticate("supabase") {
            route("/v1") {
                syncRoutes(dependencies.repositories, dependencies.pushNotifier, dependencies.metrics)
                mediaRoutes(config, dependencies)
                telegramAuthenticatedRoutes(config, dependencies.repositories)
                deviceRoutes(dependencies.repositories)
            }
        }
    }

    monitor.subscribe(ApplicationStopped) {
        dependencies.close()
    }
}

private fun authenticateToken(
    token: String,
    config: AppConfig,
    verifier: com.auth0.jwt.interfaces.JWTVerifier,
): AppPrincipal? {
    if (config.environment == AppEnvironment.TEST && config.testAuthEnabled && token.startsWith("test.")) {
        val id = runCatching { UUID.fromString(token.removePrefix("test.")) }.getOrNull() ?: return null
        return AppPrincipal(AuthenticatedUser(id, null))
    }
    return runCatching {
        val jwt = verifier.verify(token)
        val id = UUID.fromString(jwt.subject)
        val email = jwt.getClaim("email").asString()
        AppPrincipal(AuthenticatedUser(id, email))
    }.getOrNull()
}

private suspend fun ApplicationCall.authenticatedUser(repositories: Repositories): AuthenticatedUser {
    val user = principal<AppPrincipal>()?.user
        ?: throw ApiException(HttpStatusCode.Unauthorized, "unauthorized", "Authentication required")
    repositories.users.upsert(user)
    return user
}

private fun io.ktor.server.routing.Route.syncRoutes(
    repositories: Repositories,
    pushNotifier: PushNotifier,
    metrics: BackendMetrics,
) {
    route("/sync") {
        get("/snapshot") {
            val user = call.authenticatedUser(repositories)
            val since = call.request.queryParameters["since"]?.let(::nonNegativeLong)
            val snapshot = repositories.snapshots.get(user.id)
            if (snapshot == null || (since != null && snapshot.updatedAt <= since)) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(snapshot.withResolvedEntityCount())
            }
        }
        put("/snapshot") {
            val user = call.authenticatedUser(repositories)
            val sourceDeviceId = call.request.headers["X-Device-Id"]?.let(::validDeviceId)
            val incoming = call.receiveJson<AccountCloudSnapshot>(MAX_SNAPSHOT_BODY_BYTES)
            if (incoming.accountId != user.id.toString()) {
                throw ApiException(
                    HttpStatusCode.Forbidden,
                    "account_mismatch",
                    "Snapshot accountId must match the authenticated user",
                )
            }
            if (incoming.version < 1 || incoming.updatedAt < 0) {
                throw ApiException(HttpStatusCode.BadRequest, "invalid_snapshot", "Snapshot metadata is invalid")
            }
            val normalized = incoming.copy(accountId = user.id.toString()).withResolvedEntityCount()
            val checksum = sha256Hex(ContractJson.encodeToString(normalized))
            val stored = repositories.snapshots.putLww(user.id, normalized, checksum)
            metrics.recordSnapshot(stored.accepted)
            if (stored.accepted && sourceDeviceId != null) {
                val tokens = repositories.pushTokens.listForUser(user.id, sourceDeviceId).map { it.token }
                metrics.recordPushFailures(pushNotifier.notifySync(tokens))
            }
            call.respond(stored.snapshot)
        }
        get("/cursor") {
            val user = call.authenticatedUser(repositories)
            val deviceId = validDeviceId(call.request.queryParameters["deviceId"])
            val cursor = repositories.cursors.get(user.id, deviceId)
            if (cursor == null) call.respond(HttpStatusCode.NoContent) else call.respond(cursor)
        }
        put("/cursor") {
            val user = call.authenticatedUser(repositories)
            val cursor = call.receiveJson<SyncCursor>(MAX_SMALL_JSON_BODY_BYTES)
            validDeviceId(cursor.deviceId)
            if (cursor.cursor < 0) {
                throw ApiException(HttpStatusCode.BadRequest, "invalid_cursor", "cursor must be non-negative")
            }
            call.respond(repositories.cursors.put(user.id, cursor))
        }
    }
}

private fun io.ktor.server.routing.Route.deviceRoutes(repositories: Repositories) {
    route("/devices") {
        put("/push-token") {
            val user = call.authenticatedUser(repositories)
            val request = call.receiveJson<DevicePushTokenRequest>(MAX_SMALL_JSON_BODY_BYTES)
            val deviceId = validDeviceId(request.deviceId)
            val token = validPushToken(request.token)
            if (request.platform != "android") {
                throw ApiException(HttpStatusCode.BadRequest, "invalid_platform", "platform must be android")
            }
            repositories.pushTokens.upsert(
                DevicePushTokenRecord(
                    userId = user.id,
                    deviceId = deviceId,
                    token = token,
                    platform = request.platform,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            call.respond(HttpStatusCode.NoContent)
        }
        delete("/push-token") {
            val user = call.authenticatedUser(repositories)
            val deviceId = validDeviceId(call.request.queryParameters["deviceId"])
            repositories.pushTokens.delete(user.id, deviceId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun io.ktor.server.routing.Route.mediaRoutes(config: AppConfig, dependencies: AppDependencies) {
    route("/media") {
        get {
            val user = call.authenticatedUser(dependencies.repositories)
            val since = call.request.queryParameters["since"]?.let(::nonNegativeLong) ?: 0L
            val kind = call.request.queryParameters["kind"]?.let(::validMediaKind)
            val records = dependencies.repositories.media.list(user.id, since, kind, MAX_MEDIA_LIST_ITEMS)
            val items = records.map { record -> record.toDownloadContract(config, dependencies.objectStorage) }
            call.respond(
                MediaListResponse(
                    items = items,
                    nextSince = records.maxOfOrNull { it.revision } ?: since,
                ),
            )
        }
        post("/upload-url") {
            val user = call.authenticatedUser(dependencies.repositories)
            val request = call.receiveJson<MediaUploadRequest>(MAX_SMALL_JSON_BODY_BYTES)
            val kind = request.kind
                ?: throw ApiException(HttpStatusCode.BadRequest, "invalid_kind", "kind is required")
            val clientId = validMediaClientId(request.clientId)
            val loadId = validOptionalMediaLoadId(request.loadId)
            if (request.sizeBytes !in 1..config.maxUploadBytes) {
                throw ApiException(HttpStatusCode.BadRequest, "invalid_size", "Upload size is outside the allowed range")
            }
            if (request.contentType.length !in 3..255 || runCatching {
                    ContentType.parse(request.contentType)
                }.isFailure
            ) {
                throw ApiException(HttpStatusCode.BadRequest, "invalid_content_type", "contentType is invalid")
            }
            validateMediaContentType(kind, request.contentType)
            validateChecksum(request.checksum)
            validateMediaMetadata(request.metadata)
            val fileName = safeFileName(request.fileName)
            val mediaId = UUID.randomUUID()
            val objectKey = "${user.id}/$mediaId/$fileName"
            val now = System.currentTimeMillis()
            val expiresAt = now + config.uploadExpirySeconds * 1000
            val createResult = dependencies.repositories.media.createOrGet(
                MediaRecord(
                    id = mediaId,
                    userId = user.id,
                    objectKey = objectKey,
                    fileName = fileName,
                    contentType = request.contentType,
                    sizeBytes = request.sizeBytes,
                    checksum = request.checksum,
                    kind = kind,
                    clientId = clientId,
                    loadId = loadId,
                    metadata = request.metadata,
                    status = "pending",
                    createdAt = now,
                    completedAt = null,
                    updatedAt = now,
                    deletedAt = null,
                ),
            )
            val record = createResult.record
            if (record.deletedAt != null) {
                throw ApiException(HttpStatusCode.Conflict, "media_deleted", "This client media id was deleted")
            }
            if (
                record.fileName != fileName ||
                record.contentType != request.contentType ||
                record.sizeBytes != request.sizeBytes ||
                (record.checksum != null && request.checksum != null && record.checksum != request.checksum)
            ) {
                throw ApiException(
                    HttpStatusCode.Conflict,
                    "media_id_conflict",
                    "This client media id already refers to different content",
                )
            }
            if (record.status == "ready") {
                val complete = record.toDownloadContract(config, dependencies.objectStorage)
                call.respond(
                    HttpStatusCode.OK,
                    MediaUploadResponse(
                        mediaId = record.id.toString(),
                        expiresAt = complete.expiresAt ?: now,
                        alreadyComplete = true,
                        media = complete,
                    ),
                )
                return@post
            }
            val upload = dependencies.objectStorage.presignUpload(
                record.id,
                record.objectKey,
                record.contentType,
                record.sizeBytes,
                expiresAt,
            )
            call.respond(
                if (createResult.created) HttpStatusCode.Created else HttpStatusCode.OK,
                MediaUploadResponse(record.id.toString(), upload.url, headers = upload.headers, expiresAt = expiresAt),
            )
        }
        post("/complete") {
            val user = call.authenticatedUser(dependencies.repositories)
            val request = call.receiveJson<MediaUploadCompleteRequest>(MAX_SMALL_JSON_BODY_BYTES)
            val mediaId = uuid(request.mediaId, "mediaId")
            val record = dependencies.repositories.media.get(user.id, mediaId)
                ?: throw ApiException(HttpStatusCode.NotFound, "media_not_found", "Media object was not found")
            validateChecksum(request.checksum)
            if (record.status == "ready") {
                call.respond(record.toDownloadContract(config, dependencies.objectStorage))
                return@post
            }
            if (record.checksum != null && request.checksum != null && record.checksum != request.checksum) {
                throw ApiException(HttpStatusCode.Conflict, "checksum_mismatch", "Upload checksum does not match")
            }
            val stored = dependencies.objectStorage.stat(record.objectKey)
                ?: throw ApiException(HttpStatusCode.Conflict, "upload_incomplete", "Uploaded object was not found")
            if (stored.sizeBytes != record.sizeBytes) {
                throw ApiException(HttpStatusCode.Conflict, "size_mismatch", "Uploaded object size does not match")
            }
            val completed = dependencies.repositories.media.markComplete(
                user.id,
                mediaId,
                request.checksum ?: record.checksum,
                System.currentTimeMillis(),
            ) ?: throw ApiException(HttpStatusCode.NotFound, "media_not_found", "Media object was not found")
            call.respond(completed.toDownloadContract(config, dependencies.objectStorage))
        }
        get("/{mediaId}") {
            val user = call.authenticatedUser(dependencies.repositories)
            val mediaId = uuid(call.parameters["mediaId"], "mediaId")
            val record = dependencies.repositories.media.get(user.id, mediaId)
                ?: throw ApiException(HttpStatusCode.NotFound, "media_not_found", "Media object was not found")
            call.respond(record.toDownloadContract(config, dependencies.objectStorage))
        }
        delete("/{mediaId}") {
            val user = call.authenticatedUser(dependencies.repositories)
            val mediaId = uuid(call.parameters["mediaId"], "mediaId")
            val existing = dependencies.repositories.media.get(user.id, mediaId)
            if (existing == null) {
                call.respond(HttpStatusCode.NoContent)
                return@delete
            }
            dependencies.objectStorage.delete(existing.objectKey)
            dependencies.repositories.media.softDelete(user.id, mediaId, System.currentTimeMillis())
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private suspend fun MediaRecord.toDownloadContract(
    config: AppConfig,
    storage: ObjectStorage,
): MediaMetadata {
    if (status != "ready" || deletedAt != null) return toContract()
    val expiresAt = System.currentTimeMillis() + config.downloadExpirySeconds * 1000
    return toContract(storage.presignDownload(id, objectKey, expiresAt))
}

private fun io.ktor.server.routing.Route.localMediaUpload(config: AppConfig, dependencies: AppDependencies) {
    put("/v1/media/local-upload/{mediaId}") {
        val receiver = dependencies.objectStorage as? LocalUploadReceiver
            ?: throw ApiException(HttpStatusCode.NotFound, "not_found", "Route not found")
        val mediaId = uuid(call.parameters["mediaId"], "mediaId")
        val record = dependencies.repositories.media.getById(mediaId)
            ?: throw ApiException(HttpStatusCode.NotFound, "media_not_found", "Media object was not found")
        val expiresAt = call.request.queryParameters["expiresAt"]?.let(::nonNegativeLong)
            ?: throw ApiException(HttpStatusCode.BadRequest, "invalid_expiry", "expiresAt is required")
        val token = call.request.queryParameters["token"].orEmpty()
        if (call.request.headers[HttpHeaders.ContentType] != record.contentType) {
            throw ApiException(HttpStatusCode.BadRequest, "content_type_mismatch", "Upload content type does not match")
        }
        val declaredLength = call.request.headers[HttpHeaders.ContentLength]?.toLongOrNull()
        if (declaredLength != null && declaredLength != record.sizeBytes) {
            throw ApiException(HttpStatusCode.BadRequest, "size_mismatch", "Upload size does not match")
        }
        val bytes = call.receiveChannel().readRemaining(config.maxUploadBytes + 1).readByteArray()
        if (bytes.size.toLong() != record.sizeBytes || bytes.size.toLong() > config.maxUploadBytes) {
            throw ApiException(HttpStatusCode.BadRequest, "size_mismatch", "Upload size does not match")
        }
        if (!receiver.receive(mediaId, record.objectKey, expiresAt, token, bytes)) {
            throw ApiException(HttpStatusCode.Unauthorized, "invalid_upload_token", "Upload URL is invalid or expired")
        }
        call.respond(HttpStatusCode.NoContent)
    }
}

private fun io.ktor.server.routing.Route.localMediaDownload(config: AppConfig, dependencies: AppDependencies) {
    get("/v1/media/local-download/{mediaId}") {
        val receiver = dependencies.objectStorage as? LocalDownloadReceiver
            ?: throw ApiException(HttpStatusCode.NotFound, "not_found", "Route not found")
        val mediaId = uuid(call.parameters["mediaId"], "mediaId")
        val record = dependencies.repositories.media.getById(mediaId)
            ?.takeIf { it.status == "ready" }
            ?: throw ApiException(HttpStatusCode.NotFound, "media_not_found", "Media object was not found")
        val expiresAt = call.request.queryParameters["expiresAt"]?.let(::nonNegativeLong)
            ?: throw ApiException(HttpStatusCode.BadRequest, "invalid_expiry", "expiresAt is required")
        val token = call.request.queryParameters["token"].orEmpty()
        val download = receiver.openDownload(mediaId, record.objectKey, expiresAt, token)
            ?: throw ApiException(
                HttpStatusCode.Unauthorized,
                "invalid_download_token",
                "Download URL is invalid or expired",
            )
        if (download.sizeBytes != record.sizeBytes || download.sizeBytes > config.maxUploadBytes) {
            throw ApiException(HttpStatusCode.Conflict, "size_mismatch", "Stored object size does not match")
        }
        call.respondOutputStream(ContentType.parse(record.contentType), HttpStatusCode.OK) {
            download.copyTo(this, config.maxUploadBytes)
        }
    }
}

private fun io.ktor.server.routing.Route.telegramAuthenticatedRoutes(
    config: AppConfig,
    repositories: Repositories,
) {
    route("/telegram") {
        post("/link-token") {
            val user = call.authenticatedUser(repositories)
            val token = randomUrlToken()
            val expiresAt = System.currentTimeMillis() + config.telegramLinkTokenTtlSeconds * 1000
            repositories.telegram.createLinkToken(user.id, sha256(token), expiresAt)
            call.respond(HttpStatusCode.Created, TelegramLinkTokenResponse(token, expiresAt))
        }
        get("/inbox") {
            val user = call.authenticatedUser(repositories)
            val since = call.request.queryParameters["sinceUpdateId"]?.let(::nonNegativeLong) ?: 0L
            val items = repositories.telegram.listInbox(user.id, since, 200).map { it.toContract() }
            call.respond(TelegramInboxListResponse(items))
        }
        post("/inbox/{updateId}/ack") {
            val user = call.authenticatedUser(repositories)
            val updateId = nonNegativeLong(call.parameters["updateId"])
            if (!repositories.telegram.acknowledge(user.id, updateId, System.currentTimeMillis())) {
                throw ApiException(HttpStatusCode.NotFound, "inbox_item_not_found", "Inbox item was not found")
            }
            call.respond(HttpStatusCode.NoContent)
        }
        delete("/link") {
            val user = call.authenticatedUser(repositories)
            repositories.telegram.unlink(user.id)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun io.ktor.server.routing.Route.telegramWebhook(
    config: AppConfig,
    repositories: Repositories,
    metrics: BackendMetrics,
) {
    post("/v1/telegram/webhook") {
        val supplied = call.request.headers["X-Telegram-Bot-Api-Secret-Token"]
        if (!constantTimeEquals(config.telegramWebhookSecret, supplied)) {
            metrics.recordTelegramRejected()
            throw ApiException(HttpStatusCode.Unauthorized, "invalid_webhook_secret", "Webhook secret is invalid")
        }
        val update = try {
            call.receiveJson<TelegramUpdate>(MAX_TELEGRAM_BODY_BYTES)
        } catch (error: Exception) {
            metrics.recordTelegramRejected()
            throw error
        }
        val message = update.message
        if (message != null) {
            val text = message.text.orEmpty()
            val startToken = START_COMMAND.matchEntire(text.trim())?.groupValues?.getOrNull(1)
            if (!startToken.isNullOrBlank()) {
                repositories.telegram.consumeLinkTokenAndLink(
                    sha256(startToken),
                    message.chat.id,
                    message.from?.username,
                    System.currentTimeMillis(),
                )
            } else if (text.isNotBlank()) {
                repositories.telegram.linkedUser(message.chat.id)?.let { userId ->
                    val inserted = repositories.telegram.insertInbox(
                        TelegramInboxRecord(
                            updateId = update.updateId,
                            userId = userId,
                            messageId = message.messageId,
                            chatId = message.chat.id,
                            text = text,
                            senderUsername = message.from?.username,
                            receivedAt = System.currentTimeMillis(),
                            acknowledgedAt = null,
                        ),
                    )
                    if (!inserted) metrics.recordTelegramDuplicate()
                }
            }
        }
        metrics.recordTelegramAccepted()
        call.respond(buildJsonObject { put("ok", true) })
    }
}

private fun ApplicationCall.authorizeMetrics(config: AppConfig) {
    if (config.environment != AppEnvironment.PROD) return
    val expected = config.metricsBearerToken
        ?: throw ApiException(HttpStatusCode.NotFound, "not_found", "Route not found")
    val authorization = request.headers[HttpHeaders.Authorization]
    val parts = authorization?.split(' ', limit = 2)
    val supplied = parts
        ?.takeIf { it.size == 2 && it[0].equals("Bearer", ignoreCase = true) }
        ?.get(1)
    if (!constantTimeEquals(expected, supplied)) {
        throw ApiException(HttpStatusCode.Unauthorized, "unauthorized", "Metrics authentication required")
    }
}

@Serializable
private data class TelegramUpdate(
    @kotlinx.serialization.SerialName("update_id") val updateId: Long,
    val message: TelegramMessage? = null,
)

@Serializable
private data class TelegramMessage(
    @kotlinx.serialization.SerialName("message_id") val messageId: Long,
    val chat: TelegramChat,
    val from: TelegramSender? = null,
    val text: String? = null,
)

@Serializable
private data class TelegramChat(val id: Long)

@Serializable
private data class TelegramSender(val username: String? = null)

private val START_COMMAND = Regex("""^/start(?:@\w+)?\s+([A-Za-z0-9_-]{20,128})$""")
private const val MAX_SMALL_JSON_BODY_BYTES = 64L * 1024
private const val MAX_TELEGRAM_BODY_BYTES = 1024L * 1024
private const val MAX_SNAPSHOT_BODY_BYTES = 30L * 1024 * 1024
private const val MAX_MEDIA_METADATA_BYTES = 32L * 1024
private const val MAX_MEDIA_LIST_ITEMS = 200

private suspend inline fun <reified T> ApplicationCall.receiveJson(maxBytes: Long): T {
    if (!request.contentType().match(ContentType.Application.Json)) {
        throw ApiException(
            HttpStatusCode.UnsupportedMediaType,
            "unsupported_media_type",
            "Content-Type must be application/json",
        )
    }
    val bytes = receiveChannel().readRemaining(maxBytes + 1).readByteArray()
    if (bytes.size.toLong() > maxBytes) {
        throw ApiException(HttpStatusCode.PayloadTooLarge, "payload_too_large", "Request body is too large")
    }
    return ContractJson.decodeFromString(bytes.decodeToString())
}

private fun validDeviceId(value: String?): String {
    val deviceId = value?.trim().orEmpty()
    if (deviceId.length !in 1..128 || deviceId.any { it.isISOControl() }) {
        throw ApiException(HttpStatusCode.BadRequest, "invalid_device_id", "deviceId is invalid")
    }
    return deviceId
}

private fun validPushToken(value: String): String {
    val token = value.trim()
    if (token.length !in 16..4096 || token.any { it.isISOControl() }) {
        throw ApiException(HttpStatusCode.BadRequest, "invalid_push_token", "push token is invalid")
    }
    return token
}

private fun safeFileName(value: String): String {
    val fileName = value.substringAfterLast('/').substringAfterLast('\\').trim()
        .replace(Regex("""[^A-Za-z0-9._-]"""), "_")
    if (fileName.length !in 1..180 || fileName in setOf(".", "..")) {
        throw ApiException(HttpStatusCode.BadRequest, "invalid_file_name", "fileName is invalid")
    }
    return fileName
}

private fun validateChecksum(value: String?) {
    if (value != null && !value.matches(Regex("""[a-fA-F0-9]{64}"""))) {
        throw ApiException(HttpStatusCode.BadRequest, "invalid_checksum", "checksum is invalid")
    }
}

private fun validMediaKind(value: String): MediaKind =
    runCatching { MediaKind.valueOf(value.trim().uppercase()) }.getOrNull()
        ?: throw ApiException(HttpStatusCode.BadRequest, "invalid_kind", "kind must be PHOTO or SCAN")

private fun validMediaClientId(value: String?): String {
    val clientId = value?.trim().orEmpty()
    if (
        clientId.length !in 1..128 ||
        clientId.any { it.isISOControl() || it == '/' || it == '\\' }
    ) {
        throw ApiException(HttpStatusCode.BadRequest, "invalid_client_id", "clientId is invalid")
    }
    return clientId
}

private fun validOptionalMediaLoadId(value: String?): String? {
    val loadId = value?.trim()?.takeIf(String::isNotEmpty) ?: return null
    if (loadId.length > 256 || loadId.any { it.isISOControl() }) {
        throw ApiException(HttpStatusCode.BadRequest, "invalid_load_id", "loadId is invalid")
    }
    return loadId
}

private fun validateMediaContentType(kind: MediaKind, value: String) {
    val normalized = value.substringBefore(';').trim().lowercase()
    val valid = when (kind) {
        MediaKind.PHOTO -> normalized in setOf("image/jpeg", "image/png", "image/webp")
        MediaKind.SCAN -> normalized in setOf("application/pdf", "image/jpeg", "image/png")
    }
    if (!valid) {
        throw ApiException(
            HttpStatusCode.BadRequest,
            "unsupported_media_type",
            "contentType is not supported for this media kind",
        )
    }
}

private fun validateMediaMetadata(metadata: JsonObject) {
    if (ContractJson.encodeToString(metadata).encodeToByteArray().size > MAX_MEDIA_METADATA_BYTES) {
        throw ApiException(HttpStatusCode.BadRequest, "invalid_metadata", "metadata is too large")
    }
    fun depth(value: kotlinx.serialization.json.JsonElement, current: Int): Int = when (value) {
        is JsonObject -> value.values.maxOfOrNull { depth(it, current + 1) } ?: current
        is JsonArray -> value.maxOfOrNull { depth(it, current + 1) } ?: current
        else -> current
    }
    if (depth(metadata, 1) > 8) {
        throw ApiException(HttpStatusCode.BadRequest, "invalid_metadata", "metadata is too deeply nested")
    }
}

private fun nonNegativeLong(value: String?): Long =
    value?.toLongOrNull()?.takeIf { it >= 0 }
        ?: throw ApiException(HttpStatusCode.BadRequest, "invalid_number", "Expected a non-negative integer")

private fun uuid(value: String?, name: String): UUID =
    value?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        ?: throw ApiException(HttpStatusCode.BadRequest, "invalid_$name", "$name must be a UUID")
