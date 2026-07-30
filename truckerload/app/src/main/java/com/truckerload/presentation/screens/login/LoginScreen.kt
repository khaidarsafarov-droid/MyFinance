package com.truckerload.presentation.screens.login

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckerload.R
import com.truckerload.data.repository.AuthRepository
import com.truckerload.presentation.auth.GoogleAuthCallbacks
import com.truckerload.presentation.auth.rememberGoogleSignInLauncher
import com.truckerload.presentation.components.GoogleSignInButton
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.di.LocalAuthCredentialsStore
import com.truckerload.presentation.di.LocalAuthStore
import com.truckerload.presentation.di.LocalUserProfileStore
import com.truckerload.presentation.theme.BentoGlassScreenBackground
import com.truckerload.presentation.theme.LocalTruckColors

@Composable
fun LoginScreen(
    onCreateAccount: () -> Unit,
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val authStore = LocalAuthStore.current
    val userProfileStore = LocalUserProfileStore.current
    val credentialsStore = LocalAuthCredentialsStore.current
    val authRepository = remember(context, authStore, userProfileStore, credentialsStore) {
        AuthRepository(
            appContext = context.applicationContext,
            authStore = authStore,
            userProfileStore = userProfileStore,
            credentialsStore = credentialsStore,
        )
    }
    val viewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.Factory(authRepository, context.applicationContext),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val emailFocus = remember { FocusRequester() }
    val googleSignIn = rememberGoogleSignInLauncher(
        GoogleAuthCallbacks(onBusy = viewModel::setLoading),
    )

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AuthUiEvent.Toast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    BentoGlassScreenBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(48.dp))
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
                    Spacer(modifier = Modifier.height(32.dp))

                    GoogleSignInButton(
                        onClick = { googleSignIn.launch() },
                        enabled = !uiState.isLoading,
                        loading = uiState.isLoading && !uiState.showEmailFields,
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    LoginOrDivider()
                    Spacer(modifier = Modifier.height(20.dp))

                    if (!uiState.showEmailFields) {
                        Button(
                            onClick = { viewModel.showEmailFields() },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            enabled = !uiState.isLoading,
                        ) { Text(stringResource(R.string.login_with_email)) }
                    } else {
                        LoginEmailForm(
                            uiState = uiState,
                            emailFocus = emailFocus,
                            onEmailChange = viewModel::onEmailChange,
                            onPasswordChange = viewModel::onPasswordChange,
                            onSubmit = viewModel::signInWithEmail,
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
                        .clickable(enabled = !uiState.isLoading) { onCreateAccount() },
                )
            }
            if (uiState.isLoading && !uiState.showEmailFields) {
                LoginLoadingOverlay()
            }
        }
    }
}
