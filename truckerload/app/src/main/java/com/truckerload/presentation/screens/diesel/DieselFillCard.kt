package com.truckerload.presentation.screens.diesel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.model.Diesel
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.MoneyFormat
import com.truckerload.utils.formatDateForDisplay
import java.util.Locale

@Composable
fun DieselFillCard(
    diesel: Diesel,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val tc = LocalTruckColors.current
    BentoGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = formatDateForDisplay(diesel.addedAt),
                    style = MaterialTheme.typography.titleSmall,
                    color = tc.TextPrimary,
                )
                Text(
                    text = MoneyFormat.formatCurrency(diesel.totalAmount, decimals = 2),
                    style = AppTypography.NumbersSmall,
                    color = tc.TextPrimary,
                )
            }
            Text(
                text = diesel.location?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.diesel_location_unknown),
                style = MaterialTheme.typography.bodyMedium,
                color = tc.TextSecondary,
            )
            Text(
                text = dieselCardMeta(diesel),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
            )
        }
    }
}

private fun dieselCardMeta(diesel: Diesel): String {
    val parts = mutableListOf<String>()
    diesel.gallons?.let { gallons ->
        parts += String.format(Locale.US, "%.2f gal", gallons)
    }
    diesel.pricePerGallon?.let { price ->
        parts += String.format(Locale.US, "$%.2f/gal", price)
    }
    diesel.savingsAmount?.takeIf { it > 0.0 }?.let { saved ->
        parts += "−${MoneyFormat.formatCurrency(saved, decimals = 2)}"
    }
    return parts.joinToString("  ·  ")
}
