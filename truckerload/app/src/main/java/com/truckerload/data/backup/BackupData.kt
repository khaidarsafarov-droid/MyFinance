package com.truckerload.data.backup

import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Paycheck

/** Структура резервной копии БД для JSON экспорта/импорта.
 *
 * Photos/scans are intentionally omitted: binary media lives on disk under
 * app external-files and is not serialized into BackupData. Restore wipes
 * load rows then re-inserts loads/pay/diesel; media files with dangling
 * loadIds are purged by BackupService orphan cleanup before reload.
 */
data class BackupData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val loads: List<Load> = emptyList(),
    val paychecks: List<Paycheck> = emptyList(),
    val diesel: List<Diesel> = emptyList()
)
