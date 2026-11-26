package com.truckerload.presentation.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.LoadCancellation
import com.truckerload.domain.model.StopType
import com.truckerload.domain.money.Money
import com.truckerload.domain.money.MoneyInput
import com.truckerload.domain.units.DisplayUnits
import com.truckerload.presentation.components.dialogAboveIme
import com.truckerload.presentation.components.dialogBodyScroll
import com.truckerload.presentation.components.formatTonu
import com.truckerload.presentation.theme.AppChipLabel
import com.truckerload.presentation.theme.AppFilterChip
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.MoneyFormat
import com.truckerload.utils.stopMillisForLoad

/** Earliest scheduled PU time, or null when no stop has a usable time (date-only fallback is not a pickup time). */
internal fun scheduledPickupMillis(load: Load): Long? = load.stops
    .filter { it.type == StopType.PU }
    .mapNotNull { stopMillisForLoad(it.scheduledTime, load.date) }
    .minOrNull()

/**
 * Asks for the TONU pay before marking a load cancelled (or edits it on a cancelled load).
 * Quick picks are suggestions only; any amount, including $0, can be typed.
 */
@Composable
internal fun LoadCancelDialog(load: Load, nowMillis: Long, onDismiss: () -> Unit, onConfirm: (Money) -> Unit) {
    val pickup = scheduledPickupMillis(load)
    val inWindow = LoadCancellation.isWithinTonuWindow(pickup, nowMillis)
    var text by rememberSaveable(load.id) {
        mutableStateOf(MoneyInput.format(LoadCancellation.suggestedTonu(load, pickup, nowMillis)))
    }
    val parsed = MoneyInput.parse(text)
    val valid = LoadCancellation.isValidTonu(parsed)
    val keyboard = LocalSoftwareKeyboardController.current
    val title = if (load.isCancelled) R.string.load_cancel_edit_title else R.string.load_cancel_title
    val confirm = if (load.isCancelled) R.string.load_cancel_save else R.string.load_cancel_confirm
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.dialogAboveIme(),
        title = { Text(stringResource(title)) },
        text = {
            TonuForm(
                intro = !load.isCancelled,
                inWindow = inWindow,
                text = text,
                onText = { text = it },
                onDone = {
                    keyboard?.hide()
                    if (valid) parsed?.let(onConfirm)
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onConfirm) },
                enabled = valid,
            ) {
                Text(stringResource(confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TonuForm(intro: Boolean, inWindow: Boolean, text: String, onText: (String) -> Unit, onDone: () -> Unit) {
    val parsed = MoneyInput.parse(text)
    Column(
        modifier = Modifier.dialogBodyScroll(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (intro) {
            Text(stringResource(R.string.load_cancel_body))
        }
        if (inWindow) {
            Text(
                text = stringResource(R.string.load_cancel_window_hint),
                color = LocalTruckColors.current.Warning,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = stringResource(R.string.load_cancel_tonu_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LoadCancellation.SUGGESTED_TONU.forEach { amount ->
                AppFilterChip(
                    selected = parsed == amount,
                    onClick = { onText(MoneyInput.format(amount)) },
                    label = { AppChipLabel(formatTonu(amount)) },
                )
            }
            AppFilterChip(
                selected = parsed == Money.ZERO,
                onClick = { onText(MoneyInput.format(Money.ZERO)) },
                label = { AppChipLabel(stringResource(R.string.load_cancel_no_pay)) },
            )
        }
        OutlinedTextField(
            value = text,
            onValueChange = onText,
            label = { Text(stringResource(R.string.load_cancel_tonu_label)) },
            singleLine = true,
            isError = text.isNotBlank() && !LoadCancellation.isValidTonu(parsed),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Confirms restoring the rate and loaded miles from before the cancellation. */
@Composable
internal fun LoadRestoreDialog(load: Load, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val rate = formatTonu(load.originalRate ?: load.totalRate)
    val miles = originalMilesLabel(load)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.load_cancel_undo_title)) },
        text = { Text(stringResource(R.string.load_cancel_undo_body, rate, miles)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.load_cancel_undo))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun originalMilesLabel(load: Load): String {
    val unit = if (DisplayUnits.isMetric) R.string.load_card_km else R.string.load_card_miles
    return stringResource(unit, MoneyFormat.formatDistanceNumber(load.originalMiles ?: 0.0))
}

/** Banner on a cancelled load: TONU pay, what it was before, edit and restore. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LoadCancelledPanel(load: Load, onEdit: () -> Unit, onRestore: () -> Unit, modifier: Modifier = Modifier) {
    val danger = LocalTruckColors.current.Danger
    BentoGlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (load.totalRate.isPositive()) {
                    stringResource(R.string.load_cancel_badge_tonu, formatTonu(load.totalRate))
                } else {
                    stringResource(R.string.load_cancel_badge)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = danger,
            )
            load.originalRate?.let { rate ->
                Text(
                    text = stringResource(R.string.load_cancel_original, formatTonu(rate), originalMilesLabel(load)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit) {
                    Text(stringResource(R.string.load_cancel_edit_tonu))
                }
                OutlinedButton(onClick = onRestore) {
                    Text(stringResource(R.string.load_cancel_undo))
                }
            }
        }
    }
}
