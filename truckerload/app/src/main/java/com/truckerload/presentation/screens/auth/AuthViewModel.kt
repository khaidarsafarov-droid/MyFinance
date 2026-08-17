package com.truckerload.presentation.screens.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.R
import com.truckerload.data.repository.auth.AuthRepository
import com.truckerload.data.repository.auth.AuthSignInResult
import com.truckerload.data.repository.auth.GoogleAuthCredential
import com.truckerload.data.repository.auth.GoogleTokenRequestResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val showEmailFields: Boolean = false,
    val email: String = "",
    val password: String = "",
    val errorMessage: String? = null,
)

sealed interface AuthUiEvent {
    data class ShowToast(val message: String) : AuthUiEvent
    data object LaunchLegacyGoogleSignIn : AuthUiEvent
    data object ShowBiometricOfferDialog : AuthUiEvent
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AuthUiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<AuthUiEvent> = _events.asSharedFlow()

    fun showEmailFields() {
        _uiState.update { it.copy(showEmailFields = true, errorMessage = null) }
    }

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onGoogleSignInClick(activityContext: Context) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                when (val tokenResult = authRepository.requestGoogleIdToken(activityContext)) {
                    is GoogleTokenRequestResult.Token -> {
                        completeGoogle(GoogleAuthCredential(idToken = tokenResult.idToken))
                    }
                    GoogleTokenRequestResult.Cancelled -> {
                        _events.emit(
                            AuthUiEvent.ShowToast(activityContext.getString(R.string.login_google_cancelled)),
                        )
                        _uiState.update { it.copy(isLoading = false) }
                    }
                    GoogleTokenRequestResult.FallBackToLegacy -> {
                        _events.emit(AuthUiEvent.LaunchLegacyGoogleSignIn)
                        // Keep isLoading=true until legacy result arrives
                    }
                }
            } catch (e: Exception) {
                _events.emit(
                    AuthUiEvent.ShowToast(
                        activityContext.getString(
                            R.string.login_google_error,
                            e.message ?: e.toString(),
                        ),
                    ),
                )
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onLegacyGoogleCancelled() {
        _uiState.update { it.copy(isLoading = false) }
    }

    fun onLegacyGoogleError(message: String) {
        viewModelScope.launch {
            _events.emit(AuthUiEvent.ShowToast(message))
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onLegacyGoogleAccount(credential: GoogleAuthCredential) {
        viewModelScope.launch {
            completeGoogle(credential)
        }
    }

    fun onEmailSubmit(context: Context) {
        if (_uiState.value.isLoading) return
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.signInWithEmail(state.email, state.password)
            result.fold(
                onSuccess = { applySuccess(it) },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.message
                                ?: context.getString(R.string.auth_error_login_invalid),
                        )
                    }
                },
            )
        }
    }

    /** Offline / local_dev session (no UI button on LoginScreen; kept for API parity). */
    fun onAnonymousSignIn() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.signInAnonymously().fold(
                onSuccess = { applySuccess(it) },
                onFailure = { err ->
                    _events.emit(AuthUiEvent.ShowToast(err.message ?: "Anonymous sign-in failed"))
                    _uiState.update { it.copy(isLoading = false) }
                },
            )
        }
    }

    private suspend fun completeGoogle(credential: GoogleAuthCredential) {
        val result = authRepository.signInWithGoogle(credential)
        result.fold(
            onSuccess = { applySuccess(it) },
            onFailure = { err ->
                _events.emit(
                    AuthUiEvent.ShowToast(
                        err.message ?: "Google sign-in failed",
                    ),
                )
                _uiState.update { it.copy(isLoading = false) }
            },
        )
    }

    private suspend fun applySuccess(result: AuthSignInResult) {
        result.toastMessages.forEach { _events.emit(AuthUiEvent.ShowToast(it)) }
        if (result.biometricEnabled) {
            _events.emit(AuthUiEvent.ShowBiometricOfferDialog)
        }
        _uiState.update { it.copy(isLoading = false, errorMessage = null) }
    }
}
