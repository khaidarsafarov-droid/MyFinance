package com.truckerload.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckerload.presentation.theme.BentoGlassMetricCell
import com.truckerload.presentation.theme.LocalTruckColors

data class BentoItem(
    val value: String,
    val label: String,
    val color: Color? = null,
    val highlight: Boolean = false,
    val onClick: (() -> Unit)? = null,
)

@Composable
fun BentoGrid(
    items: List<BentoItem>,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    horizontalSpacing: androidx.compose.ui.unit.Dp = 10.dp,
    verticalSpacing: androidx.compose.ui.unit.Dp = 10.dp,
) {
    val tc = LocalTruckColors.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
    ) {
        items.chunked(columns).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)
            ) {
                rowItems.forEach { item ->
                    BentoGlassMetricCell(
                        modifier = Modifier.weight(1f),
                        label = item.label,
                        value = item.value,
                        accent = item.color ?: tc.TextPrimary,
                        highlight = item.highlight
                    )
                }
                repeat(columns - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun BentoSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    emoji: String? = null,
) {
    val tc = LocalTruckColors.current
    Text(
        text = if (emoji != null) "$emoji $title" else title,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        ),
        color = tc.AccentPrimary
    )
}
