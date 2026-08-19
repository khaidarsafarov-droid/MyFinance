package com.truckerload.data.sync

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.truckerload.data.backup.BackupData
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.DriverProfessionalEntity
import com.truckerload.data.local.entities.DriverProfileEntity
import com.truckerload.data.privacy.AesGcmSensitiveFieldCipher
import com.truckerload.data.local.toDomain
import com.truckerload.data.local.toEntity
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
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

        val remoteSnapshot = remote
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
                retryableFailure = backend.remoteConfigured && !pushed,
            )
        }

        var loadsApplied = 0
        var pulled = false
        if (remote != null && CloudSyncPolicy.shouldPullIncremental(cursor.lastSyncedAt(userId), remote.updatedAt)) {
            loadsApplied = mergeSnapshotIntoRoom(db, remote)
            pulled = true
            applyDriverProfileIfPresent(db, remote)
        }

        val pushed = pushLocalSnapshot(userId, db, backend)
        if (pushed) {
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
            retryableFailure = backend.remoteConfigured && !pushed,
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
        val loads = LoadRepository(db).getAllLoadsOnce()
        val paychecks = PaycheckRepository(db).getAllPaychecksOnce()
        val diesel = DieselRepository(db).getAllDieselOnce()
        val existing = backend.read(userId)
        val localBackup = BackupData(
            loads = loads,
            paychecks = paychecks,
            diesel = diesel,
        )
        val mergedBackup = if (existing != null) {
            mergeBackupsLww(existing.backup, localBackup)
        } else {
            localBackup
        }
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
            backup = mergedBackup,
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

    private fun mergeBackupsLww(remote: BackupData, local: BackupData): BackupData {
        val loads = CloudSyncPolicy.mergeById(
            local = local.loads.associateBy { it.id },
            remote = remote.loads.associateBy { it.id },
            updatedAt = { it.updatedAt },
        ).values.toList()
        // Prefer local paycheck/diesel maps by id string; LWW on addedAt when present.
        val payLocal = local.paychecks.associateBy { it.id.toString() }
        val payRemote = remote.paychecks.associateBy { it.id.toString() }
        val paychecks = CloudSyncPolicy.mergeById(payLocal, payRemote) { it.addedAt }.values.toList()
        val dieselLocal = local.diesel.associateBy { it.id.toString() }
        val dieselRemote = remote.diesel.associateBy { it.id.toString() }
        val diesel = CloudSyncPolicy.mergeById(dieselLocal, dieselRemote) { it.addedAt }.values.toList()
        return BackupData(loads = loads, paychecks = paychecks, diesel = diesel, exportedAt = System.currentTimeMillis())
    }

    private suspend fun applyFullHydration(
        context: Context,
        db: AppDatabase,
        snapshot: AccountCloudSnapshot,
    ): Int {
        val backup = snapshot.backup
        db.withTransaction {
            db.dieselDao().deleteAll()
            db.paycheckDao().deleteAll()
            db.loadHistoryDao().deleteAll()
            db.loadDao().deleteAll()
            backup.loads.forEach { load ->
                db.loadDao().insert(load.toEntity())
                if (load.stops.isNotEmpty()) {
                    db.stopDao().insertAll(load.stops.map { it.toEntity(load.id) })
                }
                if (load.penalties.isNotEmpty()) {
                    db.penaltyDao().insertAll(load.penalties.map { it.toEntity(load.id) })
                }
            }
            if (backup.paychecks.isNotEmpty()) {
                db.paycheckDao().insertAll(backup.paychecks.map { it.toEntity() })
            }
            if (backup.diesel.isNotEmpty()) {
                db.dieselDao().insertAll(backup.diesel.map { it.toEntity() })
            }
        }
        applyDriverProfileIfPresent(db, snapshot)
        return backup.loads.size
    }

    private suspend fun mergeSnapshotIntoRoom(db: AppDatabase, snapshot: AccountCloudSnapshot): Int {
        val backup = snapshot.backup
        val existing = db.loadDao().getAllLoadsOnce().associateBy { it.id }
        var applied = 0
        db.withTransaction {
            for (load in backup.loads) {
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
            // Diesel / paychecks: insert missing by id (Room replace).
            val localDieselIds = DieselRepository(db).getAllDieselOnce().map { it.id }.toSet()
            val toInsertDiesel = backup.diesel.filter { it.id !in localDieselIds }
            if (toInsertDiesel.isNotEmpty()) {
                db.dieselDao().insertAll(toInsertDiesel.map { it.toEntity() })
            }
            val localPayIds = PaycheckRepository(db).getAllPaychecksOnce().map { it.id }.toSet()
            val toInsertPay = backup.paychecks.filter { it.id !in localPayIds }
            if (toInsertPay.isNotEmpty()) {
                db.paycheckDao().insertAll(toInsertPay.map { it.toEntity() })
            }
        }
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
