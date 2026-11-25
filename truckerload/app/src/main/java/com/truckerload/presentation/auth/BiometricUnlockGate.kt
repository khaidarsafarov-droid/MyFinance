package com.truckerload.presentation.auth

import android.content.Context
import androidx.biometric.BiometricManager
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.truckerload.R
import com.truckerload.data.preferences.BiometricUnlockStore
import com.truckerload.presentation.components.TlButton as Button
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
fun BiometricUnlockGate(enabled: Boolean, content: @Composable () -> Unit) {
    var unlocked by remember { mutableStateOf(BiometricSession.unlockedThisProcess) }
    if (!enabled || unlocked) {
        content()
        return
    }
    val context = LocalContext.current
    val resources = LocalResources.current
    var error by remember { mutableStateOf<String?>(null) }
    val activity = context as? FragmentActivity

    val controller = remember(activity) {
        BiometricUnlockController(
            authenticator = activity?.let {
                AndroidBiometricAuthenticator(
                    activity = it,
                    title = resources.getString(R.string.biometric_unlock_title),
                    subtitle = resources.getString(R.string.biometric_unlock_subtitle),
                    negativeText = resources.getString(R.string.common_cancel),
                )
            },
            messages = object : BiometricUnlockMessages {
                override fun failed() = context.getString(R.string.biometric_unlock_failed)

                override fun cancelled() = context.getString(R.string.biometric_unlock_cancelled)
            },
            onUnlocked = {
                BiometricSession.unlockedThisProcess = true
                unlocked = true
                error = null
            },
            onError = { error = it },
        )
    }

    fun launchPrompt() = controller.launch()

    LaunchedEffect(Unit) { launchPrompt() }

    BiometricLockedScreen(error = error, onRetry = { launchPrompt() })
}

@Composable
private fun BiometricLockedScreen(error: String?, onRetry: () -> Unit) {
    val tc = LocalTruckColors.current
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
            Button(onClick = onRetry) {
                Text(stringResource(R.string.biometric_unlock_retry))
            }
            // FIX: skip must not unlock — that bypassed biometric for anyone with the phone
        }
    }
}
