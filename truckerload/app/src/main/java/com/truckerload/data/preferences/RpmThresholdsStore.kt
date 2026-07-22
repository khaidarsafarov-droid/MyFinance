package com.truckerload.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.truckerload.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val LEGACY_PREFS_NAME = "truckerload_settings"
private const val KEY_MIN_PROFIT = "min_profit_threshold"
private const val KEY_TARGET_PROFIT = "target_profit_threshold"
private const val DEFAULT_MIN = 2.00
private const val DEFAULT_TARGET = 2.50

data class RpmThresholds(
    val minProfit: Double,
    val targetProfit: Double
)

class RpmThresholdsStore(
    context: Context,
    userId: String = AuthStore(context).currentUserIdOrNull() ?: AccountIds.LOCAL_DEV,
) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(
            "truckerload_rpm_${AccountIds.sanitizeFilePart(userId)}",
            Context.MODE_PRIVATE,
        ).also { scoped ->
            migrateFromLegacyIfEmpty(appContext, scoped)
        }

    private val _thresholds = MutableStateFlow(loadFromPrefs())
    val thresholds: StateFlow<RpmThresholds> = _thresholds.asStateFlow()

    private fun loadFromPrefs(): RpmThresholds = RpmThresholds(
        minProfit = prefs.getFloat(KEY_MIN_PROFIT, DEFAULT_MIN.toFloat()).toDouble(),
        targetProfit = prefs.getFloat(KEY_TARGET_PROFIT, DEFAULT_TARGET.toFloat()).toDouble()
    )

    fun save(minProfit: Double, targetProfit: Double): Result<Unit> {
        val t = RpmThresholds(minProfit, targetProfit)
        val validationError = when {
            t.minProfit < 0 || t.targetProfit < 0 -> appContext.getString(R.string.settings_rpm_threshold_negative)
            t.minProfit > t.targetProfit -> appContext.getString(R.string.settings_rpm_threshold_order)
            else -> null
        }
        validationError?.let { return Result.failure(IllegalArgumentException(it)) }
        prefs.edit {
            putFloat(KEY_MIN_PROFIT, minProfit.toFloat())
            putFloat(KEY_TARGET_PROFIT, targetProfit.toFloat())
        }
        _thresholds.value = t
        return Result.success(Unit)
    }

    companion object {
        private fun migrateFromLegacyIfEmpty(context: Context, scoped: SharedPreferences) {
            if (scoped.contains(KEY_MIN_PROFIT)) return
            val legacy = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
            if (!legacy.contains(KEY_MIN_PROFIT)) return
            scoped.edit {
                putFloat(KEY_MIN_PROFIT, legacy.getFloat(KEY_MIN_PROFIT, DEFAULT_MIN.toFloat()))
                putFloat(KEY_TARGET_PROFIT, legacy.getFloat(KEY_TARGET_PROFIT, DEFAULT_TARGET.toFloat()))
            }
        }
    }
}
