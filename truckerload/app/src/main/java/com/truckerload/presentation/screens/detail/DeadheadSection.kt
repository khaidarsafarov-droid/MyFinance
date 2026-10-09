package com.truckerload.presentation.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.model.DeadheadMiles
import com.truckerload.domain.model.Load
import com.truckerload.domain.premium.PremiumFeature
import com.truckerload.presentation.premium.PremiumLockedCard
import com.truckerload.presentation.premium.rememberPremiumAccess
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.LocalTruckColors
import java.util.Locale

@Composable
internal fun DeadheadSection(
    load: Load,
    onSave: (Double) -> Unit,
) {
    val tc = LocalTruckColors.current
    val premium by rememberPremiumAccess().status.collectAsStateWithLifecycle()
    var showEditor by rememberSaveable { mutableStateOf(false) }
    val deadhead = load.deadheadMiles.coerceAtLeast(0.0)
    if (!premium.allows(PremiumFeature.DEADHEAD)) {
        BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                PremiumLockedCard(PremiumFeature.DEADHEAD)
            }
        }
        return
    }
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
                    stringResource(
                        R.string.load_detail_deadhead_set,
                        deadhead,
                        load.totalMiles,
                        load.drivenMiles,
                    )
                } else {
                    stringResource(R.string.load_detail_deadhead_none)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = tc.TextSecondary,
            )
            OutlinedButton(
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
    var text by rememberSaveable {
        mutableStateOf(
            if (initialMiles > 0.0) String.format(Locale.US, "%.0f", initialMiles) else "",
        )
    }
    val parsed = DeadheadMiles.parse(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.load_detail_deadhead_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.load_detail_deadhead_hint))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.load_detail_deadhead_miles_label)) },
                    singleLine = true,
                    isError = parsed == null,
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
