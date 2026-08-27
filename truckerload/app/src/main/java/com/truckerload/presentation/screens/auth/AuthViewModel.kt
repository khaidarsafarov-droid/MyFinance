package com.truckerload.presentation.screens.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.R
import com.truckerload.contract.DeviceSlotPolicy
import com.truckerload.data.backup.DriveSyncEligibility
import com.truckerload.data.backup.DriveSyncWorker
import com.truckerload.data.backup.GoogleDriveBackupService
import com.truckerload.data.repository.auth.AuthRepository
import com.truckerload.data.repository.auth.AuthSignInResult
import com.truckerload.data.repository.auth.GoogleAuthCredential
import com.truckerload.data.repository.auth.GoogleTokenRequestResult
import com.truckerload.data.sync.DeviceSlotDenialStore
import com.truckerload.data.sync.DeviceSlotTakenException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.delay
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
    val deviceSlotReplacePrompt: DeviceSlotReplacePrompt? = null,
)

data class DeviceSlotReplacePrompt(
    val formFactor: String,
    val message: String,
)

sealed interface AuthUiEvent {
    data class ShowToast(val message: String) : AuthUiEvent
    data object LaunchLegacyGoogleSignIn : AuthUiEvent
    data object ShowBiometricOfferDialog : AuthUiEvent
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @param:ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AuthUiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<AuthUiEvent> = _events.asSharedFlow()

    private var pendingGoogleCredential: GoogleAuthCredential? = null
    private var pendingEmailLogin: Pair<String, String>? = null

    fun consumeDeviceSlotDenial(context: Context) {
        val message = DeviceSlotDenialStore(context).consume() ?: return
        viewModelScope.launch {
            _events.emit(AuthUiEvent.ShowToast(message))
            _uiState.update { it.copy(errorMessage = message) }
        }
    }

    fun showEmailFields() {
        _uiState.update { it.copy(showEmailFields = true, errorMessage = null) }
    }

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun dismissDeviceSlotReplacePrompt() {
        pendingGoogleCredential = null
        pendingEmailLogin = null
        _uiState.update {
            it.copy(
                deviceSlotReplacePrompt = null,
                isLoading = false,
            )
        }
    }

    fun confirmDeviceSlotReplace() {
        val google = pendingGoogleCredential
        val emailLogin = pendingEmailLogin
        pendingGoogleCredential = null
        pendingEmailLogin = null
        _uiState.update { it.copy(deviceSlotReplacePrompt = null, isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when {
                google != null -> completeGoogle(google, replaceOccupant = true)
                emailLogin != null -> {
                    val (email, password) = emailLogin
                    authRepository.signInWithEmail(email, password, replaceOccupant = true).fold(
                        onSuccess = { applySuccess(it) },
                        onFailure = { err -> handleSignInFailure(err, appContext) },
                    )
                }
            }
        }
    }

    fun onGoogleSignInClick(activityContext: Context) {
        if (_uiState.value.isLoading) return
        pendingGoogleCredential = null
        pendingEmailLogin = null
        _uiState.update { it.copy(isLoading = true, errorMessage = null, deviceSlotReplacePrompt = null) }
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
                        viewModelScope.launch {
                            delay(LEGACY_GOOGLE_LOADING_TIMEOUT_MS)
                            if (_uiState.value.isLoading) {
                                _uiState.update { it.copy(isLoading = false) }
                            }
                        }
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
        pendingGoogleCredential = null
        pendingEmailLogin = null
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, deviceSlotReplacePrompt = null) }
            val result = authRepository.signInWithEmail(state.email, state.password)
            result.fold(
                onSuccess = { applySuccess(it) },
                onFailure = { err -> handleSignInFailure(err, context) },
            )
        }
    }

    /** Offline / local_dev session — shown on [LoginScreen] when [BuildConfig.LOCAL_ONLY_MODE]. */
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

    private suspend fun completeGoogle(credential: GoogleAuthCredential, replaceOccupant: Boolean = false) {
        val result = authRepository.signInWithGoogle(credential, replaceOccupant)
        result.fold(
            onSuccess = { signIn ->
                GoogleDriveBackupService.syncLinkedAccountFromGoogle(appContext)
                if (DriveSyncEligibility.shouldEnqueuePeriodic(signIn.user.userId)) {
                    DriveSyncWorker.enqueuePeriodic(appContext)
                }
                val toasts = if (GoogleDriveBackupService.isDriveScopeGranted(appContext)) {
                    signIn.toastMessages
                } else {
                    signIn.toastMessages + appContext.getString(R.string.login_google_drive_cta)
                }
                applySuccess(signIn.copy(toastMessages = toasts))
            },
            onFailure = { err -> handleSignInFailure(err, appContext, credential) },
        )
    }

    private suspend fun handleSignInFailure(
        err: Throwable,
        context: Context,
        googleCredential: GoogleAuthCredential? = null,
    ) {
        val slotTaken = err as? DeviceSlotTakenException
        if (slotTaken != null) {
            if (googleCredential != null) {
                pendingGoogleCredential = googleCredential
                pendingEmailLogin = null
            } else {
                pendingEmailLogin = _uiState.value.email to _uiState.value.password
                pendingGoogleCredential = null
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = null,
                    deviceSlotReplacePrompt = DeviceSlotReplacePrompt(
                        formFactor = slotTaken.formFactor,
                        message = slotTaken.message.orEmpty(),
                    ),
                )
            }
            return
        }
        _events.emit(
            AuthUiEvent.ShowToast(
                err.message ?: context.getString(R.string.auth_error_login_invalid),
            ),
        )
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = err.message ?: context.getString(R.string.auth_error_login_invalid),
            )
        }
    }

    private suspend fun applySuccess(result: AuthSignInResult) {
        result.toastMessages.forEach { _events.emit(AuthUiEvent.ShowToast(it)) }
        if (result.biometricEnabled) {
            _events.emit(AuthUiEvent.ShowBiometricOfferDialog)
        }
        pendingGoogleCredential = null
        pendingEmailLogin = null
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = null,
                deviceSlotReplacePrompt = null,
            )
        }
    }

    companion object {
        private const val LEGACY_GOOGLE_LOADING_TIMEOUT_MS = 30_000L
    }
}

fun deviceSlotReplaceTitleRes(formFactor: String): Int =
    if (formFactor == DeviceSlotPolicy.TABLET) {
        R.string.auth_device_replace_title_tablet
    } else {
        R.string.auth_device_replace_title_phone
    }

fun deviceSlotReplaceBodyRes(formFactor: String): Int =
    if (formFactor == DeviceSlotPolicy.TABLET) {
        R.string.auth_device_replace_body_tablet
    } else {
        R.string.auth_device_replace_body_phone
    }
