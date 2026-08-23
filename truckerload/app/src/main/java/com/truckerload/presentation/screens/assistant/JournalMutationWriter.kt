package com.truckerload.presentation.screens.assistant

import com.truckerload.data.preferences.LastUsedDefaultsStore
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.PaycheckRepository
import javax.inject.Inject

/** Persists a confirmed diesel / paycheck draft through the same repos as the add forms. */
class JournalMutationWriter @Inject constructor(
    private val dieselRepository: DieselRepository,
    private val paycheckRepository: PaycheckRepository,
    private val lastUsedDefaultsStore: LastUsedDefaultsStore,
) {
    suspend fun save(mutation: PendingAssistantMutation) {
        when (mutation) {
            is PendingAssistantMutation.DieselDraft -> {
                dieselRepository.insertDiesel(mutation.diesel)
                lastUsedDefaultsStore.saveDieselAmount(mutation.diesel.totalAmount)
            }
            is PendingAssistantMutation.PaycheckDraft -> {
                paycheckRepository.insertPaycheck(mutation.paycheck)
                lastUsedDefaultsStore.savePaycheckAmount(mutation.paycheck.netAmount)
            }
        }
    }
}
