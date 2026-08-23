package com.truckerload.presentation.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.account.DriverProfessionalRepository
import com.truckerload.data.repository.account.RegistrationService
import com.truckerload.di.UserId
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
    @UserId private val userId: String,
    private val registration: RegistrationService,
    private val professional: DriverProfessionalRepository,
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

    fun completeProfessional(
        companyName: String?,
        cdlNumber: String?,
        vehicleType: String,
        primaryRegion: String,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            registration.completeProfessional(
                companyName = companyName,
                cdlNumber = cdlNumber,
                cdlDocumentUrl = null,
                vehicleType = vehicleType,
                primaryRegion = primaryRegion,
            )
            _progress.value = registration.progress()
            onDone()
        }
    }

    fun skipProfessional(onDone: () -> Unit) {
        viewModelScope.launch {
            registration.skipProfessional()
            _progress.value = registration.progress()
            onDone()
        }
    }

    suspend fun ownCdl(): String? = professional.getOwn(userId)?.cdlNumber
}
