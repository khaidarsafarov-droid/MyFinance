package com.truckerload.data.repository

import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.PerDiemDayOverrideEntity
import com.truckerload.domain.tax.PerDiemOverrideMutation
import com.truckerload.domain.tax.PerDiemOverrideSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PerDiemOverrideRepository(
    private val db: AppDatabase,
) {
    private val dao = db.perDiemDayOverrideDao()

    suspend fun snapshotForYear(year: Int): PerDiemOverrideSnapshot = withContext(Dispatchers.IO) {
        val rows = dao.getForYear("$year-%")
        PerDiemOverrideSnapshot(
            included = rows.filter { it.included }.map { it.date }.toSet(),
            excluded = rows.filter { !it.included }.map { it.date }.toSet(),
        )
    }

    suspend fun applyMutation(isoDate: String, mutation: PerDiemOverrideMutation) {
        withContext(Dispatchers.IO) {
            when (mutation) {
                PerDiemOverrideMutation.UPSERT_INCLUDED ->
                    dao.upsert(PerDiemDayOverrideEntity(date = isoDate, included = true))
                PerDiemOverrideMutation.UPSERT_EXCLUDED ->
                    dao.upsert(PerDiemDayOverrideEntity(date = isoDate, included = false))
                PerDiemOverrideMutation.DELETE ->
                    dao.deleteByDate(isoDate)
            }
        }
    }
}
