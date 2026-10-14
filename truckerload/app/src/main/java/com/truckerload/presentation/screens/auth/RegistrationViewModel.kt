package com.truckerload.presentation.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.account.RegistrationService
import com.truckerload.domain.account.DriverRole
import com.truckerload.domain.account.RegistrationProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicBoolean
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
    private val _step = MutableStateFlow(0)
    val step: StateFlow<Int> = _step.asStateFlow()
    private val submitting = AtomicBoolean(false)

    fun goToPreviousStep(): RegistrationBackAction {
        val action = RegistrationNavigation.onBack(_step.value)
        if (action == RegistrationBackAction.Previous) {
            _step.value = RegistrationNavigation.previousStep(_step.value)
        }
        return action
    }

    fun needsRequiredOnboarding(): Boolean = registration.needsRequiredOnboarding()

    fun completeBasic(displayName: String, role: DriverRole, phone: String?, onDone: (Result<Unit>) -> Unit) {
        if (!submitting.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                val result = registration.completeBasicProfile(displayName, role, phone)
                _progress.value = registration.progress()
                onDone(result)
            } finally {
                submitting.set(false)
            }
        }
    }
}
