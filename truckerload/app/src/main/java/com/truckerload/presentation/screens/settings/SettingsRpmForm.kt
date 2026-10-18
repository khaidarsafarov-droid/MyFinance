package com.truckerload.presentation.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.domain.parser.DecimalInput
import com.truckerload.presentation.components.RpmColorLegend
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.di.LocalRpmThresholdsStore
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.LocalSemanticColors

@Composable
internal fun SettingsRpmForm(modifier: Modifier = Modifier) {
    val store = LocalRpmThresholdsStore.current
    val context = LocalContext.current
    val resources = LocalResources.current
    val semantic = LocalSemanticColors.current
    val thresholds by store.thresholds.collectAsStateWithLifecycle()
    var minInput by remember(thresholds) { mutableStateOf(thresholds.minProfit.toString()) }
    var targetInput by remember(thresholds) { mutableStateOf(thresholds.targetProfit.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    val fieldColors = AppTextFieldDefaults.outlined()

    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_rpm_thresholds_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        RpmColorLegend(compact = true)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = minInput,
                onValueChange = {
                    minInput = it
                    error = null
                },
                label = { Text(stringResource(R.string.settings_red_threshold_short)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = fieldColors,
            )
            OutlinedTextField(
                value = targetInput,
                onValueChange = {
                    targetInput = it
                    error = null
                },
                label = { Text(stringResource(R.string.settings_green_threshold_short)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = fieldColors,
            )
        }
        error?.let { err ->
            Text(
                text = err,
                color = semantic.danger,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Button(
            onClick = {
                val min = DecimalInput.parse(minInput)
                val target = DecimalInput.parse(targetInput)
                when {
                    min == null -> error = resources.getString(R.string.settings_red_threshold_error)
                    target == null -> error = resources.getString(R.string.settings_green_threshold_error)
                    else -> store.save(min, target)
                        .onSuccess {
                            error = null
                            Toast.makeText(
                                context,
                                resources.getString(R.string.settings_saved_toast),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                        .onFailure { error = it.message }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(stringResource(R.string.settings_rpm_save))
        }
    }
}
