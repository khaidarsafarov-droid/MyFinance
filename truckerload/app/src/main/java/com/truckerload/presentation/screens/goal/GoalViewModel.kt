package com.truckerload.presentation.screens.goal

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.truckerload.data.preferences.WeeklyProfitGoalStore
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.goal.WeeklyGoalCalculator
import com.truckerload.domain.goal.WeeklyGoalProgress
import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.widget.WidgetDataUpdater
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeeklyGoalUiState(
    val progress: WeeklyGoalProgress? = null,
    val goalInput: String = "",
    val isEditingGoal: Boolean = false,
    val isSavingGoal: Boolean = false,
    val goalError: String? = null,
    val isLoading: Boolean = true
)

/**
 * Local-First goal tower: Actual Daily Yield (PU→DEL), без прогнозов.
 * Room → WeeklyGoalCalculator → UI + виджет (WorkManager + мгновенный refresh при insert).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GoalViewModel(
    private val loadRepository: LoadRepository,
    private val goalStore: WeeklyProfitGoalStore,
    private val appContext: Context
) : ViewModel() {

    /** Re-emit current reporting week; rolls over without a full-journal subscription. */
    private val currentWeekKey = flow {
        while (true) {
            emit(getCurrentWeekNumberAndYear())
            delay(60_000L)
        }
    }.distinctUntilChanged()

    /** Only the current week's loads — not the entire journal. */
    private val weekLoadsFlow = currentWeekKey
        .flatMapLatest { (week, year) -> loadRepository.getLoadsByWeek(week, year) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Цель на неделю (DataStore, сохраняется между сессиями). */
    val weeklyGoal: StateFlow<Double> = goalStore.goalAmount

    private val progressFlow = combine(
        currentWeekKey,
        weekLoadsFlow,
        goalStore.goalAmount,
        loadRepository.watchCurrentWeekYieldSnapshot(),
    ) { (week, year), loads, goal, sqlYield ->
        WeeklyGoalCalculator.calculate(goal, loads, week, year, sqlYield)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Текущий гросс за неделю из Room. */
    val currentGross: StateFlow<Double> = progressFlow
        .map { it?.currentGross ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    /** Прогресс 0.0 … 1.0 для графиков и виджета. */
    val progressPercentage: StateFlow<Float> = combine(currentGross, weeklyGoal) { current, goal ->
        if (goal <= 0) 0f else (current / goal).toFloat().coerceIn(0f, 1f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    /** Сколько гросса нужно в день до конца недели. */
    val dailyPaceNeeded: StateFlow<Double> = progressFlow
        .map { it?.dailyTargetNeeded ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val daysRemaining: StateFlow<Int> = progressFlow
        .map { it?.daysRemainingInWeek ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Фактический $/день (PU → DEL). */
    val actualDailyYield: StateFlow<Double> = progressFlow
        .map { it?.actualDailyYield ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val goalProgress: StateFlow<WeeklyGoalProgress?> = progressFlow

    private val _uiState = MutableStateFlow(WeeklyGoalUiState())
    val uiState: StateFlow<WeeklyGoalUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            progressFlow.collect { progress ->
                _uiState.update {
                    it.copy(
                        progress = progress,
                        goalInput = if (it.isEditingGoal) it.goalInput
                        else progress?.let { p -> formatGoalInput(p.targetAmount) }.orEmpty(),
                        isLoading = progress == null
                    )
                }
            }
        }
    }

    fun startEditingGoal() {
        val current = _uiState.value.progress?.targetAmount ?: goalStore.getGoal()
        _uiState.update {
            it.copy(isEditingGoal = true, goalInput = formatGoalInput(current), goalError = null)
        }
    }

    fun cancelEditingGoal() {
        val current = _uiState.value.progress?.targetAmount ?: goalStore.getGoal()
        _uiState.update {
            it.copy(isEditingGoal = false, goalInput = formatGoalInput(current), goalError = null)
        }
    }

    fun onGoalInputChange(value: String) {
        _uiState.update {
            it.copy(goalInput = WeeklyGoalInputValidator.sanitize(value), goalError = null)
        }
    }

    fun saveGoal() {
        val parsed = WeeklyGoalInputValidator.parseGoalAmount(_uiState.value.goalInput)
        if (parsed == null) {
            _uiState.update { it.copy(goalError = appContext.getString(com.truckerload.R.string.goal_error_enter_amount)) }
            return
        }
        _uiState.update { it.copy(isSavingGoal = true, goalError = null) }
        viewModelScope.launch {
            goalStore.save(parsed).fold(
                onSuccess = {
                    WidgetDataUpdater.updateWidgetData(appContext)
                    _uiState.update {
                        it.copy(isSavingGoal = false, isEditingGoal = false, goalError = null)
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isSavingGoal = false, goalError = e.message)
                    }
                }
            )
        }
    }

    private fun formatGoalInput(amount: Double): String =
        when {
            amount <= 0 -> ""
            amount % 1.0 == 0.0 -> amount.toLong().toString()
            else -> amount.toString()
        }

    class Factory(
        private val loadRepository: LoadRepository,
        private val goalStore: WeeklyProfitGoalStore,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GoalViewModel(loadRepository, goalStore, context.applicationContext) as T
        }
    }
}
