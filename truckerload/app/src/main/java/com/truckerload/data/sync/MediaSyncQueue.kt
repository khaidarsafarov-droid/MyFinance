package com.truckerload.data.sync

import android.content.Context
import com.truckerload.contract.ContractJson
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.MediaSyncQueueEntity
import com.truckerload.data.local.entities.PhotoEntity
import com.truckerload.data.local.entities.ScanEntity
import com.truckerload.sync.MediaSyncWorker
import java.util.UUID
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

interface MediaSyncEnqueuer {
    fun enabled(): Boolean
    suspend fun enqueuePhotoUpsert(photo: PhotoEntity)
    suspend fun enqueueScanUpsert(scan: ScanEntity)
    suspend fun enqueuePhotoDelete(photo: PhotoEntity)
    suspend fun enqueueScanDelete(scan: ScanEntity)
    fun schedule()

    companion object {
        fun forDatabase(db: AppDatabase): MediaSyncEnqueuer {
            val context = AppDatabase.applicationContext() ?: return DisabledMediaSyncEnqueuer
            return RoomMediaSyncEnqueuer(context, db)
        }
    }
}

object DisabledMediaSyncEnqueuer : MediaSyncEnqueuer {
    override fun enabled() = false
    override suspend fun enqueuePhotoUpsert(photo: PhotoEntity) = Unit
    override suspend fun enqueueScanUpsert(scan: ScanEntity) = Unit
    override suspend fun enqueuePhotoDelete(photo: PhotoEntity) = Unit
    override suspend fun enqueueScanDelete(scan: ScanEntity) = Unit
    override fun schedule() = Unit
}

class RoomMediaSyncEnqueuer(
    context: Context,
    private val db: AppDatabase,
    private val gate: MediaCloudGate = MediaCloudGate(context),
) : MediaSyncEnqueuer {
    private val appContext = context.applicationContext
    private val dao = db.mediaSyncQueueDao()

    override fun enabled(): Boolean = gate.isEnabled()

    override suspend fun enqueuePhotoUpsert(photo: PhotoEntity) {
        if (!enabled()) return
        enqueue(
            localId = photo.id,
            kind = MediaSyncQueueEntity.KIND_PHOTO,
            operation = MediaSyncQueueEntity.OP_UPSERT,
            remoteMediaId = photo.cloudMediaId,
            filePath = photo.filePath,
            metadata = buildJsonObject {
                put("timestamp", photo.timestamp)
                put("latitude", photo.latitude)
                put("longitude", photo.longitude)
                put("city", photo.city)
                put("state", photo.state)
                put("zipCode", photo.zipCode)
                photo.loadId?.let { put("loadId", it) }
            },
        )
    }

    override suspend fun enqueueScanUpsert(scan: ScanEntity) {
        if (!enabled()) return
        enqueue(
            localId = scan.id,
            kind = MediaSyncQueueEntity.KIND_SCAN,
            operation = MediaSyncQueueEntity.OP_UPSERT,
            remoteMediaId = scan.cloudMediaId,
            filePath = scan.filePath,
            metadata = buildJsonObject {
                put("timestamp", scan.timestamp)
                put("pageCount", scan.pageCount)
                put("ocrText", scan.ocrText)
                put("category", scan.category)
                scan.loadId?.let { put("loadId", it) }
            },
        )
    }

    override suspend fun enqueuePhotoDelete(photo: PhotoEntity) {
        if (!enabled()) return
        enqueueDelete(photo.id, MediaSyncQueueEntity.KIND_PHOTO, photo.cloudMediaId)
    }

    override suspend fun enqueueScanDelete(scan: ScanEntity) {
        if (!enabled()) return
        enqueueDelete(scan.id, MediaSyncQueueEntity.KIND_SCAN, scan.cloudMediaId)
    }

    private suspend fun enqueueDelete(localId: String, kind: String, remoteMediaId: String?) {
        val existing = dao.get(kind, localId)
        enqueue(
            localId = localId,
            kind = kind,
            operation = MediaSyncQueueEntity.OP_DELETE,
            remoteMediaId = remoteMediaId ?: existing?.remoteMediaId,
            filePath = null,
            metadata = buildJsonObject { },
        )
    }

    private suspend fun enqueue(
        localId: String,
        kind: String,
        operation: String,
        remoteMediaId: String?,
        filePath: String?,
        metadata: JsonObject,
    ) {
        val existing = dao.get(kind, localId)
        val now = System.currentTimeMillis()
        val generation = maxOf(now, (existing?.updatedAt ?: Long.MIN_VALUE) + 1)
        dao.upsert(
            MediaSyncQueueEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                localId = localId,
                kind = kind,
                operation = operation,
                remoteMediaId = remoteMediaId ?: existing?.remoteMediaId,
                filePath = filePath,
                metadataJson = ContractJson.encodeToString(metadata),
                attempts = 0,
                lastError = null,
                createdAt = existing?.createdAt ?: now,
                updatedAt = generation,
                status = MediaSyncQueueEntity.STATUS_PENDING,
            ),
        )
    }

    override fun schedule() {
        if (enabled()) MediaSyncWorker.enqueue(appContext)
    }
}
