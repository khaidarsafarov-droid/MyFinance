package com.truckerload.presentation.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckerload.presentation.components.StatBox
import com.truckerload.presentation.components.WeekCalendarPicker
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.di.LocalWeekRepository
import com.truckerload.presentation.theme.LocalTruckColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(onBack: () -> Unit) {
    val tc = LocalTruckColors.current
    val weekRepository = LocalWeekRepository.current
    val loadRepository = LocalLoadRepository.current
    val viewModel: StatsViewModel = viewModel(
        factory = StatsViewModel.Factory(weekRepository, loadRepository)
    )
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = tc.Background,
        topBar = {
            TopAppBar(
                title = { Text("Статистика", color = tc.TextPrimary) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = tc.TextPrimary) } },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = tc.Background,
                    titleContentColor = tc.TextPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(tc.Background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
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
            if (uiState.weekLabel.isNotBlank()) {
                Text(
                    text = uiState.weekLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBox(title = "Зарплата", value = "$${String.format("%,.0f", uiState.totalPaycheck)}", modifier = Modifier.weight(1f))
                StatBox(title = "Миль", value = "${String.format("%,.0f", uiState.totalMiles)}", modifier = Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBox(title = "Рейсов", value = "${uiState.loadCount}", modifier = Modifier.weight(1f))
                StatBox(title = "Дизель", value = "$${String.format("%,.0f", uiState.totalDiesel)}", modifier = Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBox(title = "Гросс", value = "$${String.format("%,.0f", uiState.totalGross)}", modifier = Modifier.weight(1f))
                StatBox(title = "Чистая прибыль", value = "$${String.format("%,.0f", uiState.netProfit)}", modifier = Modifier.weight(1f))
            }
        }
    }
}
