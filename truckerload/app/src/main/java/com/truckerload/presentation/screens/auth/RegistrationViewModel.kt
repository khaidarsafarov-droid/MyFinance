package com.truckerload.presentation.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.account.RegistrationService
import com.truckerload.domain.account.DriverRole
import com.truckerload.domain.account.RegistrationProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val registration: RegistrationService,
) : ViewModel() {

    private val _progress = MutableStateFlow(registration.progress())
    val progress: StateFlow<RegistrationProgress> = _progress.asStateFlow()

    fun needsRequiredOnboarding(): Boolean = registration.needsRequiredOnboarding()

    fun completeBasic(displayName: String, role: DriverRole, phone: String?, onDone: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = registration.completeBasicProfile(displayName, role, phone)
            _progress.value = registration.progress()
            onDone(result)
        }
    }
}
