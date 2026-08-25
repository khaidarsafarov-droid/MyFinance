package com.truckerload.data.backup

import android.content.Context
import androidx.core.content.edit
import com.truckerload.data.preferences.AccountIds
import com.truckerload.data.preferences.AppLanguage
import com.truckerload.data.preferences.AppThemeMode
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.RpmThresholdsStore
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.preferences.WeeklyProfitGoalStore
import com.truckerload.domain.model.EquipmentType

/** Restores portable prefs from [BackupAppSettings] into the active account. */
object BackupPrefsApplier {

    suspend fun apply(context: Context, settings: BackupAppSettings?) {
        if (settings == null) return
        val app = context.applicationContext
        val store = SettingsDataStore(app)
        settings.themeModeOrdinal?.let {
            store.saveThemeMode(AppThemeMode.fromOrdinal(it))
        }
        settings.languageOrdinal?.let {
            store.saveLanguage(AppLanguage.fromOrdinal(it))
        }
        settings.reduceMotion?.let { store.saveReduceMotion(it) }
        settings.oledDark?.let { store.saveOledDark(it) }
        settings.dynamicColor?.let { store.saveDynamicColor(it) }
        settings.parserAutoUpdate?.let { store.saveParserAutoUpdate(it) }
        settings.parserPriceThresholdPercent?.let { store.saveParserPriceThreshold(it) }
        settings.quietHoursEnabled?.let { store.saveQuietHoursEnabled(it) }
        settings.quietHoursStart?.let { store.saveQuietHoursStart(it) }
        settings.quietHoursEnd?.let { store.saveQuietHoursEnd(it) }
        settings.notifyMissingWeek?.let { store.saveNotifyMissingWeek(it) }
        settings.notifyMaintenance?.let { store.saveNotifyMaintenance(it) }
        settings.lastEquipmentType?.let { name ->
            EquipmentType.fromStorage(name)?.let { store.saveLastEquipmentType(it) }
        }
        settings.telegramChatId?.let { store.saveTelegramChatId(it) }

        val userId = AuthStore(app).currentUserIdOrNull() ?: return
        settings.weeklyProfitGoal?.takeIf { it > 0 }?.let { goal ->
            WeeklyProfitGoalStore(app, userId).save(goal)
        }
        val min = settings.rpmMinProfit
        val target = settings.rpmTargetProfit
        if (min != null && target != null) {
            RpmThresholdsStore(app, userId).save(min, target)
        } else if (min != null || target != null) {
            // Partial restore: write prefs directly when only one side is present.
            val prefs = app.getSharedPreferences(
                "truckerload_rpm_${AccountIds.sanitizeFilePart(userId)}",
                Context.MODE_PRIVATE,
            )
            prefs.edit {
                min?.let { putFloat("min_profit_threshold", it.toFloat()) }
                target?.let { putFloat("target_profit_threshold", it.toFloat()) }
            }
        }
    }
}
