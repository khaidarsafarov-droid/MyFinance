package com.example.myfinance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.myfinance.data.Goal
import com.example.myfinance.data.WeeklyTotal
import com.example.myfinance.notifications.GoalNotificationHelper

private fun netInPeriod(weeklyTotals: List<WeeklyTotal>, periodStart: String, periodEnd: String): Double {
    return weeklyTotals
        .filter { wt ->
            val d = wt.date.take(10)
            d >= periodStart && d <= periodEnd
        }
        .sumOf { it.netProfit }
}

@Composable
fun SummaryScreen(
    viewModel: LogisticsViewModel,
    onAddWeeklyTotal: () -> Unit,
    onEditWeeklyTotal: (String) -> Unit,
    onCompany: (String) -> Unit,
    onAnalytics: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appData = viewModel.appData.value
    val weeklyTotals = appData.weeklyTotals
    val goal = appData.goal
    var showGoalDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val (netInPeriod, achieved) = remember(goal, weeklyTotals) {
        if (goal == null) Pair(0.0, false)
        else {
            val net = netInPeriod(weeklyTotals, goal.periodStart, goal.periodEnd)
            Pair(net, net >= goal.targetAmount)
        }
    }

    LaunchedEffect(goal, achieved) {
        if (goal != null && achieved && goal.achievedNotifiedAt == null) {
            GoalNotificationHelper.showGoalAchieved(context, goal.targetAmount)
            viewModel.markGoalNotified(goal)
        }
    }

    val totalGross = weeklyTotals.sumOf { it.gross }
    val totalMiles = weeklyTotals.sumOf { it.miles }
    val totalSalaryIn = weeklyTotals.sumOf { it.salaryIn }
    val totalDiesel = weeklyTotals.sumOf { it.diesel }
    val totalNet = weeklyTotals.sumOf { it.netProfit }

    if (showGoalDialog) {
        GoalDialog(
            existingGoal = goal,
            onDismiss = { showGoalDialog = false },
            onSave = { target, start, end ->
                if (goal == null) viewModel.setGoal(target, start, end)
                else viewModel.updateGoal(target, start, end)
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                "Account total",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "${weeklyTotals.size} week${if (weeklyTotals.size == 1) "" else "s"} · ${appData.companies.size} companies",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(Modifier.size(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MiniTotalCard("Gross", formatCurrency(totalGross), Modifier.weight(1f))
                MiniTotalCard("Miles", "%,.0f".format(totalMiles), Modifier.weight(1f))
                MiniTotalCard("Salary in", formatCurrency(totalSalaryIn), Modifier.weight(1f))
                MiniTotalCard("Diesel", formatCurrency(totalDiesel), Modifier.weight(1f))
                MiniTotalCard("Net", formatCurrency(totalNet), Modifier.weight(1f))
            }

            Spacer(Modifier.size(16.dp))
            if (goal != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Flag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(
                                "Net profit goal",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            "${goal.periodStart} – ${goal.periodEnd} · Target ${formatCurrency(goal.targetAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            "Current in period: ${formatCurrency(netInPeriod)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        if (achieved) {
                            Text(
                                "Goal achieved!",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        } else {
                            val progress = (netInPeriod / goal.targetAmount).toFloat().coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showGoalDialog = true },
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("Edit") }
                            Button(
                                onClick = { viewModel.clearGoal() },
                                shape = RoundedCornerShape(8.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) { Text("Clear goal") }
                        }
                    }
                }
            } else {
                Button(
                    onClick = { showGoalDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Flag, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Set net profit goal")
                }
            }
        }
        Text(
            "Weekly totals",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(weeklyTotals.sortedByDescending { it.date }) { wt ->
                val companyNames = wt.companyIds.mapNotNull { id -> appData.companies.find { it.id == id }?.name }.joinToString(", ")
                WeeklyTotalCard(
                    weeklyTotal = wt,
                    companyNames = companyNames,
                    onDelete = { viewModel.deleteWeeklyTotal(wt.id) },
                    onEdit = { onEditWeeklyTotal(wt.id) }
                )
            }
            item {
                Text(
                    "My Companies",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }
            items(appData.companies) { company ->
                val companyWeeks = weeklyTotals.filter { it.companyIds.contains(company.id) }
                val weeks = companyWeeks.size
                val gross = companyWeeks.sumOf { it.gross }
                val net = companyWeeks.sumOf { it.netProfit }
                Card(
                    onClick = { onCompany(company.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                Icons.Default.Business,
                                contentDescription = null,
                                modifier = Modifier.padding(6.dp).size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                company.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "$weeks week${if (weeks == 1) "" else "s"} · ${formatCurrency(net)} net",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Open",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
            item { Spacer(Modifier.padding(72.dp)) }
        }
    }
}

