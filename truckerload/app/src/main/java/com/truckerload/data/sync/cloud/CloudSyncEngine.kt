package com.truckerload.data.sync.cloud

import android.content.Context
import android.util.Log
import com.truckerload.data.remote.ktor.HttpClientProvider
import com.truckerload.data.remote.ktor.KtorLoadApi
import com.truckerload.data.sync.CloudSyncEngine as LegacyCloudSyncEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud-first sync orchestrator.
 *
 * Respects [SyncModeStore.effectiveMode]:
 * - [SyncMode.DEVICE_ONLY] → skip all Ktor calls (local Room / mirror only).
 * - [SyncMode.HYBRID] / [SyncMode.SERVER_PRIMARY] → delegate to the proven
 *   snapshot engine ([LegacyCloudSyncEngine]) and expose status via [SyncStatusTracker].
 *
 * Room remains the UI source of truth; this engine never blocks home-screen reads.
 */
@Singleton
class CloudSyncEngine @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val syncModeStore: SyncModeStore,
    private val statusTracker: SyncStatusTracker,
    private val httpClientProvider: HttpClientProvider,
    private val ktorLoadApi: KtorLoadApi,
) {
    suspend fun onSessionReady(): LegacyCloudSyncEngine.SyncResult {
        if (!syncModeStore.allowsCloudCalls()) {
            Log.i(TAG, "Skip cloud sync (mode=${syncModeStore.effectiveMode()})")
            statusTracker.markIdle()
            return LegacyCloudSyncEngine.SyncResult(
                mode = LegacyCloudSyncEngine.SyncResult.Mode.SKIPPED,
                message = "device_only",
            )
        }
        statusTracker.markSyncing()
        return try {
            val result = LegacyCloudSyncEngine.onSessionReady(context.applicationContext)
            if (result.retryableFailure) {
                statusTracker.markError(result.message.ifBlank { "sync_retryable_failure" })
            } else {
                statusTracker.markIdle()
            }
            result
        } catch (e: Exception) {
            Log.w(TAG, "Session sync failed", e)
            statusTracker.markError(e.message ?: "sync_failed")
            LegacyCloudSyncEngine.SyncResult(
                mode = LegacyCloudSyncEngine.SyncResult.Mode.SKIPPED,
                message = e.message.orEmpty(),
                retryableFailure = true,
            )
        }
    }

    suspend fun pushLocalSnapshot(): Boolean {
        if (!syncModeStore.allowsCloudCalls()) return false
        statusTracker.markSyncing()
        return try {
            val ok = LegacyCloudSyncEngine.pushLocalSnapshot(context.applicationContext)
            if (ok) statusTracker.markIdle() else statusTracker.markError("push_failed")
            ok
        } catch (e: Exception) {
            statusTracker.markError(e.message ?: "push_failed")
            false
        }
    }

    /**
     * Optional cloud-first load refresh for [SERVER_PRIMARY] / [HYBRID].
     * Pulls snapshot loads via Ktor and merges through the legacy engine path.
     */
    suspend fun refreshLoadsFromCloud(): LegacyCloudSyncEngine.SyncResult {
        if (!syncModeStore.allowsCloudCalls()) {
            return LegacyCloudSyncEngine.SyncResult(
                mode = LegacyCloudSyncEngine.SyncResult.Mode.SKIPPED,
                message = "device_only",
            )
        }
        if (!httpClientProvider.isBackendConfigured()) {
            return LegacyCloudSyncEngine.SyncResult(
                mode = LegacyCloudSyncEngine.SyncResult.Mode.SKIPPED,
                message = "backend_unconfigured",
            )
        }
        // Prefer the full session path so LWW / hydrate rules stay centralized.
        return onSessionReady()
    }

    /** Expose Ktor load API for tests / future per-entity sync. */
    fun loadApi(): KtorLoadApi = ktorLoadApi

    companion object {
        private const val TAG = "CloudSyncEngineV2"
    }
}
