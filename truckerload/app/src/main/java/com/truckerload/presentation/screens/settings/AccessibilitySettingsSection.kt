package com.truckerload.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MotionPhotosOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.presentation.di.LocalSettingsDataStore
import com.truckerload.presentation.theme.AppSwitchDefaults
import com.truckerload.presentation.theme.BentoGlassSection
import com.truckerload.presentation.theme.LocalTruckColors
import kotlinx.coroutines.launch

@Composable
fun AccessibilitySettingsSection() {
    val settingsDataStore = LocalSettingsDataStore.current
    val tc = LocalTruckColors.current
    val scope = rememberCoroutineScope()
    val reduceMotion by settingsDataStore.reduceMotion.collectAsStateWithLifecycle(initialValue = false)

    BentoGlassSection(
        title = stringResource(R.string.settings_accessibility_title),
        subtitle = stringResource(R.string.settings_accessibility_desc),
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
                    Icons.Outlined.MotionPhotosOff,
                    contentDescription = null,
                    tint = tc.AccentPrimary,
                    modifier = Modifier.size(22.dp),
                )
                Text(stringResource(R.string.settings_reduce_motion_title), color = tc.TextPrimary)
            }
            Switch(
                checked = reduceMotion,
                onCheckedChange = { enabled ->
                    scope.launch { settingsDataStore.saveReduceMotion(enabled) }
                },
                colors = AppSwitchDefaults.colors(),
            )
        }
        Text(
            text = stringResource(R.string.settings_reduce_motion_hint),
            color = tc.TextSecondary,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
        )
    }
}
