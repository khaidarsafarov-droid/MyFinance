package com.truckerload.presentation.auth

import android.content.Context
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.truckerload.R
import com.truckerload.data.preferences.BiometricUnlockStore
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlTextButton as TextButton
import com.truckerload.presentation.theme.LocalTruckColors

@Composable
fun BiometricOptInDialog(
    onDismiss: () -> Unit,
    onEnabled: () -> Unit = onDismiss,
) {
    val tc = LocalTruckColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.biometric_opt_in_title), color = tc.TextPrimary) },
        text = { Text(stringResource(R.string.biometric_opt_in_message), color = tc.TextSecondary) },
        confirmButton = {
            Button(onClick = onEnabled) {
                Text(stringResource(R.string.biometric_opt_in_enable))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.biometric_opt_in_skip))
            }
        },
    )
}

fun shouldOfferBiometricUnlock(context: Context): Boolean {
    if (!canUseBiometricUnlock(context)) return false
    return !BiometricUnlockStore(context).isEnabled()
}

fun enableBiometricUnlock(context: Context) {
    if (!canUseBiometricUnlock(context)) return
    BiometricUnlockStore(context).setEnabled(true)
    BiometricSession.unlockedThisProcess = true
}
