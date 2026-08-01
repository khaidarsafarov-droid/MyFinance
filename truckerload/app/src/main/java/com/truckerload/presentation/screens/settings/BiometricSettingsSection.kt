package com.truckerload.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.data.preferences.AuthProvider
import com.truckerload.data.preferences.BiometricUnlockStore
import com.truckerload.presentation.auth.canUseBiometricUnlock
import com.truckerload.presentation.di.LocalAuthStore
import com.truckerload.presentation.theme.BentoGlassSection
import com.truckerload.presentation.theme.LocalTruckColors

@Composable
fun BiometricSettingsSection(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val authStore = LocalAuthStore.current
    val tc = LocalTruckColors.current
    val isEmailAccount = authStore.authProvider() == AuthProvider.EMAIL
    val hardwareAvailable = remember { canUseBiometricUnlock(context) }
    if (!isEmailAccount || !hardwareAvailable) return

    val store = remember { BiometricUnlockStore(context) }
    var enabled by remember { mutableStateOf(store.isEnabled()) }

    BentoGlassSection(
        title = stringResource(R.string.settings_biometric_title),
        subtitle = stringResource(R.string.settings_biometric_desc),
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsToggleRow(
                icon = {
                    Icon(
                        Icons.Default.Fingerprint,
                        contentDescription = stringResource(R.string.settings_biometric_title),
                        tint = tc.AccentPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                },
                label = stringResource(R.string.settings_biometric_toggle),
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                    store.setEnabled(it)
                },
            )
            Text(
                text = stringResource(R.string.settings_biometric_hint),
                style = MaterialTheme.typography.labelSmall,
                color = tc.TextSecondary,
            )
        }
    }
}
