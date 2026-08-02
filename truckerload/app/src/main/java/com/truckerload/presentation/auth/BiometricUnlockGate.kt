package com.truckerload.presentation.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.truckerload.R
import com.truckerload.data.preferences.BiometricUnlockStore
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlTextButton as TextButton
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors

fun canUseBiometricUnlock(context: Context): Boolean {
    val manager = BiometricManager.from(context)
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
        BiometricManager.Authenticators.BIOMETRIC_WEAK
    return manager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
}

/**
 * Process-scoped unlock gate for email accounts with biometric enabled.
 * Survives composition; resets on process death (user must unlock again).
 */
object BiometricSession {
    @Volatile
    var unlockedThisProcess: Boolean = false
}

@Composable
fun BiometricUnlockGate(
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    var unlocked by remember { mutableStateOf(BiometricSession.unlockedThisProcess) }
    if (!enabled || unlocked) {
        content()
        return
    }
    val context = LocalContext.current
    val tc = LocalTruckColors.current
    var error by remember { mutableStateOf<String?>(null) }
    val activity = context as? FragmentActivity

    fun markUnlocked() {
        BiometricSession.unlockedThisProcess = true
        unlocked = true
        error = null
    }

    fun launchPrompt() {
        if (activity == null) {
            markUnlocked()
            return
        }
        if (!canUseBiometricUnlock(context)) {
            markUnlocked()
            return
        }
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    markUnlocked()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        error = context.getString(R.string.biometric_unlock_cancelled)
                    } else {
                        error = errString.toString()
                    }
                }

                override fun onAuthenticationFailed() {
                    error = context.getString(R.string.biometric_unlock_failed)
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.biometric_unlock_title))
            .setSubtitle(context.getString(R.string.biometric_unlock_subtitle))
            .setNegativeButtonText(context.getString(R.string.common_cancel))
            .build()
        prompt.authenticate(info)
    }

    LaunchedEffect(Unit) { launchPrompt() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BentoGlassTheme.ScreenBackground),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                text = stringResource(R.string.biometric_unlock_title),
                style = MaterialTheme.typography.titleLarge,
                color = tc.TextPrimary,
            )
            Text(
                text = stringResource(R.string.biometric_unlock_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = tc.TextSecondary,
            )
            error?.let {
                Text(text = it, color = tc.AccentExpense, style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = { launchPrompt() }) {
                Text(stringResource(R.string.biometric_unlock_retry))
            }
            TextButton(onClick = { markUnlocked() }) {
                Text(stringResource(R.string.biometric_unlock_skip))
            }
        }
    }
}
