package com.truckerload.presentation.screens.goal

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.truckerload.data.preferences.WeeklyProfitGoalStore
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.goal.DayPlanRow
import com.truckerload.domain.goal.WeeklyDayPlan
import com.truckerload.domain.goal.WeeklyGoalCalculator
import com.truckerload.domain.money.Money
import com.truckerload.domain.goal.ClosedWeekGross
import com.truckerload.domain.goal.WeeklyGoalProgress
import com.truckerload.domain.ux.UxMotivation
import com.truckerload.utils.getPreviousWeekNumberAndYear
import com.truckerload.utils.getWeekRange
import com.truckerload.widget.WidgetDataUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class WeeklyGoalUiState(
    val progress: WeeklyGoalProgress? = null,
    val goalInput: String = "",
    val isEditingGoal: Boolean = false,
    val isSavingGoal: Boolean = false,
    val goalError: String? = null,
    val isLoading: Boolean = true,
    val suggestedGoals: List<Double> = UxMotivation.suggestedWeeklyGoals(),
    /** Null until the enter transition has settled and the day query returns. */
    val dayPlan: List<DayPlanRow>? = null,
)

/**
 * Local-First goal tower: Actual Daily Yield (PU→DEL), без прогнозов.
 * Room → WeeklyGoalCalculator → UI + виджет (WorkManager + мгновенный refresh при insert).
 */
@HiltViewModel
class GoalViewModel @Inject constructor(
    private val loadRepository: LoadRepository,
    private val goalStore: WeeklyProfitGoalStore,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    /** Цель на неделю (DataStore, сохраняется между сессиями). */
    val weeklyGoal: StateFlow<Double> = goalStore.goalAmount

    /**
     * One journal subscription. Current-week progress and closed-week gross are
     * both derived from that list. Sharing stops five seconds after the last
     * collector leaves; the day-plan query stays behind [onMotionSettled].
     */
    private val goalFeed: StateFlow<GoalFeed> = combine(
        loadRepository.watchLoads(),
        goalStore.goalAmount,
        loadRepository.watchCurrentWeekYieldSnapshot(),
    ) { loads, goal, sqlYield ->
        val (closedWeek, closedYear) = getPreviousWeekNumberAndYear()
        GoalFeed(
            progress = WeeklyGoalCalculator.calculateCurrentWeek(
                Money.fromDollars(goal),
                loads,
                sqlYield,
            ),
            closedWeekGross = ClosedWeekGross.dollars(loads, closedWeek, closedYear),
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalFeed())

    /** Текущий гросс за неделю из Room. */
    val currentGross: StateFlow<Double> = goalFeed
        .map { it.progress?.currentGross?.cents?.div(100.0) ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    /** Прогресс 0.0 … 1.0 для графиков и виджета. */
    val progressPercentage: StateFlow<Float> = combine(currentGross, weeklyGoal) { current, goal ->
        if (goal <= 0) 0f else (current / goal).toFloat().coerceIn(0f, 1f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    /** Сколько гросса нужно в день до конца недели. */
    val dailyPaceNeeded: StateFlow<Double> = goalFeed
        .map { it.progress?.dailyTargetNeeded?.cents?.div(100.0) ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val daysRemaining: StateFlow<Int> = goalFeed
        .map { it.progress?.daysRemainingInWeek ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Фактический $/день (PU → DEL). */
    val actualDailyYield: StateFlow<Double> = goalFeed
        .map { it.progress?.actualDailyYield?.cents?.div(100.0) ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val goalProgress: StateFlow<WeeklyGoalProgress?> = goalFeed
        .map { it.progress }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _uiState = MutableStateFlow(WeeklyGoalUiState())
    val uiState: StateFlow<WeeklyGoalUiState> = _uiState.asStateFlow()

    /** Stays false during the tab transition so the day query does not start on the first frame. */
    private val motionReady = MutableStateFlow(false)

    fun onMotionSettled() {
        motionReady.value = true
    }

    init {
        viewModelScope.launch {
            goalFeed.collect { feed ->
                val progress = feed.progress
                _uiState.update {
                    it.copy(
                        progress = progress,
                        goalInput = if (it.isEditingGoal) it.goalInput
                        else progress?.let { p -> formatGoalInput(p.targetAmount.cents / 100.0) }.orEmpty(),
                        isLoading = progress == null,
                        suggestedGoals = UxMotivation.suggestedWeeklyGoals(feed.closedWeekGross),
                    )
                }
            }
        }
        viewModelScope.launch {
            combine(motionReady, goalFeed) { ready, feed ->
                val progress = feed.progress
                if (!ready || progress == null) {
                    null
                } else {
                    DayPlanRequest(
                        weekNumber = progress.weekNumber,
                        year = progress.year,
                        grossCents = progress.currentGross.cents,
                        dailyNeeded = progress.dailyTargetNeeded,
                    )
                }
            }.distinctUntilChanged().collectLatest { request ->
                if (request == null) return@collectLatest
                val plan = withContext(Dispatchers.IO) {
                    val (start, end, _) = getWeekRange(request.weekNumber, request.year)
                    val grossByDay = runCatching {
                        loadRepository.dailyGrossCentsByWeekday(start, end)
                    }.getOrDefault(emptyMap())
                    WeeklyDayPlan.rows(
                        weekStart = LocalDate.parse(start),
                        today = LocalDate.now(),
                        grossCentsBySqliteDow = grossByDay,
                        dailyNeeded = request.dailyNeeded,
                    )
                }
                _uiState.update { it.copy(dayPlan = plan) }
            }
        }
    }

    fun startEditingGoal() {
        val current = _uiState.value.progress?.targetAmount?.let { it.cents / 100.0 } ?: goalStore.getGoal()
        _uiState.update {
            it.copy(isEditingGoal = true, goalInput = formatGoalInput(current), goalError = null)
        }
    }

    fun applySuggestedGoal(amount: Double) {
        _uiState.update {
            it.copy(
                isEditingGoal = true,
                goalInput = formatGoalInput(amount),
                goalError = null,
            )
        }
    }

    fun cancelEditingGoal() {
        val current = _uiState.value.progress?.targetAmount?.let { it.cents / 100.0 } ?: goalStore.getGoal()
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

    private data class GoalFeed(
        val progress: WeeklyGoalProgress? = null,
        val closedWeekGross: Double = 0.0,
    )

    private data class DayPlanRequest(
        val weekNumber: Int,
        val year: Int,
        val grossCents: Long,
        val dailyNeeded: Money,
    )

    private fun formatGoalInput(amount: Double): String =
        when {
            amount <= 0 -> ""
            amount % 1.0 == 0.0 -> amount.toLong().toString()
            else -> amount.toString()
        }

}
