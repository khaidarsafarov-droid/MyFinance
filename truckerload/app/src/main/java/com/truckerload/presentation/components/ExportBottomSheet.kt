package com.truckerload.presentation.components

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.di.LocalDieselRepository
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.di.LocalPaycheckRepository
import com.truckerload.presentation.di.LocalWeekRepository
import com.truckerload.presentation.screens.stats.StatsPeriod
import com.truckerload.utils.ExportService
import com.truckerload.utils.ReportGeneratorService
import com.truckerload.utils.getMonthRange
import com.truckerload.utils.getWeekRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Параметры периода для экспорта (соответствуют текущим фильтрам статистики). */
data class ExportPeriodParams(
    val statsPeriod: StatsPeriod,
    val weekNumber: Int,
    val year: Int,
    val calendarMonth: Int,
    val calendarYear: Int,
    val periodLabel: String,
    val grossRevenue: Double,
    val totalMiles: Double,
    val avgRpm: Double,
    val totalLoads: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
    year: Int,
    exportPeriodParams: ExportPeriodParams?,
    onDismiss: () -> Unit,
    onExported: (uri: android.net.Uri?, mimeType: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val weekRepo = LocalWeekRepository.current
    val loadRepo = LocalLoadRepository.current
    val paycheckRepo = LocalPaycheckRepository.current
    val dieselRepo = LocalDieselRepository.current

    fun shareAndNotify(uri: android.net.Uri, mimeType: String, displayPath: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.export_share_chooser)))
        android.widget.Toast.makeText(context, context.getString(R.string.export_saved_to_folder, displayPath), android.widget.Toast.LENGTH_LONG).show()
        onExported(uri, mimeType)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(stringResource(R.string.export_title), style = MaterialTheme.typography.titleLarge)
            Text(
                text = exportPeriodParams?.let { stringResource(R.string.export_period_label, it.periodLabel) }
                    ?: stringResource(R.string.export_year_label, year),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Button(
                onClick = {
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            ExportService(context, weekRepo, loadRepo, paycheckRepo, dieselRepo).exportCsvToStorage(year)
                        }
                        result?.let { shareAndNotify(it.uri, "text/csv", it.displayPath) }
                            ?: android.widget.Toast.makeText(context, context.getString(R.string.export_csv_error), android.widget.Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text(stringResource(R.string.export_csv_button))
            }

            Button(
                onClick = {
                    val params = exportPeriodParams
                    if (params == null) return@Button
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            val (startDate, endDate) = when (params.statsPeriod) {
                                StatsPeriod.WEEK -> {
                                    val (s, e, _) = getWeekRange(params.weekNumber, params.year)
                                    Pair(s, e)
                                }
                                StatsPeriod.MONTH -> getMonthRange(params.calendarMonth, params.calendarYear)
                                StatsPeriod.YEAR -> Pair(
                                    "%04d-01-01".format(params.calendarYear),
                                    "%04d-12-31".format(params.calendarYear)
                                )
                            }
                            val loads = loadRepo.getLoadsByDateRangeOnce(startDate, endDate)
                            val driverName = paycheckRepo.getAllPaychecksOnce()
                                .lastOrNull { it.driverName?.isNotBlank() == true }?.driverName

                            ReportGeneratorService(context).generatePdfAndSaveToStorage(
                                ReportGeneratorService.ReportParams(
                                    periodLabel = params.periodLabel,
                                    startDate = startDate,
                                    endDate = endDate,
                                    driverName = driverName,
                                    grossRevenue = params.grossRevenue,
                                    totalMiles = params.totalMiles,
                                    avgRpm = params.avgRpm,
                                    totalLoads = params.totalLoads,
                                    loads = loads
                                )
                            )
                        }
                        result?.let { shareAndNotify(it.uri, "application/pdf", it.displayPath) }
                            ?: android.widget.Toast.makeText(context, context.getString(R.string.export_pdf_error), android.widget.Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                enabled = exportPeriodParams != null
            ) {
                Text(stringResource(R.string.export_pdf_button))
            }
        }
    }
}
