package com.truckerload.data.sync.cloud

import android.content.Context
import android.util.Log
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.AuthStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room ↔ cloud bridge for loads without a Room schema bump.
 *
 * - UI always reads Room via [com.truckerload.data.repository.LoadRepository].
 * - Pending cloud mutations are represented by `sync_outbox` rows (not a
 *   `syncPending` column — Room stays at v29).
 * - `lastSyncedAt` lives in [com.truckerload.data.sync.CloudSyncCursorStore].
 *
 * When [SyncMode.SERVER_PRIMARY] or [SyncMode.HYBRID], [refreshIfOnline] pulls
 * from Ktor then merges into Room (LWW). [SyncMode.DEVICE_ONLY] is a no-op.
 */
@Singleton
class LoadCloudBridge @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val syncModeStore: SyncModeStore,
    private val cloudSyncEngine: CloudSyncEngine,
    private val authStore: AuthStore,
) {
    suspend fun refreshIfOnline() {
        if (!syncModeStore.allowsCloudCalls()) return
        val userId = authStore.currentUserIdOrNull() ?: return
        runCatching { AppDatabase.getInstance(context.applicationContext, userId) }
        val result = cloudSyncEngine.refreshLoadsFromCloud()
        Log.d(
            TAG,
            "refreshIfOnline mode=${result.mode} pulled=${result.pulled} loads=${result.loadsApplied}",
        )
    }

    /** True when local mutations are waiting in the outbox (stand-in for syncPending). */
    suspend fun hasPendingSync(): Boolean {
        val userId = authStore.currentUserIdOrNull() ?: return false
        val db = AppDatabase.getInstance(context.applicationContext, userId)
        return runCatching {
            db.syncOutboxDao().countByStatus(
                com.truckerload.data.local.entities.SyncOutboxEntity.STATUS_PENDING,
            )
        }.getOrDefault(0) > 0
    }

    companion object {
        private const val TAG = "LoadCloudBridge"
    }
}
