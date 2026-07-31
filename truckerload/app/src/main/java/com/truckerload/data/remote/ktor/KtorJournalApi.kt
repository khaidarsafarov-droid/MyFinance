package com.truckerload.data.remote.ktor

import com.truckerload.data.backup.BackupData
import com.truckerload.data.sync.AccountCloudSnapshot
import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Paycheck
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Journal (paycheck + diesel) entries via the account snapshot API.
 * UI continues to read Room; this API is for cloud pull/push only.
 */
@Singleton
class KtorJournalApi @Inject constructor(
    private val loadApi: KtorLoadApi,
) {
    data class JournalEntries(
        val paychecks: List<Paycheck> = emptyList(),
        val diesel: List<Diesel> = emptyList(),
        val snapshotUpdatedAt: Long = 0L,
    )

    suspend fun getJournal(accountId: String): JournalEntries {
        val snapshot = loadApi.fetchSnapshot(accountId) ?: return JournalEntries()
        return JournalEntries(
            paychecks = snapshot.backup.paychecks,
            diesel = snapshot.backup.diesel,
            snapshotUpdatedAt = snapshot.updatedAt,
        )
    }

    suspend fun putJournal(
        accountId: String,
        paychecks: List<Paycheck>,
        diesel: List<Diesel>,
        existingLoads: List<Load> = emptyList(),
    ): AccountCloudSnapshot {
        val existing = loadApi.fetchSnapshot(accountId)
        val now = System.currentTimeMillis()
        val backup = BackupData(
            loads = existingLoads.ifEmpty { existing?.backup?.loads.orEmpty() },
            paychecks = paychecks,
            diesel = diesel,
            exportedAt = now,
        )
        val snapshot = AccountCloudSnapshot(
            accountId = accountId,
            updatedAt = maxOf(now, (existing?.updatedAt ?: 0L) + 1L),
            backup = backup,
            driverProfileJson = existing?.driverProfileJson,
        )
        return loadApi.syncLoads(snapshot)
    }
}
