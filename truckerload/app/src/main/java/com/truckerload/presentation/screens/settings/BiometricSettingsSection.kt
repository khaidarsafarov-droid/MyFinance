package com.truckerload.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
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
import com.truckerload.data.preferences.BiometricUnlockStore
import com.truckerload.presentation.auth.canUseBiometricUnlock
import com.truckerload.presentation.theme.AppSwitchDefaults
import com.truckerload.presentation.theme.BentoGlassSection
import com.truckerload.presentation.theme.LocalTruckColors

@Composable
fun BiometricSettingsSection() {
    val context = LocalContext.current
    val tc = LocalTruckColors.current
    val store = remember(context) { BiometricUnlockStore(context) }
    val available = remember(context) { canUseBiometricUnlock(context) }
    if (!available) return

    var enabled by remember { mutableStateOf(store.isEnabled()) }

    BentoGlassSection(
        title = stringResource(R.string.settings_biometric_title),
        subtitle = stringResource(R.string.settings_biometric_desc),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    Icons.Outlined.Fingerprint,
                    contentDescription = null,
                    tint = tc.AccentPrimary,
                    modifier = Modifier.size(22.dp),
                )
                Text(stringResource(R.string.settings_biometric_toggle), color = tc.TextPrimary)
            }
            Switch(
                checked = enabled,
                onCheckedChange = { checked ->
                    enabled = checked
                    store.setEnabled(checked)
                },
                colors = AppSwitchDefaults.colors(),
            )
        }
        Text(
            text = stringResource(R.string.settings_biometric_hint),
            color = tc.TextSecondary,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
