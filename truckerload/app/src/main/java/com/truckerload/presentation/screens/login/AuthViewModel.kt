package com.truckerload.presentation.screens.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.R
import com.truckerload.data.repository.AuthActionResult
import com.truckerload.data.repository.AuthRepository
import com.truckerload.presentation.auth.offerBiometricAfterEmailLogin
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val showEmailFields: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)

sealed interface AuthUiEvent {
    data class Toast(val message: String) : AuthUiEvent
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AuthUiEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<AuthUiEvent> = _events.asSharedFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun showEmailFields() {
        _uiState.update { it.copy(showEmailFields = true) }
    }

    fun setLoading(loading: Boolean) {
        _uiState.update { it.copy(isLoading = loading) }
    }

    fun signInWithEmail() {
        val state = _uiState.value
        if (state.isLoading) return
        _uiState.update { it.copy(error = null, isLoading = true) }
        viewModelScope.launch {
            val result = authRepository.signInWithEmail(state.email, state.password)
            applyResult(result)
        }
    }

    private suspend fun applyResult(result: AuthActionResult) {
        result.toastMessage?.let { _events.emit(AuthUiEvent.Toast(it)) }
        if (result.succeeded && result.offerBiometric) {
            if (offerBiometricAfterEmailLogin(appContext)) {
                _events.emit(AuthUiEvent.Toast(appContext.getString(R.string.biometric_enabled_toast)))
            }
        }
        _uiState.update {
            it.copy(isLoading = false, error = result.fieldError)
        }
    }
}
