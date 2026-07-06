package com.truckerload.data.preferences

import androidx.core.content.edit
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFS_NAME = "truckerload_settings"
private const val KEY_SELECTED_STATE = "selected_stats_state"
private const val DEFAULT_SELECTED_STATE = "KY"

class SelectedStateStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _selectedState = MutableStateFlow(readFromPrefs())
    val selectedState: StateFlow<String> = _selectedState.asStateFlow()

    fun current(): String = _selectedState.value

    fun save(code: String) {
        prefs.edit {putString(KEY_SELECTED_STATE, code)}
        _selectedState.value = code
    }

    private fun readFromPrefs(): String =
        prefs.getString(KEY_SELECTED_STATE, DEFAULT_SELECTED_STATE).orEmpty().ifBlank { DEFAULT_SELECTED_STATE }
}
