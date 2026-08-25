package com.truckerload.sync

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.withTransaction
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.hilt.work.HiltWorker
import com.truckerload.BuildConfig
import com.truckerload.contract.ContractJson
import com.truckerload.contract.MediaKind
import com.truckerload.contract.MediaMetadata
import com.truckerload.contract.MediaUploadRequest
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.MediaSyncQueueEntity
import com.truckerload.data.local.entities.PhotoEntity
import com.truckerload.data.local.entities.ScanEntity
import com.truckerload.data.preferences.AccountIds
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.sync.DeviceIdentity
import com.truckerload.data.sync.MediaCloudClient
import com.truckerload.data.sync.MediaCloudGate
import com.truckerload.data.sync.MediaFilePolicy
import com.truckerload.data.sync.MediaQueuePolicy
import com.truckerload.data.sync.MediaSyncCursorStore
import com.truckerload.data.sync.MediaValidationException
import com.truckerload.di.UserComponentManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

@HiltWorker
class MediaSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val authStore: AuthStore,
    private val userComponentManager: UserComponentManager,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val gate = MediaCloudGate(applicationContext)
        if (!gate.isEnabled()) return Result.success()
        val accountId = authStore.currentUserIdOrNull() ?: return Result.success()
        // Keep queued local work untouched while a session is temporarily offline
        // or awaiting refresh. Repositories enqueue based on feature configuration,
        // not current token availability, so no media mutation is lost.
        if (authStore.accessTokenOrNull().isNullOrBlank()) return Result.retry()
        val db = userComponentManager.startSession(accountId).database
        val client = MediaCloudClient(
            backendUrl = BuildConfig.SYNC_BACKEND_URL,
            accessToken = authStore::accessTokenOrNull,
            deviceId = DeviceIdentity(applicationContext).id(),
        )
        val queue = db.mediaSyncQueueDao()
        queue.resetStuck(System.currentTimeMillis() - STUCK_AFTER_MS)

        var retryNeeded = false
        for (item in queue.pending(MAX_QUEUE_BATCH)) {
            if (queue.markProcessing(item.id, item.operation, item.updatedAt) != 1) continue
            try {
                when (item.operation) {
                    MediaSyncQueueEntity.OP_UPSERT -> processUpsert(db, client, item)
                    MediaSyncQueueEntity.OP_DELETE -> processDelete(db, client, item)
                    else -> throw MediaValidationException("invalid_operation")
                }
            } catch (error: Throwable) {
                val failure = MediaQueuePolicy.afterFailure(item.attempts, error)
                queue.updateAttempt(
                    id = item.id,
                    operation = item.operation,
                    generation = item.updatedAt,
                    status = failure.status,
                    attempts = failure.attempts,
                    lastError = failure.safeError,
                )
                if (!failure.retry) markLocalFailed(db, item)
                retryNeeded = retryNeeded || failure.retry
            }
        }

        try {
            pullRemote(db, client, accountId)
        } catch (error: Throwable) {
            retryNeeded = retryNeeded || MediaQueuePolicy.afterFailure(0, error).retry
        }
        retryNeeded = retryNeeded ||
            queue.count(MediaSyncQueueEntity.STATUS_PENDING) > 0
        return if (retryNeeded) Result.retry() else Result.success()
    }

    private suspend fun processUpsert(
        db: AppDatabase,
        client: MediaCloudClient,
        item: MediaSyncQueueEntity,
    ) {
        val kind = item.mediaKind()
        val local = when (kind) {
            MediaKind.PHOTO -> db.photoDao().getById(item.localId)
            MediaKind.SCAN -> db.scanDao().getById(item.localId)
        }
        if (local == null) {
            val remoteId = item.remoteMediaId
            if (remoteId != null) client.delete(remoteId)
            db.mediaSyncQueueDao().deleteIfCurrent(item.id, item.operation, item.updatedAt)
            return
        }
        val file = File(item.filePath ?: throw MediaValidationException("missing_file"))
        val validated = MediaFilePolicy.validateUpload(file, kind)
        val metadata = runCatching {
            ContractJson.decodeFromString<JsonObject>(item.metadataJson)
        }.getOrElse { throw MediaValidationException("invalid_metadata") }
        val loadId = when (local) {
            is PhotoEntity -> local.loadId
            is ScanEntity -> local.loadId
            else -> null
        }
        val fileName = when (local) {
            is PhotoEntity -> local.fileName
            is ScanEntity -> local.fileName
            else -> file.name
        }
        val upload = client.requestUpload(
            MediaUploadRequest(
                fileName = fileName,
                contentType = validated.contentType,
                sizeBytes = validated.sizeBytes,
                checksum = validated.sha256,
                kind = kind,
                clientId = item.localId,
                loadId = loadId,
                metadata = metadata,
            ),
        )
        val remoteId = upload.mediaId
        if (upload.alreadyComplete) {
            val attached = db.mediaSyncQueueDao().setRemoteMediaId(
                item.id,
                item.operation,
                item.updatedAt,
                remoteId,
            )
            if (attached != 1) {
                // A concurrent local delete replaced this queue generation.
                // Transfer the discovered remote id to that delete before cleanup.
                db.mediaSyncQueueDao().attachRemoteIdToDelete(item.kind, item.localId, remoteId)
                return
            }
            val complete = upload.media ?: client.get(remoteId)
            markLocalSynced(db, item, complete)
            db.mediaSyncQueueDao().deleteIfCurrent(item.id, item.operation, item.updatedAt)
            return
        }
        val attached = db.mediaSyncQueueDao().setRemoteMediaId(
            item.id,
            item.operation,
            item.updatedAt,
            remoteId,
        )
        if (attached != 1) {
            db.mediaSyncQueueDao().attachRemoteIdToDelete(item.kind, item.localId, remoteId)
            client.delete(remoteId)
            return
        }
        client.putExactBytes(upload, validated.file, validated.contentType, validated.sizeBytes)
        val complete = client.complete(remoteId, validated.sha256)
        markLocalSynced(db, item, complete)
        db.mediaSyncQueueDao().deleteIfCurrent(item.id, item.operation, item.updatedAt)
    }

    private suspend fun processDelete(
        db: AppDatabase,
        client: MediaCloudClient,
        item: MediaSyncQueueEntity,
    ) {
        val remoteId = item.remoteMediaId
        if (remoteId != null) client.delete(remoteId)
        db.mediaSyncQueueDao().deleteIfCurrent(item.id, item.operation, item.updatedAt)
    }

    private suspend fun markLocalSynced(
        db: AppDatabase,
        item: MediaSyncQueueEntity,
        metadata: MediaMetadata,
    ) {
        when (item.mediaKind()) {
            MediaKind.PHOTO -> db.photoDao().updateCloudState(
                item.localId,
                metadata.mediaId,
                PhotoEntity.CLOUD_SYNCED,
                metadata.updatedAt,
            )
            MediaKind.SCAN -> db.scanDao().updateCloudState(
                item.localId,
                metadata.mediaId,
                ScanEntity.CLOUD_SYNCED,
                metadata.updatedAt,
            )
        }
    }

    private suspend fun markLocalFailed(db: AppDatabase, item: MediaSyncQueueEntity) {
        when (runCatching { item.mediaKind() }.getOrNull()) {
            MediaKind.PHOTO -> db.photoDao().updateCloudStatus(item.localId, PhotoEntity.CLOUD_FAILED)
            MediaKind.SCAN -> db.scanDao().updateCloudStatus(item.localId, ScanEntity.CLOUD_FAILED)
            null -> Unit
        }
    }

    private suspend fun pullRemote(db: AppDatabase, client: MediaCloudClient, accountId: String) {
        val cursors = MediaSyncCursorStore(applicationContext)
        val since = cursors.get(accountId)
        val response = client.list(since)
        for (metadata in response.items) {
            try {
                if (metadata.status == "deleted" || metadata.deletedAt != null) {
                    applyRemoteDelete(db, metadata)
                } else if (metadata.status == "ready") {
                    applyRemoteMedia(db, client, accountId, metadata)
                }
            } catch (error: MediaValidationException) {
                // A malformed owned record is permanently rejected. Advancing the
                // opaque revision prevents one bad row from blocking all later media.
            }
        }
        cursors.set(accountId, response.nextSince)
    }

    private suspend fun applyRemoteDelete(db: AppDatabase, metadata: MediaMetadata) {
        val kind = metadata.kind.queueKind()
        val pending = db.mediaSyncQueueDao().get(kind, metadata.clientId)
        if (pending?.operation == MediaSyncQueueEntity.OP_UPSERT) {
            // A local edit queued after the remote tombstone must be uploaded; do not
            // destroy the only local copy while that durable mutation is pending.
            return
        }
        val existingPath = when (metadata.kind) {
            MediaKind.PHOTO -> db.photoDao().getById(metadata.clientId)?.filePath
            MediaKind.SCAN -> db.scanDao().getById(metadata.clientId)?.filePath
        }
        db.withTransaction {
            db.mediaSyncQueueDao().deleteForLocal(kind, metadata.clientId)
            when (metadata.kind) {
                MediaKind.PHOTO -> db.photoDao().deleteById(metadata.clientId)
                MediaKind.SCAN -> db.scanDao().deleteById(metadata.clientId)
            }
        }
        existingPath?.let { runCatching { File(it).delete() } }
    }

    private suspend fun applyRemoteMedia(
        db: AppDatabase,
        client: MediaCloudClient,
        accountId: String,
        metadata: MediaMetadata,
    ) {
        MediaFilePolicy.validateRemote(
            metadata.kind,
            metadata.clientId,
            metadata.fileName,
            metadata.contentType,
            metadata.sizeBytes,
            metadata.checksum,
        )
        val queueKind = metadata.kind.queueKind()
        val pending = db.mediaSyncQueueDao().get(queueKind, metadata.clientId)
        if (pending?.operation == MediaSyncQueueEntity.OP_UPSERT) return
        val existing = when (metadata.kind) {
            MediaKind.PHOTO -> db.photoDao().getById(metadata.clientId)
            MediaKind.SCAN -> db.scanDao().getById(metadata.clientId)
        }
        val existingFile = when (existing) {
            is PhotoEntity -> File(existing.filePath)
            is ScanEntity -> File(existing.filePath)
            else -> null
        }
        if (existingFile?.isFile == true && existingFile.length() == metadata.sizeBytes) {
            val validExisting = runCatching {
                MediaFilePolicy.contentMatches(existingFile, metadata.contentType)
                MediaFilePolicy.verifyChecksum(existingFile, metadata.checksum)
            }.isSuccess
            if (validExisting) {
                markExistingRemoteState(db, metadata)
                return
            }
        }

        val root = applicationContext.getExternalFilesDir(null) ?: applicationContext.filesDir
        val accountDir = File(
            root,
            "cloud_media/${AccountIds.sanitizeFilePart(accountId)}/${metadata.kind.name.lowercase()}",
        )
        if (!accountDir.mkdirs() && !accountDir.isDirectory) {
            throw java.io.IOException("media_directory_unavailable")
        }
        val destination = File(
            accountDir,
            MediaFilePolicy.destinationName(metadata.kind, metadata.clientId, metadata.contentType),
        )
        val canonicalRoot = accountDir.canonicalFile
        val canonicalParent = destination.canonicalFile.parentFile
            ?: throw MediaValidationException("invalid_destination")
        if (canonicalParent != canonicalRoot) {
            throw MediaValidationException("invalid_destination")
        }
        val temporary = File.createTempFile(".download-", ".tmp", accountDir)
        try {
            client.download(metadata, temporary)
            moveAtomically(temporary, destination)
            var accepted = false
            db.withTransaction {
                val nowPending = db.mediaSyncQueueDao().get(queueKind, metadata.clientId)
                if (nowPending?.operation == MediaSyncQueueEntity.OP_UPSERT) return@withTransaction
                when (metadata.kind) {
                    MediaKind.PHOTO -> {
                        val current = db.photoDao().getById(metadata.clientId)
                        db.photoDao().insert(metadata.toPhoto(destination, current))
                    }
                    MediaKind.SCAN -> {
                        val current = db.scanDao().getById(metadata.clientId)
                        db.scanDao().insert(metadata.toScan(destination, current))
                    }
                }
                accepted = true
            }
            if (!accepted) destination.delete()
        } finally {
            temporary.delete()
        }
    }

    private suspend fun markExistingRemoteState(db: AppDatabase, metadata: MediaMetadata) {
        when (metadata.kind) {
            MediaKind.PHOTO -> db.photoDao().updateCloudState(
                metadata.clientId,
                metadata.mediaId,
                PhotoEntity.CLOUD_SYNCED,
                metadata.updatedAt,
            )
            MediaKind.SCAN -> db.scanDao().updateCloudState(
                metadata.clientId,
                metadata.mediaId,
                ScanEntity.CLOUD_SYNCED,
                metadata.updatedAt,
            )
        }
    }

    private fun MediaMetadata.toPhoto(destination: File, current: PhotoEntity?): PhotoEntity {
        val json = metadata
        return PhotoEntity(
            id = clientId,
            fileName = fileName,
            filePath = destination.absolutePath,
            latitude = json.double("latitude") ?: current?.latitude ?: 0.0,
            longitude = json.double("longitude") ?: current?.longitude ?: 0.0,
            city = json.string("city") ?: current?.city.orEmpty(),
            state = json.string("state") ?: current?.state.orEmpty(),
            zipCode = json.string("zipCode") ?: current?.zipCode.orEmpty(),
            timestamp = json.long("timestamp") ?: current?.timestamp ?: createdAt,
            loadId = loadId,
            cloudMediaId = mediaId,
            cloudSyncStatus = PhotoEntity.CLOUD_SYNCED,
            cloudUpdatedAt = updatedAt,
        )
    }

    private fun MediaMetadata.toScan(destination: File, current: ScanEntity?): ScanEntity {
        val json = metadata
        return ScanEntity(
            id = clientId,
            fileName = fileName,
            filePath = destination.absolutePath,
            timestamp = json.long("timestamp") ?: current?.timestamp ?: createdAt,
            fileSizeBytes = sizeBytes,
            pageCount = json.int("pageCount") ?: current?.pageCount ?: 1,
            ocrText = json.string("ocrText") ?: current?.ocrText.orEmpty(),
            loadId = loadId,
            category = json.string("category") ?: current?.category ?: "OTHER",
            cloudMediaId = mediaId,
            cloudSyncStatus = ScanEntity.CLOUD_SYNCED,
            cloudUpdatedAt = updatedAt,
        )
    }

    private fun JsonObject.string(key: String): String? =
        get(key)?.jsonPrimitive?.content?.takeIf(String::isNotBlank)

    private fun JsonObject.long(key: String): Long? = get(key)?.jsonPrimitive?.longOrNull
    private fun JsonObject.int(key: String): Int? = get(key)?.jsonPrimitive?.intOrNull
    private fun JsonObject.double(key: String): Double? = get(key)?.jsonPrimitive?.doubleOrNull

    private fun MediaSyncQueueEntity.mediaKind(): MediaKind =
        runCatching { MediaKind.valueOf(kind) }.getOrElse {
            throw MediaValidationException("invalid_kind")
        }

    private fun MediaKind.queueKind(): String = name

    private fun moveAtomically(source: File, destination: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            moveAtomicallyApi26(source, destination)
            return
        }
        if (destination.exists() && !destination.delete()) {
            throw java.io.IOException("media_destination_replace_failed")
        }
        if (!source.renameTo(destination)) {
            source.inputStream().use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }
            if (!source.delete()) {
                destination.delete()
                throw java.io.IOException("media_source_cleanup_failed")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun moveAtomicallyApi26(source: File, destination: File) {
        try {
            Files.move(
                source.toPath(),
                destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    companion object {
        const val UNIQUE_ONESHOT = "media_cloud_sync_oneshot"
        const val UNIQUE_PERIODIC = "media_cloud_sync_periodic"
        private const val MAX_QUEUE_BATCH = 20
        private const val STUCK_AFTER_MS = 15L * 60 * 1000

        fun enqueue(context: Context) {
            val app = context.applicationContext
            if (!MediaCloudGate(app).isEnabled()) return
            val request = OneTimeWorkRequestBuilder<MediaSyncWorker>()
                .setConstraints(networkConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(app).enqueueUniqueWork(
                UNIQUE_ONESHOT,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }

        fun enqueuePeriodic(context: Context) {
            val app = context.applicationContext
            if (!MediaCloudGate(app).isEnabled()) return
            val request = PeriodicWorkRequestBuilder<MediaSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(networkConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(app).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        private fun networkConstraints() = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}
