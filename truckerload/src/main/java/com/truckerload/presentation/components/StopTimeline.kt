package com.truckerload.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
                Text("🕐 ${stop.scheduledTime} ${stop.timezone}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 2.dp))
                Text(stop.facilityCode ?: "", style = MaterialTheme.typography.labelMedium)
                Text(stop.fullAddress, style = MaterialTheme.typography.bodyMedium)
                Text("${stop.city}, ${stop.state} ${stop.zip}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
