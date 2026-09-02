package com.truckerload.presentation.screens.login

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.truckerload.presentation.auth.BiometricOptInDialog
import com.truckerload.presentation.auth.enableBiometricUnlock
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.BuildConfig
import com.truckerload.presentation.components.AppBrandLogo
import com.truckerload.presentation.components.FillViewportScrollColumn
import com.truckerload.presentation.components.GoogleSignInButton
import com.truckerload.presentation.screens.auth.AuthUiEvent
import com.truckerload.presentation.screens.auth.AuthViewModel
import com.truckerload.presentation.theme.BentoGlassScreenBackground
import com.truckerload.presentation.theme.LocalTruckColors

@Composable
fun LoginScreen(
    onCreateAccount: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val emailFocus = remember { FocusRequester() }
    val launchLegacyGoogle = rememberLegacyGoogleSignInLaunch(viewModel)
    var showBiometricOffer by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AuthUiEvent.ShowToast ->
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                AuthUiEvent.LaunchLegacyGoogleSignIn -> launchLegacyGoogle()
                AuthUiEvent.ShowBiometricOfferDialog -> showBiometricOffer = true
            }
        }
    }

    BentoGlassScreenBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            FillViewportScrollColumn(
                contentPadding = PaddingValues(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(36.dp))
                    AppBrandLogo(size = 112.dp)
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.login_title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = tc.TextPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.login_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = tc.TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                    uiState.errorMessage?.let { message ->
                        if (!uiState.showEmailFields) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = tc.AccentExpense,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))

                    GoogleSignInButton(
                        onClick = { viewModel.onGoogleSignInClick(context) },
                        enabled = !uiState.isLoading,
                        loading = uiState.isLoading && !uiState.showEmailFields,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.login_google_sync_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = tc.TextSecondary,
                        textAlign = TextAlign.Center,
                    )

                    if (BuildConfig.LOCAL_ONLY_MODE) {
                        Spacer(modifier = Modifier.height(20.dp))
                        OutlinedButton(
                            onClick = { viewModel.onAnonymousSignIn() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            enabled = !uiState.isLoading,
                        ) { Text(stringResource(R.string.login_continue_offline)) }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.login_continue_offline_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = tc.TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = tc.TextSecondary.copy(alpha = 0.35f),
                        )
                        Text(
                            text = stringResource(R.string.login_or_divider),
                            style = MaterialTheme.typography.bodySmall,
                            color = tc.TextSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = tc.TextSecondary.copy(alpha = 0.35f),
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    if (!uiState.showEmailFields) {
                        OutlinedButton(
                            onClick = { viewModel.showEmailFields() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            enabled = !uiState.isLoading,
                        ) { Text(stringResource(R.string.login_with_email)) }
                    } else {
                        LoginEmailFields(
                            uiState = uiState,
                            emailFocus = emailFocus,
                            onEmailChange = viewModel::onEmailChanged,
                            onPasswordChange = viewModel::onPasswordChanged,
                            onSubmit = { viewModel.onEmailSubmit(context) },
                        )
                    }
                }
                Text(
                    text = buildAnnotatedString {
                        append(stringResource(R.string.login_no_account_prefix))
                        withStyle(
                            SpanStyle(color = tc.AccentPrimary, fontWeight = FontWeight.Medium),
                        ) { append(stringResource(R.string.login_create_account)) }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(bottom = 32.dp)
                        .clickable(
                            enabled = !uiState.isLoading,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onCreateAccount() },
                )
            }
            if (uiState.isLoading && !uiState.showEmailFields) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = tc.AccentPrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.login_checking),
                            style = MaterialTheme.typography.bodyMedium,
                            color = tc.TextSecondary,
                        )
                    }
                }
            }
        }
    }
    if (showBiometricOffer) {
        BiometricOptInDialog(
            onDismiss = { showBiometricOffer = false },
            onEnabled = {
                enableBiometricUnlock(context)
                Toast.makeText(
                    context,
                    context.getString(R.string.biometric_enabled_toast),
                    Toast.LENGTH_SHORT,
                ).show()
                showBiometricOffer = false
            },
        )
    }
}
