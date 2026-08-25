package com.truckerload.backend

import io.micrometer.core.instrument.Counter
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class BackendMetrics(
    val registry: PrometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
) {
    private lateinit var snapshotAccepted: Counter
    private lateinit var snapshotStale: Counter
    private lateinit var telegramAccepted: Counter
    private lateinit var telegramRejected: Counter
    private lateinit var telegramDuplicate: Counter
    private lateinit var pushFailures: Counter
    private val rateLimitedByBucket = ConcurrentHashMap<String, Counter>()

    @Synchronized
    fun initialize() {
        if (::snapshotAccepted.isInitialized) return
        snapshotAccepted = resultCounter(SNAPSHOT_WRITES, "accepted")
        snapshotStale = resultCounter(SNAPSHOT_WRITES, "stale")
        telegramAccepted = resultCounter(TELEGRAM_WEBHOOK_UPDATES, "accepted")
        telegramRejected = resultCounter(TELEGRAM_WEBHOOK_UPDATES, "rejected")
        telegramDuplicate = resultCounter(TELEGRAM_WEBHOOK_UPDATES, "duplicate")
        pushFailures = Counter.builder(PUSH_NOTIFICATION_FAILURES)
            .description("FCM messages that could not be delivered")
            .register(registry)
    }

    fun recordSnapshot(accepted: Boolean) {
        initialize()
        if (accepted) snapshotAccepted.increment() else snapshotStale.increment()
    }

    fun recordTelegramAccepted() {
        initialize()
        telegramAccepted.increment()
    }

    fun recordTelegramRejected() {
        initialize()
        telegramRejected.increment()
    }

    fun recordTelegramDuplicate() {
        initialize()
        telegramDuplicate.increment()
    }

    fun recordPushFailures(count: Int) {
        initialize()
        if (count > 0) pushFailures.increment(count.toDouble())
    }

    /** Stage3: rate-limit hits for alert rules (webhook brute-force, sync floods). */
    fun recordRateLimited(bucket: String) {
        initialize()
        val normalized = bucket.substringBefore(':').ifBlank { "unknown" }
        rateLimitedByBucket.computeIfAbsent(normalized) {
            Counter.builder(HTTP_RATE_LIMITED)
                .description("Requests rejected by in-process rate limiter")
                .tag("bucket", normalized)
                .register(registry)
        }.increment()
    }

    fun recordHttp(method: String, status: Int, durationNanos: Long) {
        registry.timer(
            HTTP_REQUESTS,
            "method",
            method,
            "status",
            status.toString(),
        ).record(durationNanos.coerceAtLeast(0), TimeUnit.NANOSECONDS)
    }

    fun scrape(): String {
        initialize()
        return registry.scrape()
    }

    private fun resultCounter(name: String, result: String): Counter =
        Counter.builder(name)
            .description("TruckoRig backend operation outcomes")
            .tag("result", result)
            .register(registry)

    companion object {
        const val HTTP_REQUESTS = "truckerload.http.server.requests"
        const val HTTP_RATE_LIMITED = "truckerload.http.rate_limited"
        const val SNAPSHOT_WRITES = "truckerload.snapshot.writes"
        const val TELEGRAM_WEBHOOK_UPDATES = "truckerload.telegram.webhook.updates"
        const val PUSH_NOTIFICATION_FAILURES = "truckerload.push.notification.failures"
    }
}
