package com.truckerload.presentation.screens.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.model.Load
import com.truckerload.domain.units.DisplayUnits
import com.truckerload.presentation.components.DisputeSection
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.rememberChipChrome
import com.truckerload.presentation.utils.MoneyFormat

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LoadDetailActionChips(
    load: Load,
    onEditMiles: () -> Unit,
    onEditFinish: () -> Unit,
    onSaveDeadhead: (Double) -> Unit,
    onDisputeChanged: (Load) -> Unit,
    modifier: Modifier = Modifier,
    onCancelLoad: () -> Unit = {},
) {
    var showDeadhead by rememberSaveable { mutableStateOf(false) }
    var showDispute by rememberSaveable { mutableStateOf(false) }
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DistanceChips(
            load = load,
            onEditMiles = onEditMiles,
            onEditDeadhead = { showDeadhead = true },
        )
        if (!load.isCancelled) {
            ActiveLoadChips(
                load = load,
                onFinish = onEditFinish,
                onDispute = { showDispute = !showDispute },
                onCancel = onCancelLoad,
            )
        }
    }
    if (showDispute && !load.isCancelled) {
        BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
            DisputeSection(
                load = load,
                onDisputeChanged = onDisputeChanged,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
    if (showDeadhead) {
        DeadheadEditorDialog(
            initialMiles = load.deadheadMiles.coerceAtLeast(0.0),
            onDismiss = { showDeadhead = false },
            onSave = { miles ->
                showDeadhead = false
                onSaveDeadhead(miles)
            },
        )
    }
}

/** Loaded miles (hidden on a cancelled load: it has none) and deadhead, which always counts. */
@Composable
private fun DistanceChips(load: Load, onEditMiles: () -> Unit, onEditDeadhead: () -> Unit) {
    val milesSet = load.totalMiles > 0.0
    val deadhead = load.deadheadMiles.coerceAtLeast(0.0)
    val distanceUnit = if (DisplayUnits.isMetric) R.string.load_card_km else R.string.load_card_miles
    if (!load.isCancelled) {
        DetailInfoChip(
            label = if (milesSet) {
                stringResource(distanceUnit, MoneyFormat.formatDistanceNumber(load.totalMiles))
            } else {
                stringResource(R.string.load_detail_chip_add_miles)
            },
            filled = milesSet,
            onClick = onEditMiles,
        )
    }
    DetailInfoChip(
        label = if (deadhead > 0.0) {
            stringResource(
                R.string.load_detail_chip_deadhead_value,
                stringResource(distanceUnit, MoneyFormat.formatDistanceNumber(deadhead)),
            )
        } else {
            stringResource(R.string.load_detail_deadhead_add)
        },
        filled = deadhead > 0.0,
        onClick = onEditDeadhead,
    )
}

/** Delivery, dispute, and "load cancelled" chips; hidden once the load is cancelled. */
@Composable
private fun ActiveLoadChips(load: Load, onFinish: () -> Unit, onDispute: () -> Unit, onCancel: () -> Unit) {
    val delivered = !load.actualFinishDate.isNullOrBlank()
    DetailInfoChip(
        label = stringResource(
            if (delivered) R.string.load_detail_chip_delivered else R.string.load_detail_chip_complete,
        ),
        filled = delivered,
        onClick = onFinish,
    )
    DetailInfoChip(
        label = when {
            load.isActiveDispute -> stringResource(R.string.load_detail_chip_dispute_open)
            load.hadDispute -> stringResource(R.string.dispute_was_dispute)
            else -> stringResource(R.string.load_detail_chip_add_dispute)
        },
        filled = load.isDispute,
        onClick = onDispute,
    )
    DetailInfoChip(
        label = stringResource(R.string.load_cancel_action),
        filled = false,
        onClick = onCancel,
    )
}

@Composable
private fun DetailInfoChip(
    label: String,
    filled: Boolean,
    onClick: () -> Unit,
) {
    val chrome = rememberChipChrome(filled)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = chrome.container,
        contentColor = chrome.content,
        border = BorderStroke(1.dp, chrome.border),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (filled) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
