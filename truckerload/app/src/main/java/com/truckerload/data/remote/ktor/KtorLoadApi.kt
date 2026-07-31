package com.truckerload.data.remote.ktor

import com.truckerload.data.backup.BackupData
import com.truckerload.data.sync.AccountCloudSnapshot
import com.truckerload.data.sync.AccountCloudSnapshotCodec
import com.truckerload.domain.model.Load
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Load sync over the hosted account snapshot API.
 *
 * Current backend exposes `GET/PUT /v1/sync/snapshot` (account blob). Entity paths
 * (`GET /loads`, `POST /loads/sync`, `PUT /loads/{id}`) are the client surface and
 * map to that snapshot until per-entity routes land server-side.
 */
@Singleton
class KtorLoadApi @Inject constructor(
    private val http: HttpClientProvider,
) {
    suspend fun getLoads(accountId: String): List<Load> {
        val snapshot = fetchSnapshot(accountId) ?: return emptyList()
        return snapshot.backup.loads
    }

    /** Full bidirectional sync payload (push merged snapshot). */
    suspend fun syncLoads(snapshot: AccountCloudSnapshot): AccountCloudSnapshot {
        ensureConfigured()
        val json = AccountCloudSnapshotCodec.toJson(snapshot)
        val response = http.client.put("v1/sync/snapshot") {
            setBody(TextContent(json, ContentType.Application.Json))
        }
        if (response.status == HttpStatusCode.NoContent) return snapshot
        if (!response.status.isSuccess()) {
            throw IOException("Load sync failed HTTP ${response.status.value}")
        }
        val body = response.bodyAsText()
        return AccountCloudSnapshotCodec.fromJson(body)
            ?: throw IOException("Load sync acknowledgement body invalid")
    }

    /** Upsert a single load inside a snapshot write (LWW merge happens server-side). */
    suspend fun putLoad(accountId: String, load: Load): AccountCloudSnapshot {
        val existing = fetchSnapshot(accountId)
        val loads = (existing?.backup?.loads.orEmpty())
            .filterNot { it.id == load.id } + load
        val now = System.currentTimeMillis()
        val snapshot = AccountCloudSnapshot(
            accountId = accountId,
            updatedAt = maxOf(now, (existing?.updatedAt ?: 0L) + 1L),
            backup = (existing?.backup ?: BackupData()).copy(
                loads = loads,
                exportedAt = now,
            ),
            driverProfileJson = existing?.driverProfileJson,
        )
        return syncLoads(snapshot)
    }

    suspend fun fetchSnapshot(accountId: String): AccountCloudSnapshot? {
        ensureConfigured()
        val response = http.client.get("v1/sync/snapshot")
        if (response.status == HttpStatusCode.NoContent) return null
        if (!response.status.isSuccess()) {
            throw IOException("Snapshot download failed HTTP ${response.status.value}")
        }
        val body = response.bodyAsText().takeIf { it.isNotBlank() }
            ?: throw IOException("Snapshot response body is empty")
        val snapshot = AccountCloudSnapshotCodec.fromJson(body)
            ?: throw IOException("Snapshot decode failed")
        if (snapshot.accountId != accountId) {
            throw IOException("Snapshot account does not match active user")
        }
        return snapshot
    }

    private fun ensureConfigured() {
        if (!http.isBackendConfigured()) {
            throw IOException("SYNC_BACKEND_URL not configured")
        }
    }
}
