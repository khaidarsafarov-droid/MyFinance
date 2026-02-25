package com.truckerload.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.truckerload.domain.model.Load
import com.truckerload.presentation.theme.LocalTruckColors

@Composable
fun LoadCard(
    load: Load,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tc = LocalTruckColors.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = tc.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = load.tripId,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = tc.AccentInfo
                )
                Text(
                    text = load.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary
                )
            }
            Text(
                text = "${load.pointA}  ━━━━━━━►  ${load.pointB}",
                style = MaterialTheme.typography.titleSmall,
                color = tc.TextPrimary,
                modifier = Modifier.padding(top = 12.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$${String.format("%,.2f", load.totalRate)}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = tc.AccentPrimary
                )
                Text(
                    text = "${String.format("%,.2f", load.totalMiles)} mi",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = tc.TextSecondary
                )
            }
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChipLabel(text = "📦 ${load.puCount} PU")
                ChipLabel(text = "🏁 ${load.delCount} DEL")
            }
        }
    }
}

@Composable
private fun ChipLabel(text: String) {
    val tc = LocalTruckColors.current
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = tc.TextSecondary,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(tc.SurfaceSecondary)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
