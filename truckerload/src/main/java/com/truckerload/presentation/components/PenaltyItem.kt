package com.truckerload.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PenaltyItem(
    description: String,
    amount: Double,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(
            if (amount < 0) "-$${String.format("%.2f", -amount)}" else "$${String.format("%.2f", amount)}",
            style = MaterialTheme.typography.bodyMedium,
            color = if (amount < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    }
}
