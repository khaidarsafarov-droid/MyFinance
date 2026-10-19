package com.truckerload.presentation.screens.settings
import kotlinx.coroutines.CancellationException

import android.database.sqlite.SQLiteException
import java.io.IOException


import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.preferences.SettingsScreenSnapshot
import com.truckerload.data.repository.LoadRepository
import com.truckerload.utils.CsvExporter
import com.truckerload.utils.LoadExporter
import com.truckerload.utils.SoundManager
import com.truckerload.utils.VibrationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val loadRepository: LoadRepository,
    settingsDataStore: SettingsDataStore,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    /**
     * Theme, quiet hours, notifications, and week start from one DataStore read.
     * Defaults match the previous per-flow initial values. Keys are unchanged.
     */
    val settingsSnapshot: StateFlow<SettingsScreenSnapshot> = settingsDataStore.screenSnapshot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsScreenSnapshot())

    sealed interface ExportState {
        data object Idle : ExportState
        data object Loading : ExportState
        data class Success(val file: File) : ExportState
        data class Error(val message: String) : ExportState
    }

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    fun exportCsv() {
        if (_exportState.value is ExportState.Loading) return
        viewModelScope.launch {
            _exportState.value = ExportState.Loading
            try {
                val loads = withContext(Dispatchers.IO) { loadRepository.getAll() }
                if (loads.isEmpty()) {
                    _exportState.value = ExportState.Error(ERROR_NO_LOADS)
                    return@launch
                }
                val file = CsvExporter.exportAllLoads(appContext, loads)
                _exportState.value = ExportState.Success(file)
            } catch (e: SQLiteException) {
                _exportState.value = ExportState.Error(e.message.orEmpty().ifBlank { ERROR_GENERIC })
            
            } catch (e: IOException) {
                _exportState.value = ExportState.Error(e.message.orEmpty().ifBlank { ERROR_GENERIC })
            
            } catch (e: IllegalArgumentException) {
                _exportState.value = ExportState.Error(e.message.orEmpty().ifBlank { ERROR_GENERIC })
            
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalStateException) {
                _exportState.value = ExportState.Error(e.message.orEmpty().ifBlank { ERROR_GENERIC })
            
            }
        }
    }

    fun isSoundEnabled(): Boolean = SoundManager.isEnabled(appContext)

    fun isVibrationEnabled(): Boolean = VibrationManager.isEnabled(appContext)

    fun setSoundEnabled(enabled: Boolean) {
        SoundManager.setEnabled(appContext, enabled)
        if (enabled) {
            SoundManager.preview(appContext)
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        VibrationManager.setEnabled(appContext, enabled)
        if (enabled) {
            VibrationManager.preview(appContext)
        }
    }

    fun openExportsFolder(file: File, host: Context = appContext) {
        LoadExporter.openExportsFolder(host, file)
    }

    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }

    companion object {
        const val ERROR_NO_LOADS = "no_loads"
        const val ERROR_GENERIC = "failed"
    }
}
