package com.truckerload.presentation.screens.analytics

import android.app.Application
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.truckerload.R
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.repository.AnalyticsDashboard
import com.truckerload.data.repository.AnalyticsRepository
import com.truckerload.data.repository.social.ProfileRepository
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.analytics.AnalyticsFilter
import com.truckerload.domain.model.analytics.AnalyticsPeriod
import com.truckerload.domain.model.analytics.AnalyticsSummary
import com.truckerload.domain.model.analytics.DailyData
import com.truckerload.domain.model.analytics.PeriodFinance
import com.truckerload.domain.model.analytics.RouteData
import com.truckerload.domain.model.analytics.WeekData
import com.truckerload.utils.AnalyticsExportShare
import com.truckerload.utils.AnalyticsOwnerName
import com.truckerload.utils.AnalyticsShareFormat
import com.truckerload.utils.analyticsExportLabels
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

data class AnalyticsShareReady(
    val file: File,
    val format: AnalyticsShareFormat,
    val caption: String,
)

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val filter: AnalyticsFilter = AnalyticsFilter.DEFAULT,
    val weeks: List<WeekData> = emptyList(),
    val routes: List<RouteData> = emptyList(),
    val daily: List<DailyData> = emptyList(),
    val summary: AnalyticsSummary? = null,
    val finance: PeriodFinance = PeriodFinance(),
    val selectedWeekIndex: Int? = null,
    val selectedWeekLoads: List<Load> = emptyList(),
    val ownerGivenName: String = "",
    val ownerFamilyName: String = "",
    val shareReady: AnalyticsShareReady? = null,
    val error: String? = null,
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val repository: AnalyticsRepository,
    private val profileRepository: ProfileRepository,
    private val userProfileStore: UserProfileStore,
    private val app: Application,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private var lastDashboard: AnalyticsDashboard? = null
    private var refreshJob: Job? = null
    private val refreshGeneration = AtomicInteger(0)
    private var weekLoadJob: Job? = null

    init {
        refresh()
        viewModelScope.launch {
            combine(
                userProfileStore.profile,
                profileRepository.watchMyProfile(),
            ) { user, social ->
                AnalyticsOwnerName.fromProfile(
                    givenName = user?.givenName,
                    familyName = user?.familyName,
                    email = user?.email,
                    socialDisplayName = social.displayName,
                )
            }.collect { (given, family) ->
                _uiState.update { it.copy(ownerGivenName = given, ownerFamilyName = family) }
            }
        }
    }

    fun setPeriod(period: AnalyticsPeriod) {
        applyFilter(_uiState.value.filter.selectPreset(period))
    }

    fun selectYear(year: Int) {
        applyFilter(_uiState.value.filter.selectYear(year))
    }

    fun selectMonth(month: Int) {
        applyFilter(_uiState.value.filter.selectMonth(month))
    }

    fun selectCalendarWeek(weekNumber: Int, weekYear: Int) {
        applyFilter(_uiState.value.filter.selectWeek(weekNumber, weekYear))
    }

    private fun applyFilter(filter: AnalyticsFilter) {
        if (filter == _uiState.value.filter) return
        _uiState.update { it.copy(filter = filter) }
        refresh()
    }

    fun selectWeek(index: Int) {
        val week = _uiState.value.weeks.getOrNull(index) ?: return
        _uiState.update { it.copy(selectedWeekIndex = index) }
        weekLoadJob?.cancel()
        weekLoadJob = viewModelScope.launch {
            runCatching {
                repository.getLoadsForWeek(week.weekNumber, week.year)
            }.onSuccess { loads ->
                if (_uiState.value.selectedWeekIndex != index) return@launch
                _uiState.update { it.copy(selectedWeekLoads = loads) }
            }
        }
    }

    fun refresh() {
        refreshJob?.cancel()
        val generation = refreshGeneration.incrementAndGet()
        val filter = _uiState.value.filter
        refreshJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                repository.loadDashboard(filter)
            }.onSuccess { dashboard ->
                if (generation != refreshGeneration.get()) return@launch
                lastDashboard = dashboard
                val defaultIndex = dashboard.weeks.lastIndex.takeIf { idx -> idx >= 0 }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        weeks = dashboard.weeks,
                        routes = dashboard.routes,
                        daily = dashboard.daily,
                        summary = dashboard.summary,
                        finance = dashboard.finance,
                        selectedWeekIndex = defaultIndex,
                        selectedWeekLoads = emptyList(),
                    )
                }
                defaultIndex?.let { selectWeek(it) }
            }.onFailure { e ->
                if (generation != refreshGeneration.get()) return@launch
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: app.getString(R.string.analytics_error_loading))
                }
            }
        }
    }

    fun shareAnalytics(format: AnalyticsShareFormat, givenName: String, familyName: String) {
        val dashboard = lastDashboard ?: return
        viewModelScope.launch {
            val given = givenName.trim()
            val family = familyName.trim()
            runCatching { profileRepository.updateOwnName(given, family) }
            val owner = AnalyticsOwnerName.display(given, family)
            val filter = _uiState.value.filter
            val labels = analyticsExportLabels(app, filter, owner)
            runCatching {
                AnalyticsExportShare.writeReport(app, dashboard, labels, format, filter.exportKey())
            }.onSuccess { file ->
                val caption = if (owner.isBlank()) {
                    app.getString(
                        R.string.analytics_share_caption,
                        labels.appName,
                        labels.title,
                        labels.period,
                    )
                } else {
                    app.getString(
                        R.string.analytics_share_caption_named,
                        labels.appName,
                        labels.title,
                        labels.period,
                        owner,
                    )
                }
                _uiState.update {
                    it.copy(
                        shareReady = AnalyticsShareReady(
                            file = file,
                            format = format,
                            caption = caption,
                        ),
                        ownerGivenName = given,
                        ownerFamilyName = family,
                        error = null,
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        shareReady = null,
                        error = app.getString(R.string.analytics_share_failed),
                    )
                }
            }
        }
    }

    fun clearShareReady() {
        _uiState.update { it.copy(shareReady = null) }
    }

}
