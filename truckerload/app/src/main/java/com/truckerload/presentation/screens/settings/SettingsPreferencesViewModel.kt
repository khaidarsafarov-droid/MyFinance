package com.truckerload.presentation.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.preferences.AppLanguage
import com.truckerload.data.preferences.AppThemeMode
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.repository.WeekStartRebind
import com.truckerload.domain.units.DisplayUnits
import com.truckerload.domain.units.DistanceUnit
import com.truckerload.domain.units.DistanceUnitPolicy
import com.truckerload.domain.week.WeekStartDay
import com.truckerload.domain.week.WeekStartRuntime
import com.truckerload.presentation.theme.ThemeManager
import com.truckerload.sync.MaintenanceReminderCoordinator
import com.truckerload.utils.AppLanguageManager
import com.truckerload.utils.BackupService
import com.truckerload.widget.WidgetRefresh
import com.truckerload.widget.WidgetUpdateWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Write side of the settings sections: each setter persists the value and runs the side effects
 * (widgets, reminders, week rebind) that used to live in the composables.
 */
@HiltViewModel
class SettingsPreferencesViewModel @Inject constructor(
    private val settings: SettingsDataStore,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    fun setNotifyMissingWeek(enabled: Boolean) {
        viewModelScope.launch { settings.saveNotifyMissingWeek(enabled) }
    }

    fun setNotifyMaintenance(enabled: Boolean) {
        viewModelScope.launch {
            settings.saveNotifyMaintenance(enabled)
            MaintenanceReminderCoordinator.sync(appContext)
        }
    }

    fun setQuietHoursEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.saveQuietHoursEnabled(enabled) }
    }

    fun setQuietHoursStart(hour: Int) {
        viewModelScope.launch { settings.saveQuietHoursStart(hour) }
    }

    fun setQuietHoursEnd(hour: Int) {
        viewModelScope.launch { settings.saveQuietHoursEnd(hour) }
    }

    fun setReduceMotion(enabled: Boolean) {
        viewModelScope.launch { settings.saveReduceMotion(enabled) }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            settings.saveThemeMode(mode)
            ThemeManager.apply(mode)
            WidgetRefresh.refreshAndUpdateAsync(appContext)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settings.saveDynamicColor(enabled)
            WidgetRefresh.refreshAndUpdateAsync(appContext)
        }
    }

    fun setOledDark(enabled: Boolean) {
        viewModelScope.launch { settings.saveOledDark(enabled) }
    }

    fun setLoadsWeekStart(day: WeekStartDay) {
        viewModelScope.launch {
            WeekStartRuntime.installLoads(day)
            settings.saveLoadWeekStartDay(day)
            rebindAfterWeekStartChange()
        }
    }

    fun setDieselWeekStart(day: WeekStartDay) {
        viewModelScope.launch {
            WeekStartRuntime.installDiesel(day)
            settings.saveDieselWeekStartDay(day)
            rebindAfterWeekStartChange()
        }
    }

    private suspend fun rebindAfterWeekStartChange() {
        withContext(Dispatchers.IO) {
            WeekStartRebind.rebindIfPossible(appContext)
            BackupService.scheduleCreateAutoBackup(appContext)
        }
        WidgetUpdateWorker.refreshNow(appContext)
    }

    fun setLanguage(code: String) {
        viewModelScope.launch {
            settings.saveLanguage(AppLanguage.fromTag(code))
            AppLanguageManager.setLanguage(code)
            val explicit = settings.getExplicitDistanceUnitOnce()
            DisplayUnits.install(DistanceUnitPolicy.resolve(explicit, code))
            WidgetRefresh.refreshAndUpdateAsync(appContext)
        }
    }

    /** Persists [unit] unless it is already the explicit choice; calls [onApplied] either way. */
    fun setDistanceUnit(unit: DistanceUnit, onApplied: () -> Unit) {
        viewModelScope.launch {
            if (settings.getExplicitDistanceUnitOnce() != unit) {
                settings.saveDistanceUnit(unit)
                DisplayUnits.install(unit)
                WidgetRefresh.refreshAndUpdateAsync(appContext)
            }
            onApplied()
        }
    }
}
