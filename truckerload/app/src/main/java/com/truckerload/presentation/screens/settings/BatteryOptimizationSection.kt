package com.truckerload.presentation.screens.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.theme.TruckColorPalette
import com.truckerload.utils.BatteryOptimizationHelper

@Composable
internal fun BatteryOptimizationContent(
    context: android.content.Context,
    tc: TruckColorPalette
) {
    var ignoring by remember { mutableStateOf(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)) }

    if (!ignoring) {
        Button(
            onClick = {
                BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context)
                ignoring = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
            },
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(stringResource(R.string.settings_battery_button))
        }
    } else {
        Text(
            text = stringResource(R.string.settings_battery_ok),
            style = MaterialTheme.typography.bodySmall,
            color = tc.AccentProfit
        )
    }
}
