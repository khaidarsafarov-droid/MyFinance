package com.truckerload.presentation.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.truckerload.R
import com.truckerload.data.preferences.AuthProvider
import com.truckerload.data.preferences.BiometricUnlockStore
import com.truckerload.presentation.auth.canUseBiometricUnlock
import com.truckerload.presentation.di.LocalAuthStore
import com.truckerload.presentation.icons.AppIcons

@Composable
fun BiometricSettingsSection(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val authStore = LocalAuthStore.current
    val emailAccount = authStore.authProvider() == AuthProvider.EMAIL
    val hardwareOk = remember { canUseBiometricUnlock(context) }
    if (!emailAccount || !hardwareOk) return

    val biometricStore = remember { BiometricUnlockStore(context) }
    var enabled by remember { mutableStateOf(biometricStore.isEnabled()) }

    SettingsSwitchRow(
        icon = AppIcons.Fingerprint,
        title = stringResource(R.string.settings_biometric_unlock_label),
        subtitle = stringResource(R.string.settings_biometric_subtitle),
        checked = enabled,
        onCheckedChange = { value ->
            enabled = value
            biometricStore.setEnabled(value)
        },
        modifier = modifier,
    )
}
