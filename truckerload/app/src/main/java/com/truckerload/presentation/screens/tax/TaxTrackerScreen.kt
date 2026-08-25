package com.truckerload.presentation.screens.tax

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.domain.tax.AccountantExportSection
import com.truckerload.domain.tax.PerDiemCalculator
import com.truckerload.presentation.components.LoadCalendarWithDots
import com.truckerload.presentation.components.TlButton
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.FinanceCockpitColors
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.AccountantExportShare
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxTrackerScreen(onBack: () -> Unit) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val viewModel: TaxTrackerViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showSendSheet by remember { mutableStateOf(false) }

    fun sendWorkbook(sections: Set<AccountantExportSection>) {
        viewModel.setExporting(true)
        scope.launch {
            runCatching {
                val input = viewModel.prepareWorkbookInput(sections)
                if (viewModel.isWorkbookEmpty(input, sections)) {
                    viewModel.setExporting(false, context.getString(R.string.tax_send_empty))
                    return@launch
                }
                val file = AccountantExportShare.writeWorkbook(context, input, sections)
                AccountantExportShare.shareWorkbook(context, file)
                viewModel.setExporting(
                    false,
                    context.getString(R.string.tax_export_success, file.name)
                )
                showSendSheet = false
            }.onFailure { err ->
                viewModel.setExporting(
                    false,
                    context.getString(
                        R.string.tax_export_error,
                        err.message ?: err.javaClass.simpleName,
                    ),
                )
            }
        }
    }

    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tax_title), color = tc.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            AppIcons.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = tc.TextPrimary,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSendSheet = true },
                        enabled = !uiState.isExporting && !uiState.isLoading,
                    ) {
                        Icon(
                            AppIcons.Share,
                            contentDescription = stringResource(R.string.tax_send_title),
                            tint = tc.TextPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BentoGlassTheme.ScreenBackground,
                    titleContentColor = tc.TextPrimary,
                ),
            )
        },
    ) { padding ->
        var refreshing by remember { mutableStateOf(false) }
        val pullRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                refreshing = true
                viewModel.refresh()
                scope.launch {
                    delay(800)
                    refreshing = false
                }
            },
            modifier = Modifier.fillMaxSize(),
            state = pullRefreshState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TaxYearSelector(
                    year = uiState.year,
                    onYearChange = viewModel::setYear,
                )

                if (uiState.nextQuarterlyDate.isNotBlank()) {
                    BentoGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = FinanceCockpitColors.SalaryAccent.copy(alpha = 0.35f),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.tax_next_quarter, uiState.nextQuarterlyDate),
                                style = MaterialTheme.typography.titleSmall,
                                color = tc.TextPrimary,
                            )
                            Text(
                                stringResource(R.string.tax_in_days, uiState.daysUntilNextQuarterly),
                                style = MaterialTheme.typography.bodySmall,
                                color = tc.TextSecondary,
                            )
                        }
                    }
                }

                BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.tax_income),
                            style = MaterialTheme.typography.titleSmall,
                            color = tc.TextPrimary,
                        )
                        Text(
                            "$${String.format(Locale.US, "%,.2f", uiState.totalGrossIncome)}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = FinanceCockpitColors.SalaryAccent,
                        )
                    }
                }

                BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            stringResource(R.string.tax_deductions),
                            style = MaterialTheme.typography.titleSmall,
                            color = tc.TextPrimary,
                        )
                        Text(
                            stringResource(R.string.tax_diesel_deduction, uiState.dieselDeductions),
                            style = MaterialTheme.typography.bodyMedium,
                            color = tc.AccentExpense,
                        )
                        Text(
                            stringResource(
                                R.string.tax_per_diem,
                                uiState.perDiemDays,
                                uiState.perDiemAmount,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = tc.AccentExpense,
                        )
                        Text(
                            stringResource(R.string.tax_per_diem_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = tc.TextLabel,
                        )
                        Text(
                            stringResource(
                                R.string.tax_total_deductions,
                                uiState.totalDeductions + uiState.perDiemAmount,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = tc.TextSecondary,
                        )
                    }
                }

                BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            stringResource(R.string.tax_per_diem_calendar_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = tc.TextPrimary,
                        )
                        Text(
                            stringResource(
                                R.string.tax_per_diem_calendar_subtitle,
                                uiState.perDiemDays,
                                PerDiemCalculator.DAILY_RATE.toInt(),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = tc.TextSecondary,
                        )
                        LoadCalendarWithDots(
                            year = uiState.year,
                            month = uiState.calendarMonth,
                            datesWithLoads = uiState.perDiemDates,
                            selectedDate = null,
                            onDateSelect = { },
                            onMonthChange = { y, m -> viewModel.setCalendarMonth(y, m) },
                        )
                    }
                }

                BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.taxable_income),
                            style = MaterialTheme.typography.titleSmall,
                            color = tc.TextPrimary,
                        )
                        Text(
                            "$${String.format(Locale.US, "%,.2f", uiState.taxableIncome)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = tc.TextPrimary,
                        )
                    }
                }

                BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.tax_se_tax, uiState.selfEmploymentTax),
                            style = MaterialTheme.typography.bodyMedium,
                            color = tc.TextSecondary,
                        )
                        Text(
                            stringResource(R.string.tax_federal_tax, uiState.federalTax),
                            style = MaterialTheme.typography.bodyMedium,
                            color = tc.TextSecondary,
                        )
                        Text(
                            stringResource(R.string.tax_total_owed, uiState.totalTaxOwed),
                            style = MaterialTheme.typography.headlineSmall,
                            color = tc.AccentExpense,
                        )
                    }
                }

                TlButton(
                    onClick = { showSendSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isExporting,
                ) {
                    Text(stringResource(R.string.tax_send_title))
                }

                uiState.exportMessage?.let { msg ->
                    Text(
                        msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = tc.TextSecondary,
                    )
                }
                uiState.errorMessage?.let { msg ->
                    Text(
                        msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = tc.AccentExpense,
                    )
                }

                Text(
                    stringResource(R.string.tax_disclaimer),
                    style = MaterialTheme.typography.labelSmall,
                    color = tc.TextLabel,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }

    if (showSendSheet) {
        TaxSendDataSheet(
            year = uiState.year,
            exporting = uiState.isExporting,
            onDismiss = { showSendSheet = false },
            onSend = ::sendWorkbook,
        )
    }
}

@Composable
private fun TaxYearSelector(
    year: Int,
    onYearChange: (Int) -> Unit,
) {
    val tc = LocalTruckColors.current
    val current = Calendar.getInstance().get(Calendar.YEAR)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = { onYearChange(year - 1) }) {
            Text("‹ ${year - 1}", color = tc.TextPrimary)
        }
        Text(
            text = year.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = tc.TextPrimary,
        )
        TextButton(
            onClick = { onYearChange(year + 1) },
            enabled = year < current,
        ) {
            Text("${year + 1} ›", color = if (year < current) tc.TextPrimary else tc.TextLabel)
        }
    }
}
