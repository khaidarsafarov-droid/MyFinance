package com.truckerload.presentation.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.model.DeadheadMiles
import com.truckerload.domain.model.Load
import com.truckerload.domain.parser.DecimalInput
import com.truckerload.domain.units.DisplayUnits
import com.truckerload.domain.units.MeasurementUnits
import com.truckerload.presentation.components.TlOutlinedButton
import com.truckerload.presentation.components.dialogAboveIme
import com.truckerload.presentation.components.dialogBodyScroll
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.LocalTruckColors
import java.util.Locale

@Composable
internal fun DeadheadSection(
    load: Load,
    onSave: (Double) -> Unit,
) {
    val tc = LocalTruckColors.current
    var showEditor by rememberSaveable { mutableStateOf(false) }
    val deadhead = load.deadheadMiles.coerceAtLeast(0.0)
    BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.load_detail_deadhead_title),
                style = MaterialTheme.typography.titleMedium,
                color = tc.TextPrimary,
            )
            Text(
                if (deadhead > 0.0) {
                    if (DisplayUnits.isMetric) {
                        stringResource(
                            R.string.load_detail_deadhead_set_km,
                            MeasurementUnits.displayDistance(deadhead),
                            MeasurementUnits.displayDistance(load.totalMiles),
                            MeasurementUnits.displayDistance(load.drivenMiles),
                        )
                    } else {
                        stringResource(
                            R.string.load_detail_deadhead_set,
                            deadhead,
                            load.totalMiles,
                            load.drivenMiles,
                        )
                    }
                } else {
                    stringResource(R.string.load_detail_deadhead_none)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = tc.TextSecondary,
            )
            TlOutlinedButton(
                onClick = { showEditor = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (deadhead > 0.0) {
                        stringResource(R.string.load_detail_deadhead_edit)
                    } else {
                        stringResource(R.string.load_detail_deadhead_add)
                    },
                )
            }
        }
    }
    if (showEditor) {
        DeadheadEditorDialog(
            initialMiles = deadhead,
            onDismiss = { showEditor = false },
            onSave = { miles ->
                showEditor = false
                onSave(miles)
            },
        )
    }
}

@Composable
private fun DeadheadEditorDialog(
    initialMiles: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
) {
    val metric = DisplayUnits.isMetric
    var text by rememberSaveable {
        mutableStateOf(
            when {
                initialMiles <= 0.0 -> ""
                metric -> MeasurementUnits.formatInput(MeasurementUnits.displayDistance(initialMiles))
                else -> String.format(Locale.US, "%.0f", initialMiles)
            },
        )
    }
    val parsed = if (metric) parseDeadheadDisplay(text) else DeadheadMiles.parse(text)
    val keyboard = LocalSoftwareKeyboardController.current
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.dialogAboveIme(),
        title = { Text(stringResource(R.string.load_detail_deadhead_title)) },
        text = {
            Column(
                modifier = Modifier.dialogBodyScroll(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.load_detail_deadhead_hint))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = {
                        Text(
                            stringResource(
                                if (metric) {
                                    R.string.load_detail_deadhead_miles_label_km
                                } else {
                                    R.string.load_detail_deadhead_miles_label
                                },
                            ),
                        )
                    },
                    singleLine = true,
                    isError = parsed == null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboard?.hide()
                            parsed?.let(onSave)
                        },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onSave) },
                enabled = parsed != null,
            ) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

/** Typed kilometers become stored miles. Blank clears deadhead, same as [DeadheadMiles.parse]. */
private fun parseDeadheadDisplay(raw: String): Double? {
    if (raw.isBlank()) return 0.0
    val display = DecimalInput.parse(raw) ?: return null
    val miles = MeasurementUnits.storedDistance(display)
    if (miles < 0.0 || miles > DeadheadMiles.MAX) return null
    return miles
}
