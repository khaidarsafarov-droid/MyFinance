package com.truckerload.data.sync

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.truckerload.data.backup.BackupData

/**
 * Account-scoped cloud payload (server row / mirror blob).
 * Extends journal backup with driver profile metadata for cross-device restore.
 */
data class AccountCloudSnapshot(
    val version: Int = 1,
    val accountId: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val backup: BackupData = BackupData(),
    /** Serialized driver professional fields (CDL, hub, DOB, …). */
    val driverProfileJson: String? = null,
) {
    val entityCount: Int
        get() = backup.loads.size + backup.paychecks.size + backup.diesel.size
}

object AccountCloudSnapshotCodec {
    val gson: Gson = GsonBuilder().create()

    fun toJson(snapshot: AccountCloudSnapshot): String = gson.toJson(snapshot)

    fun fromJson(json: String): AccountCloudSnapshot? =
        runCatching { gson.fromJson(json, AccountCloudSnapshot::class.java) }.getOrNull()
            ?.takeIf { it.version >= 1 && it.accountId.isNotBlank() }
}
