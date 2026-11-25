package com.truckerload.presentation.screens.settings

import androidx.lifecycle.ViewModel
import com.truckerload.data.repository.account.AccountDeletionService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DeleteAccountViewModel @Inject constructor(private val deletion: AccountDeletionService) : ViewModel() {
    suspend fun deleteAccountAndSignOut(): Result<Unit> = runCatching { deletion.deleteAccountAndSignOut() }
}
