package com.truckerload.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.di.LocalSettingsDataStore
import com.truckerload.presentation.theme.AppSwitchDefaults
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.BentoGlassSection
import com.truckerload.presentation.theme.LocalTruckColors
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.launch

@Composable
fun ParserSettings(
    modifier: Modifier = Modifier,
) {
    val settingsDataStore = LocalSettingsDataStore.current
    val scope = rememberCoroutineScope()
    val tc = LocalTruckColors.current

    val autoUpdate by settingsDataStore.parserAutoUpdate.collectAsState(initial = true)
    val priceThreshold by settingsDataStore.parserPriceThreshold.collectAsState(initial = 1.0)
    var thresholdInput by remember(priceThreshold) {
        mutableStateOf(priceThreshold.toString())
    }

    BentoGlassSection(
        title = stringResource(R.string.parser_settings),
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.auto_update),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tc.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = autoUpdate,
                    onCheckedChange = { enabled ->
                        scope.launch { settingsDataStore.saveParserAutoUpdate(enabled) }
                    },
                    colors = AppSwitchDefaults.colors(),
                )
            }

            Text(
                text = stringResource(R.string.price_threshold_desc),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
            )

            OutlinedTextField(
                value = thresholdInput,
                onValueChange = { value ->
                    thresholdInput = value
                    value.toDoubleOrNull()?.let { parsed ->
                        scope.launch { settingsDataStore.saveParserPriceThreshold(parsed) }
                    }
                },
                label = { Text(stringResource(R.string.price_threshold)) },
                suffix = { Text("%") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = AppTextFieldDefaults.outlined(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
