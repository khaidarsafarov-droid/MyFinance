package com.truckerload.presentation.components

import com.truckerload.presentation.icons.AppIcons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType

@Composable
fun StopTimeline(
    stops: List<Stop>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        stops.forEach { stop ->
            val isPu = stop.type == StopType.PU
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = if (isPu) "PU #${stop.stopNumber}" else "DEL #${stop.stopNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                stop.puNumber?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                stop.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = AppIcons.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Text("${stop.scheduledTime} ${stop.timezone}", style = MaterialTheme.typography.labelMedium)
                }
                Text(stop.facilityCode ?: "", style = MaterialTheme.typography.labelMedium)
                Text(stop.fullAddress, style = MaterialTheme.typography.bodyMedium)
                Text("${stop.city}, ${stop.state} ${stop.zip}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
