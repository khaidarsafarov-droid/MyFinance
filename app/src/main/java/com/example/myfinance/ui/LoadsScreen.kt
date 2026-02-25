package com.example.myfinance.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myfinance.data.Trip
import java.text.SimpleDateFormat
import java.util.Locale

private fun monthKey(date: String): String {
    return date.takeIf { it.length >= 7 } ?: date
}

private fun formatMonthKey(key: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val d = sdf.parse(key) ?: return key
        SimpleDateFormat("MMMM yyyy", Locale.US).format(d)
    } catch (_: Exception) {
        key
    }
}

@Composable
fun LoadsScreen(
    trips: List<Trip>,
    viewModel: LogisticsViewModel,
    onEditTrip: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val byMonth = trips
        .sortedByDescending { it.date }
        .groupBy { monthKey(it.date) }
        .toList()
        .sortedByDescending { it.first }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (trips.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                ) {
                    Text(
                        "No loads yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        "Add loads: tap + in the top bar, or send trip details to your Telegram bot and Sync.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            byMonth.forEach { (monthKey, list) ->
                item {
                    Text(
                        formatMonthKey(monthKey),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(list) { trip ->
                    TripCard(
                        trip = trip,
                        onDelete = { viewModel.deleteTrip(trip.id) },
                        onEdit = { onEditTrip(trip.id) },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }
    }
}
