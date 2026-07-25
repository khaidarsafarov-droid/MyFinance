package com.truckerload.backend

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

interface PushNotifier : AutoCloseable {
    suspend fun notifySync(tokens: List<String>)
    override fun close() = Unit
}

object NoOpPushNotifier : PushNotifier {
    override suspend fun notifySync(tokens: List<String>) = Unit
}

class FirebasePushNotifier private constructor(
    private val app: FirebaseApp,
) : PushNotifier {
    override suspend fun notifySync(tokens: List<String>) {
        if (tokens.isEmpty()) return
        withContext(Dispatchers.IO) {
            tokens.distinct().chunked(MAX_MULTICAST_TOKENS).forEach { batch ->
                val messages = batch.map { token ->
                    Message.builder()
                        .putData("type", "sync")
                        .setToken(token)
                        .build()
                }
                runCatching {
                    FirebaseMessaging.getInstance(app).sendEach(messages)
                }.onFailure { error ->
                    log.warn("Firebase sync notification failed: {}", error.javaClass.simpleName)
                }
            }
        }
    }

    override fun close() {
        runCatching { app.delete() }
    }

    companion object {
        private const val APP_NAME = "truckerload-backend"
        private const val MAX_MULTICAST_TOKENS = 500
        private val log = LoggerFactory.getLogger(FirebasePushNotifier::class.java)

        fun createOrNoOp(projectId: String?): PushNotifier {
            if (projectId.isNullOrBlank()) {
                log.warn("FIREBASE_PROJECT_ID is not configured; push notifications are disabled")
                return NoOpPushNotifier
            }
            return runCatching {
                val existing = FirebaseApp.getApps().firstOrNull { it.name == APP_NAME }
                if (existing != null) {
                    FirebasePushNotifier(existing)
                } else {
                    val options = FirebaseOptions.builder()
                        .setProjectId(projectId)
                        .setCredentials(GoogleCredentials.getApplicationDefault())
                        .build()
                    FirebasePushNotifier(FirebaseApp.initializeApp(options, APP_NAME))
                }
            }.getOrElse { error ->
                log.warn(
                    "Firebase application default credentials are unavailable; push notifications are disabled: {}",
                    error.javaClass.simpleName,
                )
                NoOpPushNotifier
            }
        }
    }
}
