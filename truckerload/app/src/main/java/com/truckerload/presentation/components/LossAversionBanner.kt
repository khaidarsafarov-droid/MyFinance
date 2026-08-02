package com.truckerload.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.ux.LossAversionKind
import com.truckerload.domain.ux.LossAversionSignal
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.MoneyFormat

@Composable
fun LossAversionBanner(
    signal: LossAversionSignal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (signal.kind == LossAversionKind.NONE) return
    val tc = LocalTruckColors.current
    val text = when (signal.kind) {
        LossAversionKind.GOAL_BEHIND -> stringResource(
            R.string.ux_loss_goal_behind,
            MoneyFormat.formatCurrency(signal.remainingAmount),
            signal.daysRemaining,
        )
        LossAversionKind.GOAL_UNSET_WITH_EARNINGS -> stringResource(
            R.string.ux_loss_goal_unset,
            MoneyFormat.formatCurrency(signal.currentGross),
        )
        LossAversionKind.NO_LOADS_THIS_WEEK -> stringResource(R.string.ux_loss_no_loads)
        LossAversionKind.NONE -> return
    }
    Text(
        text = text,
        color = tc.TextPrimary,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier
            .fillMaxWidth()
            .background(tc.AccentWarning.copy(alpha = 0.28f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}
