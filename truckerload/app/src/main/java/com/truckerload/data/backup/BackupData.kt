package com.truckerload.data.backup

import androidx.annotation.Keep
import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Paycheck

/**
 * JSON backup payload shared by export and import ([BackupDataCodec]).
 *
 * Photos/scans binaries stay on disk (optional media sync is separate).
 * Schema v2 adds maintenance (ТО), archive receipts metadata, and app settings
 * so cloud / Drive / file restore brings back the full account, not only the journal.
 *
 * [schemaVersion] is the backup-format version. [version] is the legacy alias
 * written by older app builds; readers accept either.
 */
@Keep
data class BackupData(
    /** 0 = unset (legacy JSON); writers always emit [BackupSchema.CURRENT]. */
    val schemaVersion: Int = 0,
    val version: Int = 0,
    val exportedAt: Long = System.currentTimeMillis(),
    /** Owning TruckerLoad account id; restore refuses cross-account injection when set. */
    val accountId: String? = null,
    val loads: List<Load> = emptyList(),
    val paychecks: List<Paycheck> = emptyList(),
    val diesel: List<Diesel> = emptyList(),
    val maintenanceTasks: List<BackupMaintenanceTask> = emptyList(),
    val maintenanceArchive: List<BackupMaintenanceArchive> = emptyList(),
    val appSettings: BackupAppSettings? = null,
)
