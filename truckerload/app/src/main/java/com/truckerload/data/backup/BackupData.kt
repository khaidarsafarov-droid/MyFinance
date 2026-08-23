package com.truckerload.data.backup

import androidx.annotation.Keep
import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Paycheck

/**
 * JSON backup payload shared by export and import ([BackupDataCodec]).
 *
 * Photos/scans are omitted from the JSON payload (binary media stays on disk).
 * On restore, media linked to restored load ids is kept; only orphan load attachments are pruned.
 * Maintenance tasks are not part of schema v1 (add in a future schemaVersion).
 *
 * [schemaVersion] is the backup-format version. [version] is the legacy alias
 * written by older app builds; readers accept either.
 */
@Keep
data class BackupData(
    val schemaVersion: Int = BackupSchema.CURRENT,
    val version: Int = BackupSchema.CURRENT,
    val exportedAt: Long = System.currentTimeMillis(),
    /** Owning TruckerLoad account id; restore refuses cross-account injection when set. */
    val accountId: String? = null,
    val loads: List<Load> = emptyList(),
    val paychecks: List<Paycheck> = emptyList(),
    val diesel: List<Diesel> = emptyList(),
)
