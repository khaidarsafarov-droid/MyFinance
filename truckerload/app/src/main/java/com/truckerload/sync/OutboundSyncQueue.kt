package com.truckerload.sync

import android.content.Context
import android.util.Log
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.SyncOutboxEntity
import org.json.JSONObject
import java.util.UUID

/**
 * Enqueues local mutations for hybrid online push (bot/server).
 * Always writes Room first; [OutboundSyncWorker] drains when online.
 */
object OutboundSyncQueue {
    private const val TAG = "OutboundSyncQueue"

    suspend fun enqueueLoadUpsert(context: Context, loadId: String, summaryJson: JSONObject = JSONObject()) {
        enqueue(
            context = context,
            entityType = SyncOutboxEntity.TYPE_LOAD,
            entityId = loadId,
            op = SyncOutboxEntity.OP_UPSERT,
            payloadJson = summaryJson.put("loadId", loadId).toString(),
        )
    }

    suspend fun enqueueLoadDelete(context: Context, loadId: String) {
        enqueue(
            context = context,
            entityType = SyncOutboxEntity.TYPE_LOAD,
            entityId = loadId,
            op = SyncOutboxEntity.OP_DELETE,
            payloadJson = JSONObject().put("loadId", loadId).put("op", "delete").toString(),
        )
    }

    suspend fun enqueueDieselUpsert(context: Context, dieselId: String, summaryJson: JSONObject = JSONObject()) {
        enqueue(
            context = context,
            entityType = SyncOutboxEntity.TYPE_DIESEL,
            entityId = dieselId,
            op = SyncOutboxEntity.OP_UPSERT,
            payloadJson = summaryJson.put("dieselId", dieselId).toString(),
        )
    }

    suspend fun enqueueProfileUpsert(context: Context, profileId: String, summaryJson: JSONObject = JSONObject()) {
        enqueue(
            context = context,
            entityType = SyncOutboxEntity.TYPE_PROFILE,
            entityId = profileId,
            op = SyncOutboxEntity.OP_UPSERT,
            payloadJson = summaryJson.put("profileId", profileId).toString(),
        )
    }

    suspend fun enqueue(
        context: Context,
        entityType: String,
        entityId: String,
        op: String,
        payloadJson: String,
    ) {
        val db = AppDatabase.getInstanceForActiveUser(context) ?: run {
            Log.w(TAG, "No active user — skip outbox enqueue")
            return
        }
        val now = System.currentTimeMillis()
        db.syncOutboxDao().upsert(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                entityType = entityType,
                entityId = entityId,
                op = op,
                payloadJson = payloadJson,
                status = SyncOutboxEntity.STATUS_PENDING,
                createdAt = now,
                updatedAt = now,
            ),
        )
        OutboundSyncWorker.enqueue(context)
        CloudSyncWorker.enqueue(context)
    }
}
