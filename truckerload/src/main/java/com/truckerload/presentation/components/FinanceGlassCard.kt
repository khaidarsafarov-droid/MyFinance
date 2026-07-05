package com.truckerload.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.theme.FinanceCockpitColors

@Composable
fun FinanceGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(8.dp, MaterialTheme.shapes.large)
            .clip(MaterialTheme.shapes.large)
            .background(FinanceCockpitColors.GlassCard)
            .border(1.dp, FinanceCockpitColors.GlassBorder, MaterialTheme.shapes.large)
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun FinanceNetProfitCard(
    amount: Double,
    loadsCount: Int,
    totalMiles: Double,
    modifier: Modifier = Modifier
) {
    FinanceGlassCard(modifier = modifier) {
        Column {
            androidx.compose.material3.Text(
                text = stringResource(R.string.finance_net_profit_title),
                style = MaterialTheme.typography.labelMedium,
                color = FinanceCockpitColors.TextSecondary
            )
            androidx.compose.material3.Text(
                text = "$${String.format("%,.2f", amount)}",
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            FinanceCockpitColors.NetProfitStart,
                            FinanceCockpitColors.NetProfitEnd
                        )
                    )
                ).merge(MaterialTheme.typography.headlineLarge)
            )
            androidx.compose.material3.Text(
                text = stringResource(R.string.finance_loads_miles, loadsCount, totalMiles),
                style = MaterialTheme.typography.bodySmall,
                color = FinanceCockpitColors.TextMuted,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
