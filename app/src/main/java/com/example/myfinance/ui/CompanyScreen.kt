package com.example.myfinance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myfinance.data.WeeklyTotal

@Composable
fun CompanyScreen(
    companyId: String,
    companyName: String,
    weeklyTotals: List<WeeklyTotal>,
    isCurrentCompany: Boolean,
    viewModel: LogisticsViewModel,
    onAddWeeklyTotal: () -> Unit,
    onEditWeeklyTotal: (String) -> Unit,
    onSetAsCurrent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val weeks = weeklyTotals.size
    val gross = weeklyTotals.sumOf { it.gross }
    val net = weeklyTotals.sumOf { it.netProfit }
    val diesel = weeklyTotals.sumOf { it.diesel }
    val sorted = weeklyTotals.sortedByDescending { it.date }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                companyName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "$weeks week${if (weeks == 1) "" else "s"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            if (!isCurrentCompany) {
                TextButton(onClick = onSetAsCurrent) {
                    Text("Set as current company")
                }
            }
            Spacer(Modifier.size(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniTotalCard("Gross", formatCurrency(gross), Modifier.weight(1f))
                MiniTotalCard("Net", formatCurrency(net), Modifier.weight(1f))
                MiniTotalCard("Diesel", formatCurrency(diesel), Modifier.weight(1f))
            }
        }
        Text(
            "Weekly totals",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        if (sorted.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "No weekly totals yet",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Spacer(Modifier.size(16.dp))
                FloatingActionButton(
                    onClick = onAddWeeklyTotal,
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add weekly total")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sorted) { wt ->
                    WeeklyTotalCard(
                        weeklyTotal = wt,
                        companyNames = companyName,
                        onDelete = { viewModel.deleteWeeklyTotal(wt.id) },
                        onEdit = { onEditWeeklyTotal(wt.id) }
                    )
                }
                item { Spacer(Modifier.padding(72.dp)) }
            }
        }
    }
}

