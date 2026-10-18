package com.truckerload.presentation.components

import com.truckerload.presentation.icons.AppIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.truckerload.presentation.theme.AppTypography
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatJournalDate(raw: String): String {
    val parsed = runCatching { LocalDate.parse(raw.take(10)) }.getOrNull() ?: return raw
    return parsed.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
}

/**
 * Shared pickup → delivery block used by the home load card and the detail summary
 * so the shared-element transition reads as the same card.
 */
@Composable
fun LoadRouteBlock(
    dateLabel: String,
    tripLabel: String,
    origin: String,
    destination: String,
    rate: String,
    meta: String,
    modifier: Modifier = Modifier,
    equipment: String? = null,
    trailing: @Composable () -> Unit = {},
    belowRate: @Composable () -> Unit = {},
) {
    val cs = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = buildString {
                    append(dateLabel)
                    if (tripLabel.isNotBlank()) {
                        append(" · ")
                        append(tripLabel)
                    }
                    if (!equipment.isNullOrBlank()) {
                        append(" · ")
                        append(equipment)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            trailing()
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RouteDots(
                origin = origin,
                destination = destination,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                tint = cs.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = rate,
            style = AppTypography.HeroNumber.copy(fontSize = MaterialTheme.typography.displaySmall.fontSize),
            color = cs.onSurface,
            maxLines = 1,
        )
        if (meta.isNotBlank()) {
            Text(
                text = meta,
                style = AppTypography.Caption.copy(fontFeatureSettings = "tnum"),
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        belowRate()
    }
}

@Composable
fun RouteDots(
    origin: String,
    destination: String,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RouteDot(filled = false)
            Box(
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .width(2.dp)
                    .height(18.dp)
                    .background(cs.primary.copy(alpha = 0.35f)),
            )
            RouteDot(filled = true)
        }
        Column(
            modifier = Modifier.padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = origin.ifBlank { "—" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = destination.ifBlank { "—" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RouteDot(filled: Boolean) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .then(
                if (filled) {
                    Modifier.background(cs.primary)
                } else {
                    Modifier.border(2.dp, cs.primary, CircleShape)
                },
            ),
    )
}
