package com.truckerload.domain.store

import com.truckerload.domain.model.Paycheck

/** Paycheck journal used when week labels are rewritten or duplicates are removed. */
interface PaycheckStore {
    suspend fun getAllPaychecksOnce(): List<Paycheck>

    suspend fun replaceReportingWeeks(updated: List<Paycheck>)

    suspend fun deletePaycheck(id: Int)
}
