package com.truckerload.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.data.preferences.AuthProvider
import com.truckerload.data.preferences.BiometricUnlockStore
import com.truckerload.presentation.auth.canUseBiometricUnlock
import com.truckerload.presentation.di.LocalAuthStore
import com.truckerload.presentation.theme.AppSwitchDefaults
import com.truckerload.presentation.theme.BentoGlassSection
import com.truckerload.presentation.theme.LocalTruckColors

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
    val tc = LocalTruckColors.current

    BentoGlassSection(
        title = stringResource(R.string.settings_biometric_title),
        subtitle = stringResource(R.string.settings_biometric_desc),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    Icons.Default.Fingerprint,
                    contentDescription = stringResource(R.string.settings_biometric_title),
                    tint = tc.AccentPrimary,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = stringResource(R.string.settings_biometric_unlock_label),
                    color = tc.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = { value ->
                    enabled = value
                    biometricStore.setEnabled(value)
                },
                colors = AppSwitchDefaults.colors(),
            )
        }
    }
}
