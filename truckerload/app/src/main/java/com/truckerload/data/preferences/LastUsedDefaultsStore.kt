package com.truckerload.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Smart defaults: last-used diesel / paycheck amounts survive across sessions.
 */
class LastUsedDefaultsStore(
    context: Context,
    userId: String = AuthStore(context).currentUserIdOrNull() ?: AccountIds.LOCAL_DEV,
) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(
            "truckerload_last_defaults_${AccountIds.sanitizeFilePart(userId)}",
            Context.MODE_PRIVATE,
        )

    private val _lastDieselAmount = MutableStateFlow(prefs.getString(KEY_DIESEL, null)?.toDoubleOrNull())
    val lastDieselAmount: StateFlow<Double?> = _lastDieselAmount.asStateFlow()

    private val _lastPaycheckAmount = MutableStateFlow(prefs.getString(KEY_PAYCHECK, null)?.toDoubleOrNull())
    val lastPaycheckAmount: StateFlow<Double?> = _lastPaycheckAmount.asStateFlow()

    fun saveDieselAmount(amount: Double) {
        if (amount <= 0) return
        prefs.edit(commit = true) { putString(KEY_DIESEL, amount.toString()) }
        _lastDieselAmount.value = amount
    }

    fun savePaycheckAmount(amount: Double) {
        if (amount <= 0) return
        prefs.edit(commit = true) { putString(KEY_PAYCHECK, amount.toString()) }
        _lastPaycheckAmount.value = amount
    }

    companion object {
        private const val KEY_DIESEL = "last_diesel_amount"
        private const val KEY_PAYCHECK = "last_paycheck_amount"
    }
}
