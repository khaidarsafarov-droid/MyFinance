package com.truckerload.presentation.screens.auth

import androidx.lifecycle.ViewModel
import com.truckerload.R
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.LocalDeviceOnboarding
import com.truckerload.data.preferences.UserProfileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class FirstRunUiState(
    val givenName: String = "",
    val familyName: String = "",
    val isSaving: Boolean = false,
    val errorMessageRes: Int? = null,
)

@HiltViewModel
class FirstRunViewModel @Inject constructor(
    private val authStore: AuthStore,
    private val userProfileStore: UserProfileStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FirstRunUiState())
    val uiState: StateFlow<FirstRunUiState> = _uiState.asStateFlow()

    fun onGivenNameChanged(value: String) {
        _uiState.update { it.copy(givenName = value, errorMessageRes = null) }
    }

    fun onFamilyNameChanged(value: String) {
        _uiState.update { it.copy(familyName = value, errorMessageRes = null) }
    }

    fun onSave() {
        val state = _uiState.value
        if (state.isSaving) return
        if (!LocalDeviceOnboarding.namesAreValid(state.givenName, state.familyName)) {
            _uiState.update { it.copy(errorMessageRes = R.string.first_run_name_required) }
            return
        }
        _uiState.update { it.copy(isSaving = true, errorMessageRes = null) }
        runCatching {
            LocalDeviceOnboarding.complete(
                authStore = authStore,
                userProfileStore = userProfileStore,
                givenName = state.givenName,
                familyName = state.familyName,
            )
        }.onFailure {
            _uiState.update {
                it.copy(
                    isSaving = false,
                    errorMessageRes = R.string.first_run_name_required,
                )
            }
        }
    }
}
