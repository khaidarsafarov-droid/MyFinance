package com.truckerload.presentation.screens.home

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.R
import com.truckerload.data.preferences.WeeklyProfitGoalStore
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.MiscExpenseRepository
import com.truckerload.domain.model.Load
import com.truckerload.domain.money.Money
import com.truckerload.domain.report.PeriodReportAssembler
import com.truckerload.domain.report.PeriodReportBuilder
import com.truckerload.domain.report.PeriodReportKind
import com.truckerload.domain.report.PeriodReportSelection
import com.truckerload.domain.report.PeriodReportWindow
import com.truckerload.domain.report.PeriodReportWindowResolver
import com.truckerload.utils.PeriodReportFormat
import com.truckerload.utils.PeriodReportReady
import com.truckerload.utils.PeriodReportShare
import com.truckerload.utils.logFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class PeriodReportViewModel @Inject constructor(
    private val loadRepository: LoadRepository,
    private val dieselRepository: DieselRepository,
    private val expenseRepository: MiscExpenseRepository,
    private val goalStore: WeeklyProfitGoalStore,
    private val app: Application,
) : ViewModel() {

    private val sharing = AtomicBoolean(false)
    private val _shareReady = MutableStateFlow<PeriodReportReady?>(null)
    val shareReady: StateFlow<PeriodReportReady?> = _shareReady.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun share(state: HomeUiState, driverName: String, format: PeriodReportFormat) {
        if (!sharing.compareAndSet(false, true)) return
        viewModelScope.launch {
            _error.value = null
            runCatching {
                withContext(Dispatchers.IO) { prepare(state, driverName, format) }
            }.onSuccess { ready ->
                _shareReady.value = ready
            }.onFailure { error ->
                logFailure("PeriodReport", "share failed", error)
                _error.value = app.getString(R.string.period_report_failed)
            }
            sharing.set(false)
        }
    }

    fun consumeShare() {
        _shareReady.value = null
    }

    fun consumeError() {
        _error.value = null
    }

    private suspend fun prepare(
        state: HomeUiState,
        driverName: String,
        format: PeriodReportFormat,
    ): PeriodReportReady {
        val locale = Locale.getDefault()
        val numbers = periodReportNumbers(locale)
        val labels = periodReportLabels(app)
        val window = PeriodReportWindowResolver.resolve(state.toPeriodSelection())
        val periodLabel = PeriodReportWindowResolver.label(
            window = window,
            locale = locale,
            formatDate = numbers.date,
            templates = periodReportTemplates(app),
        )
        val goal = if (window.includeWeeklyGoal) Money.fromDollars(goalStore.getGoal()) else null
        val draft = PeriodReportAssembler.assemble(
            loads = loadsFor(window),
            diesel = if (window.includeFuel) dieselRepository.getAllDieselOnce() else emptyList(),
            expenses = if (window.includeExpenses) expenseRepository.getAllOnce() else emptyList(),
            window = window,
            driverName = driverName,
            periodLabel = periodLabel,
            generatedOn = LocalDate.now(),
            locale = locale,
            goal = goal,
            labels = labels,
            numbers = numbers,
        )
        val report = PeriodReportBuilder.build(draft, labels, numbers)
        val subject = "${labels.appName} — ${labels.title}"
        return when (format) {
            PeriodReportFormat.TEXT -> PeriodReportReady(
                file = null,
                mime = PeriodReportShare.MIME_TEXT,
                subject = subject,
                body = report.plainText,
            )
            PeriodReportFormat.PDF -> {
                val file = PeriodReportShare.writePdf(app, report, fileKey(window))
                val caption = app.getString(
                    R.string.period_report_caption,
                    labels.appName,
                    labels.title,
                    periodLabel,
                )
                PeriodReportReady(
                    file = file,
                    mime = PeriodReportShare.MIME_PDF,
                    subject = subject,
                    body = caption,
                )
            }
        }
    }

    private suspend fun loadsFor(window: PeriodReportWindow): List<Load> = when (window.kind) {
        PeriodReportKind.WEEK ->
            loadRepository.getLoadsByWeek(requireNotNull(window.weekNumber), requireNotNull(window.weekYear)).first()
        PeriodReportKind.MONTH, PeriodReportKind.DAY ->
            loadRepository.getLoadsOverlappingRange(
                requireNotNull(window.start).toString(),
                requireNotNull(window.end).toString(),
            ).first()
        PeriodReportKind.DISPUTES ->
            loadRepository.getAllLoadsOnce().filter { it.isDispute }
        PeriodReportKind.ALL -> {
            val start = window.start
            val end = window.end
            if (start != null && end != null) {
                loadRepository.getLoadsByDateRangeOnce(start.toString(), end.toString())
            } else {
                loadRepository.getAllLoadsOnce()
            }
        }
    }

    private fun fileKey(window: PeriodReportWindow): String = when (window.kind) {
        PeriodReportKind.WEEK -> "w${window.weekNumber}_${window.weekYear}"
        PeriodReportKind.MONTH -> window.start?.toString()?.take(7) ?: "month"
        PeriodReportKind.DAY -> window.start?.toString() ?: "day"
        PeriodReportKind.DISPUTES -> "disputes"
        PeriodReportKind.ALL -> window.calendarYear?.toString() ?: "all"
    }
}

private fun HomeUiState.toPeriodSelection(): PeriodReportSelection = PeriodReportSelection(
    filter = filter,
    selectedDate = selectedDate,
    selectedDateLabel = selectedDateLabel,
    selectedWeekStart = selectedWeekStart,
    selectedMonthYear = selectedMonthYear,
    selectedMonth = selectedMonth,
    selectedMonthLabel = selectedMonthLabel,
    selectedYear = selectedYear,
)
