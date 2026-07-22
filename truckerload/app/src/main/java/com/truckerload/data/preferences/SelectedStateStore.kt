package com.truckerload.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val LEGACY_PREFS_NAME = "truckerload_settings"
private const val KEY_SELECTED_STATE = "selected_stats_state"
private const val DEFAULT_SELECTED_STATE = "KY"

class SelectedStateStore(
    context: Context,
    userId: String = AuthStore(context).currentUserIdOrNull() ?: AccountIds.LOCAL_DEV,
) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(
            "truckerload_selected_state_${AccountIds.sanitizeFilePart(userId)}",
            Context.MODE_PRIVATE,
        ).also { scoped ->
            if (!scoped.contains(KEY_SELECTED_STATE)) {
                val legacy = context.applicationContext
                    .getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
                val value = legacy.getString(KEY_SELECTED_STATE, null)
                if (!value.isNullOrBlank()) {
                    scoped.edit { putString(KEY_SELECTED_STATE, value) }
                }
            }
        }

    private val _selectedState = MutableStateFlow(readFromPrefs())
    val selectedState: StateFlow<String> = _selectedState.asStateFlow()

    fun current(): String = _selectedState.value

    fun save(code: String) {
        prefs.edit { putString(KEY_SELECTED_STATE, code) }
        _selectedState.value = code
    }

    private fun readFromPrefs(): String =
        prefs.getString(KEY_SELECTED_STATE, DEFAULT_SELECTED_STATE).orEmpty()
            .ifBlank { DEFAULT_SELECTED_STATE }
}
