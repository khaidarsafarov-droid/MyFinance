package com.truckerload.data.sync

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.truckerload.data.backup.BackupPrefsApplier
import com.truckerload.data.backup.BackupRoomApplier
import com.truckerload.data.backup.BackupSnapshotBuilder
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.DeletedLoadLedger
import com.truckerload.data.local.entities.DriverProfessionalEntity
import com.truckerload.data.local.entities.DriverProfileEntity
import com.truckerload.data.local.toEntity
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.privacy.AesGcmSensitiveFieldCipher
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.data.sync.CloudSyncEngine.pushLocalSnapshot
import com.truckerload.data.sync.cloud.SyncModeStore
import com.truckerload.widget.WidgetDataUpdater
import org.json.JSONObject

/**
 * Account-based cloud data synchronization (Local-First).
 *
 * Stage 1 — first login: [pushLocalSnapshot] uploads Room → account mirror.
 * Stage 2 — same device return: [pullAndMerge] since [CloudSyncCursorStore.lastSyncedAt].
 * Stage 3 — new / empty device: [fullHydrateIfNeeded] restores entire account blob.
 *
 * Conflict rule: Last Write Wins on `updatedAt` ([CloudSyncPolicy]).
 */
object CloudSyncEngine {
    private const val TAG = "CloudSyncEngine"

    data class SyncResult(
        val mode: Mode,
        val pushed: Boolean = false,
        val pulled: Boolean = false,
        val hydrated: Boolean = false,
        val loadsApplied: Int = 0,
        val message: String = "",
        val retryableFailure: Boolean = false,
    ) {
        enum class Mode { SKIPPED, PUSH, PULL, FULL_HYDRATE, PUSH_AND_PULL, DEVICE_SLOT_DENIED }
    }

    /**
     * Cold-start / post-login entry: hydrate empty DB, else pull+push.
     */
    suspend fun onSessionReady(context: Context): SyncResult {
        val app = context.applicationContext
        if (!SyncModeStore(app).allowsCloudCalls()) {
            return SyncResult(
                mode = SyncResult.Mode.SKIPPED,
                message = "device_only",
            )
        }
        val userId = AuthStore(app).currentUserIdOrNull() ?: return SyncResult(
            mode = SyncResult.Mode.SKIPPED,
            message = "no_session",
        )
        when (val gate = DeviceSlotBinder(app).registerCurrentDevice(required = false)) {
            is DeviceSlotResult.SlotTaken -> {
                val message = DeviceSlotBinder(app).userMessage(gate)
                DeviceSlotDenialStore(app).save(message)
                return SyncResult(
                    mode = SyncResult.Mode.DEVICE_SLOT_DENIED,
                    message = message,
                )
            }
            DeviceSlotResult.Allowed, DeviceSlotResult.Skipped, is DeviceSlotResult.Unavailable -> Unit
        }
        val db = AppDatabase.getInstance(app, userId)
        val cursor = CloudSyncCursorStore(app)
        val backend = AccountCloudBackendFactory.create(app)
        cursor.markAttempt(userId)

        val localLoads = LoadRepository(db).getAllLoadsOnce()
        val remote = backend.read(userId)
        val staleMirror = backend.lastReadUsedStaleMirror
        // A failed remote read must not be treated as a cloud snapshot (would hydrate/merge
        // the local mirror as if it were authoritative).
        val remoteSnapshot = remote.takeUnless { staleMirror }
        if (remoteSnapshot != null && CloudSyncPolicy.needsFullHydration(
                lastSyncedAt = cursor.lastSyncedAt(userId),
                localEntityCount = localLoads.size,
                remoteEntityCount = remoteSnapshot.entityCount,
            )
        ) {
            val applied = applyFullHydration(app, db, remoteSnapshot)
            val pushed = pushLocalSnapshot(userId, db, backend)
            cursor.markFullHydration(userId, markSynced = pushed || !backend.remoteConfigured)
            WidgetDataUpdater.updateWidgetData(app)
            Log.i(TAG, "Full hydration for $userId: $applied loads")
            return SyncResult(
                mode = SyncResult.Mode.FULL_HYDRATE,
                pushed = pushed,
                hydrated = true,
                loadsApplied = applied,
                message = "restored_from_cloud",
                retryableFailure = backend.remoteConfigured && (!pushed || staleMirror),
            )
        }

        val lastSyncedAt = cursor.lastSyncedAt(userId)
        var loadsApplied = 0
        var pulled = false
        when {
            remoteSnapshot != null &&
                CloudSyncPolicy.needsInitialMerge(
                    lastSyncedAt = lastSyncedAt,
                    localEntityCount = localLoads.size,
                    remoteEntityCount = remoteSnapshot.entityCount,
                ) -> {
                // FIX: non-empty local + cloud data on first sync — LWW merge, keep unpushed locals
                loadsApplied = mergeSnapshotIntoRoom(
                    db = db,
                    snapshot = remoteSnapshot,
                    lastSyncedAt = lastSyncedAt,
                    deleteOrphans = false,
                )
                pulled = true
                applyDriverProfileIfPresent(db, remoteSnapshot)
                BackupPrefsApplier.apply(app, remoteSnapshot.backup.appSettings)
                Log.i(TAG, "Initial merge for $userId: $loadsApplied rows touched")
            }
            remoteSnapshot != null &&
                CloudSyncPolicy.shouldPullIncremental(lastSyncedAt, remoteSnapshot.updatedAt) -> {
                loadsApplied = mergeSnapshotIntoRoom(
                    db = db,
                    snapshot = remoteSnapshot,
                    lastSyncedAt = lastSyncedAt,
                    deleteOrphans = true,
                )
                pulled = true
                applyDriverProfileIfPresent(db, remoteSnapshot)
                BackupPrefsApplier.apply(app, remoteSnapshot.backup.appSettings)
            }
        }

        // FIX: stale remote read — skip push so local cannot overwrite newer cloud snapshot
        val pushed = if (staleMirror && backend.remoteConfigured) {
            Log.w(TAG, "Skipping push — remote read failed (stale mirror)")
            false
        } else {
            pushLocalSnapshot(userId, db, backend)
        }
        if (pulled || pushed) {
            cursor.markSynced(userId)
        }
        if (pulled) WidgetDataUpdater.updateWidgetData(app)

        return SyncResult(
            mode = when {
                pushed && pulled -> SyncResult.Mode.PUSH_AND_PULL
                pushed -> SyncResult.Mode.PUSH
                pulled -> SyncResult.Mode.PULL
                else -> SyncResult.Mode.SKIPPED
            },
            pushed = pushed,
            pulled = pulled,
            loadsApplied = loadsApplied,
            retryableFailure = backend.remoteConfigured && (!pushed || staleMirror),
        )
    }

    /** Outbound drain: publish current Room state to the account mirror. */
    suspend fun pushLocalSnapshot(context: Context): Boolean {
        val app = context.applicationContext
        if (!SyncModeStore(app).allowsCloudCalls()) return false
        val userId = AuthStore(app).currentUserIdOrNull() ?: return false
        val db = AppDatabase.getInstance(app, userId)
        val ok = pushLocalSnapshot(userId, db, AccountCloudBackendFactory.create(app))
        if (ok) CloudSyncCursorStore(app).markSynced(userId)
        return ok
    }

    private suspend fun pushLocalSnapshot(
        userId: String,
        db: AppDatabase,
        backend: AccountCloudBackend,
    ): Boolean {
        val appContext = AppDatabase.applicationContext() ?: return false
        val existing = backend.read(userId)
        // Local Room (+ prefs) is the source of truth after pull. Do NOT merge remote-only
        // journal entities back in — that resurrects loads the user deleted on this device.
        val localBackup = BackupSnapshotBuilder.build(appContext, db)
        val publishBackup = localBackup.copy(
            loads = CloudSyncPolicy.localSnapshotForPush(localBackup.loads.associateBy { it.id })
                .values.toList(),
            paychecks = CloudSyncPolicy.localSnapshotForPush(
                localBackup.paychecks.associateBy { it.id.toString() },
            ).values.toList(),
            diesel = CloudSyncPolicy.localSnapshotForPush(
                localBackup.diesel.associateBy { it.id.toString() },
            ).values.toList(),
        )
        // A device clock may lag behind another device. Keep the account snapshot
        // timestamp monotonic so a successfully pulled newer snapshot can always be
        // acknowledged by the server instead of entering a permanent stale-write loop.
        val now = maxOf(
            System.currentTimeMillis(),
            (existing?.updatedAt ?: 0L).let { if (it == Long.MAX_VALUE) it else it + 1L },
        )
        val snapshot = AccountCloudSnapshot(
            accountId = userId,
            updatedAt = now,
            backup = publishBackup,
            driverProfileJson = serializeDriverProfile(db.driverProfileDao().getProfile()),
        )
        val result = backend.write(snapshot)
        if (result.successful) {
            Log.i(TAG, "Published account snapshot (${snapshot.entityCount} entities)")
        } else {
            Log.w(TAG, "Account snapshot cached locally but remote acknowledgement failed")
        }
        return result.successful
    }

    private suspend fun applyFullHydration(
        context: Context,
        db: AppDatabase,
        snapshot: AccountCloudSnapshot,
    ): Int {
        val backup = snapshot.backup.copy(
            loads = snapshot.backup.loads.filterNot { load ->
                DeletedLoadLedger.isBlocked(context, load.id, load.tripId)
            },
        )
        BackupRoomApplier.applyFullReplace(db, backup)
        BackupRoomApplier.pruneOrphanMedia(db)
        applyDriverProfileIfPresent(db, snapshot)
        BackupPrefsApplier.apply(context, backup.appSettings)
        return backup.loads.size
    }

    private suspend fun mergeSnapshotIntoRoom(
        db: AppDatabase,
        snapshot: AccountCloudSnapshot,
        lastSyncedAt: Long = 0L,
        deleteOrphans: Boolean = true,
    ): Int {
        val backup = snapshot.backup
        val existing = db.loadDao().getAllLoadsOnce().associateBy { it.id }
        val remoteLoadIds = backup.loads.map { it.id }.toSet()
        // FIX: only propagate cross-device deletions for rows last acked at sync time
        val orphanLoadIds = if (deleteOrphans) {
            CloudSyncPolicy.orphanLocalIdsForPull(
                localIds = existing.keys,
                remoteIds = remoteLoadIds,
                localUpdatedAt = { id -> existing[id]?.updatedAt ?: 0L },
                lastSyncedAt = lastSyncedAt,
            )
        } else {
            emptySet()
        }
        var applied = 0
        db.withTransaction {
            for (orphanId in orphanLoadIds) {
                db.stopDao().deleteByLoadId(orphanId)
                db.penaltyDao().deleteByLoadId(orphanId)
                db.loadDao().deleteById(orphanId)
                applied++
            }
            for (load in backup.loads) {
                val blocked = AppDatabase.applicationContext()?.let { ctx ->
                    DeletedLoadLedger.isBlocked(ctx, load.id, load.tripId)
                } == true
                if (blocked) continue
                val local = existing[load.id]
                val localUpdated = local?.updatedAt
                if (CloudSyncPolicy.remoteWins(localUpdated, load.updatedAt)) {
                    db.stopDao().deleteByLoadId(load.id)
                    db.penaltyDao().deleteByLoadId(load.id)
                    db.loadDao().insert(load.toEntity())
                    if (load.stops.isNotEmpty()) {
                        db.stopDao().insertAll(load.stops.map { it.toEntity(load.id) })
                    }
                    if (load.penalties.isNotEmpty()) {
                        db.penaltyDao().insertAll(load.penalties.map { it.toEntity(load.id) })
                    }
                    applied++
                }
            }
            // Diesel / paychecks: LWW upsert on addedAt; drop local orphans so deletions propagate.
            val localDiesel = DieselRepository(db).getAllDieselOnce()
            val localDieselById = localDiesel.associateBy { it.id }
            val remoteDieselIds = backup.diesel.map { it.id }.toSet()
            val dieselOrphans = if (deleteOrphans) {
                CloudSyncPolicy.orphanLocalIntIdsForPull(
                    localIds = localDieselById.keys,
                    remoteIds = remoteDieselIds,
                    localAddedAt = { id -> localDieselById[id]?.addedAt ?: 0L },
                    lastSyncedAt = lastSyncedAt,
                )
            } else {
                emptySet()
            }
            for (orphanId in dieselOrphans) {
                db.dieselDao().deleteById(orphanId)
                applied++
            }
            val dieselIdsToApply = CloudSyncPolicy.remoteIntIdsToApplyOnPull(
                localById = localDieselById,
                remoteById = backup.diesel.associateBy { it.id },
                updatedAt = { it.addedAt },
            )
            if (dieselIdsToApply.isNotEmpty()) {
                val upserts = backup.diesel.filter { it.id in dieselIdsToApply }.map { it.toEntity() }
                db.dieselDao().insertAll(upserts)
                applied += upserts.size
            }
            val localPay = PaycheckRepository(db).getAllPaychecksOnce()
            val localPayById = localPay.associateBy { it.id }
            val remotePayIds = backup.paychecks.map { it.id }.toSet()
            val payOrphans = if (deleteOrphans) {
                CloudSyncPolicy.orphanLocalIntIdsForPull(
                    localIds = localPayById.keys,
                    remoteIds = remotePayIds,
                    localAddedAt = { id -> localPayById[id]?.addedAt ?: 0L },
                    lastSyncedAt = lastSyncedAt,
                )
            } else {
                emptySet()
            }
            for (orphanId in payOrphans) {
                db.paycheckDao().deleteById(orphanId)
                applied++
            }
            val payIdsToApply = CloudSyncPolicy.remoteIntIdsToApplyOnPull(
                localById = localPayById,
                remoteById = backup.paychecks.associateBy { it.id },
                updatedAt = { it.addedAt },
            )
            if (payIdsToApply.isNotEmpty()) {
                val upserts = backup.paychecks.filter { it.id in payIdsToApply }.map { it.toEntity() }
                db.paycheckDao().insertAll(upserts)
                applied += upserts.size
            }
        }
        // ТО: snapshot LWW — replace local tables only when remote backup includes maintenance.
        if (BackupRoomApplier.carriesMaintenance(backup)) {
            BackupRoomApplier.applyMaintenanceReplace(db, backup)
        }
        BackupRoomApplier.pruneOrphanMedia(db)
        return applied
    }

    private suspend fun applyDriverProfileIfPresent(db: AppDatabase, snapshot: AccountCloudSnapshot) {
        val json = snapshot.driverProfileJson?.takeIf { it.isNotBlank() } ?: return
        val obj = runCatching { JSONObject(json) }.getOrNull() ?: return
        val existing = db.driverProfileDao().getProfile() ?: DriverProfileEntity()
        db.driverProfileDao().upsert(
            existing.copy(
                displayName = obj.optString("displayName").ifBlank { existing.displayName },
                phoneNumber = obj.optString("phoneNumber").takeIf { it.isNotBlank() } ?: existing.phoneNumber,
                homeState = obj.optString("homeState").ifBlank { existing.homeState },
                truckType = obj.optString("truckType").ifBlank { existing.truckType },
                licenseClass = obj.optString("licenseClass").ifBlank { existing.licenseClass },
                cdlNumber = "",
                axleCount = obj.optInt("axleCount", existing.axleCount),
                homeHubCity = obj.optString("homeHubCity").ifBlank { existing.homeHubCity },
                dateOfBirthEpochDay = obj.optLong("dateOfBirthEpochDay")
                    .takeIf { obj.has("dateOfBirthEpochDay") && !obj.isNull("dateOfBirthEpochDay") }
                    ?: existing.dateOfBirthEpochDay,
                lastActive = System.currentTimeMillis(),
            ),
        )
        val leakedCdl = obj.optString("cdlNumber")
        if (leakedCdl.isNotBlank()) {
            absorbLegacyCdl(db, existing.id, leakedCdl)
        }
    }

    private suspend fun absorbLegacyCdl(db: AppDatabase, userId: String, plaintext: String) {
        val existing = db.driverProfessionalDao().get(userId)
        if (existing?.cdlNumberCiphertext?.startsWith(AesGcmSensitiveFieldCipher.PREFIX_V1) == true) {
            return
        }
        db.driverProfessionalDao().upsert(
            (existing ?: DriverProfessionalEntity(userId = userId)).copy(
                cdlNumberCiphertext = AesGcmSensitiveFieldCipher.wrapPlaintextForMigration(plaintext),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    private fun serializeDriverProfile(profile: DriverProfileEntity?): String? {
        if (profile == null) return null
        return JSONObject()
            .put("displayName", profile.displayName)
            .put("phoneNumber", profile.phoneNumber)
            .put("homeState", profile.homeState)
            .put("truckType", profile.truckType)
            .put("licenseClass", profile.licenseClass)
            .put("axleCount", profile.axleCount)
            .put("homeHubCity", profile.homeHubCity)
            .put("dateOfBirthEpochDay", profile.dateOfBirthEpochDay)
            .toString()
    }
}
