package com.truckerload.presentation.screens.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.di.LocalDieselRepository
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.di.LocalPaycheckRepository
import com.truckerload.presentation.di.LocalWeekRepository
import com.truckerload.presentation.components.WeekCalendarPicker

@Composable
fun FinanceScreen(
    onAddPaycheck: () -> Unit,
    onAddDiesel: () -> Unit,
    onLoadClick: (String) -> Unit
) {
    val tc = LocalTruckColors.current
    val weekRepository = LocalWeekRepository.current
    val loadRepository = LocalLoadRepository.current
    val paycheckRepository = LocalPaycheckRepository.current
    val dieselRepository = LocalDieselRepository.current
    val viewModel: FinanceViewModel = viewModel(
        factory = FinanceViewModel.Factory(weekRepository, loadRepository, paycheckRepository, dieselRepository)
    )
    val uiState by viewModel.uiState.collectAsState()
    val summary = uiState.weekSummary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(tc.Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = uiState.period == FinancePeriod.WEEK,
                onClick = { viewModel.setPeriod(FinancePeriod.WEEK) },
                label = { Text("Неделя") },
                shape = MaterialTheme.shapes.small,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = tc.SurfaceSecondary,
                    selectedLabelColor = tc.AccentPrimary,
                    containerColor = tc.CardBackground,
                    labelColor = tc.TextSecondary
                )
            )
            FilterChip(
                selected = uiState.period == FinancePeriod.MONTH,
                onClick = { viewModel.setPeriod(FinancePeriod.MONTH) },
                label = { Text("Месяц") },
                shape = MaterialTheme.shapes.small,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = tc.SurfaceSecondary,
                    selectedLabelColor = tc.AccentPrimary,
                    containerColor = tc.CardBackground,
                    labelColor = tc.TextSecondary
                )
            )
            FilterChip(
                selected = uiState.period == FinancePeriod.YEAR,
                onClick = { viewModel.setPeriod(FinancePeriod.YEAR) },
                label = { Text("Год") },
                shape = MaterialTheme.shapes.small,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = tc.SurfaceSecondary,
                    selectedLabelColor = tc.AccentPrimary,
                    containerColor = tc.CardBackground,
                    labelColor = tc.TextSecondary
                )
            )
        }
        Spacer(modifier = Modifier.padding(8.dp))
        if (uiState.period == FinancePeriod.WEEK) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = tc.CardBackground)
            ) {
                WeekCalendarPicker(
                    selectedMonth = uiState.calendarMonth,
                    selectedYear = uiState.calendarYear,
                    weeksInMonth = uiState.weeksInMonth,
                    selectedWeekNumber = uiState.weekNumber,
                    selectedWeekYear = uiState.year,
                    onMonthYearChange = { m, y -> viewModel.setMonthYear(m, y) },
                    onWeekSelect = { w, y -> viewModel.selectWeek(w, y) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { viewModel.previousWeek() }) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous", tint = tc.TextPrimary)
                }
                Text(
                    summary?.weekLabel ?: "Week ${uiState.weekNumber}",
                    style = MaterialTheme.typography.titleSmall,
                    color = tc.TextSecondary
                )
                IconButton(onClick = { viewModel.nextWeek() }) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next", tint = tc.TextPrimary)
                }
            }
        } else {
            Text(
                "Week ${uiState.weekNumber} • ${summary?.weekLabel ?: ""}",
                style = MaterialTheme.typography.titleMedium,
                color = tc.TextPrimary
            )
        }
        if (summary != null) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = tc.CardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Week ${summary.weekNumber} • ${summary.weekLabel}", style = MaterialTheme.typography.titleSmall, color = tc.TextSecondary)
                    Spacer(modifier = Modifier.padding(8.dp))
                    Text("💰 Зарплата          $${String.format("%,.2f", summary.paycheckAmount)}", style = MaterialTheme.typography.bodyMedium, color = tc.AccentPrimary)
                    Text("⛽ Дизель             -$${String.format("%,.2f", summary.dieselAmount)}", style = MaterialTheme.typography.bodyMedium, color = tc.AccentExpense)
                    Text("─────────────────────────────", style = MaterialTheme.typography.bodySmall, color = tc.Divider)
                    Text("📈 Чистыми            $${String.format("%,.2f", summary.netProfit)}", style = MaterialTheme.typography.headlineMedium, color = tc.AccentPrimary)
                    Text("🚛 ${summary.loadsCount} лоуда     •     ${String.format("%,.0f", summary.totalMiles)} mi", style = MaterialTheme.typography.bodySmall, color = tc.TextSecondary, modifier = Modifier.padding(top = 8.dp))
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("💰 Зарплата", style = MaterialTheme.typography.titleSmall, color = tc.TextPrimary)
                IconButton(onClick = onAddPaycheck) { Icon(Icons.Default.Add, contentDescription = "Add", tint = tc.AccentPrimary) }
            }
            if (uiState.paycheck != null) {
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = tc.CardBackground)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(uiState.paycheck!!.weekLabel, style = MaterialTheme.typography.bodyMedium, color = tc.TextSecondary)
                        Text("Выплата: $${String.format("%,.2f", uiState.paycheck!!.netAmount)}", style = MaterialTheme.typography.bodyMedium, color = tc.AccentPrimary)
                    }
                }
            } else {
                Text("Данные за эту неделю не добавлены", style = MaterialTheme.typography.bodySmall, color = tc.TextSecondary)
            }
            Spacer(modifier = Modifier.padding(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("⛽ Дизель", style = MaterialTheme.typography.titleSmall, color = tc.TextPrimary)
                IconButton(onClick = onAddDiesel) { Icon(Icons.Default.Add, contentDescription = "Add", tint = tc.AccentPrimary) }
            }
            if (uiState.dieselList.isNotEmpty()) {
                uiState.dieselList.forEach { d ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = tc.CardBackground)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(d.location ?: d.weekLabel, style = MaterialTheme.typography.bodyMedium, color = tc.TextPrimary)
                            Text("Потрачено: $${String.format("%,.2f", d.totalAmount)}", style = MaterialTheme.typography.bodyMedium, color = tc.AccentExpense)
                        }
                    }
                }
            } else {
                Text("Данные за эту неделю не добавлены", style = MaterialTheme.typography.bodySmall, color = tc.TextSecondary)
            }
        }
    }
}
