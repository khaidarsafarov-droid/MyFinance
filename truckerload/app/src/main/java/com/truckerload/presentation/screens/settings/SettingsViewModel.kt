package com.truckerload.presentation.screens.settings

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.preferences.TelegramTokenStore
import com.truckerload.data.repository.LoadRepository
import com.truckerload.sync.TelegramBotSyncEngine
import com.truckerload.utils.CsvExporter
import com.truckerload.utils.LoadExporter
import com.truckerload.utils.SoundManager
import com.truckerload.utils.VibrationManager
import com.truckerload.utils.LoadImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SettingsViewModel(
    private val loadRepository: LoadRepository,
    private val settingsDataStore: SettingsDataStore,
    private val appContext: Context
) : ViewModel() {

    sealed interface ExportState {
        data object Idle : ExportState
        data object Loading : ExportState
        data class Success(val file: File) : ExportState
        data class Error(val message: String) : ExportState
    }

    sealed interface RestoreState {
        data object Idle : RestoreState
        data object Loading : RestoreState
        data class Success(val imported: Int, val skipped: Int) : RestoreState
        data class Error(val message: String) : RestoreState
    }

    sealed interface SendTelegramState {
        data object Idle : SendTelegramState
        data object Loading : SendTelegramState
        data object Success : SendTelegramState
        data class Error(val message: String) : SendTelegramState
        data object NeedChatId : SendTelegramState
    }

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private val _restoreState = MutableStateFlow<RestoreState>(RestoreState.Idle)
    val restoreState: StateFlow<RestoreState> = _restoreState.asStateFlow()

    private val _sendTelegramState = MutableStateFlow<SendTelegramState>(SendTelegramState.Idle)
    val sendTelegramState: StateFlow<SendTelegramState> = _sendTelegramState.asStateFlow()

    val telegramChatId = settingsDataStore.telegramChatId.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    fun exportLoads() {
        viewModelScope.launch {
            _exportState.value = ExportState.Loading
            try {
                val loads = loadRepository.getAll()
                if (loads.isEmpty()) {
                    _exportState.value = ExportState.Error(ERROR_NO_LOADS)
                    return@launch
                }
                val file = LoadExporter.exportAllLoads(appContext, loads)
                _exportState.value = ExportState.Success(file)
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message.orEmpty().ifBlank { ERROR_GENERIC })
            }
        }
    }

    fun exportCsv() {
        viewModelScope.launch {
            _exportState.value = ExportState.Loading
            try {
                val loads = loadRepository.getAll()
                if (loads.isEmpty()) {
                    _exportState.value = ExportState.Error(ERROR_NO_LOADS)
                    return@launch
                }
                val file = CsvExporter.exportAllLoads(appContext, loads)
                _exportState.value = ExportState.Success(file)
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message.orEmpty().ifBlank { ERROR_GENERIC })
            }
        }
    }

    fun isSoundEnabled(): Boolean = SoundManager.isEnabled(appContext)

    fun isVibrationEnabled(): Boolean = VibrationManager.isEnabled(appContext)

    fun setSoundEnabled(enabled: Boolean) {
        SoundManager.setEnabled(appContext, enabled)
    }

    fun setVibrationEnabled(enabled: Boolean) {
        VibrationManager.setEnabled(appContext, enabled)
    }

    fun restoreLoadsFromUri(uri: Uri) {
        viewModelScope.launch {
            _restoreState.value = RestoreState.Loading
            try {
                val text = withContext(Dispatchers.IO) {
                    appContext.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader(Charsets.UTF_8).readText()
                    } ?: throw IllegalStateException("read_failed")
                }
                val result = LoadImporter.importFromText(loadRepository, text)
                if (result.parsed == 0) {
                    _restoreState.value = RestoreState.Error(ERROR_NO_PARSED_LOADS)
                    return@launch
                }
                _restoreState.value = RestoreState.Success(result.imported, result.skipped)
            } catch (e: Exception) {
                Log.e(TAG, "restoreLoadsFromUri failed", e)
                _restoreState.value = RestoreState.Error(e.message.orEmpty().ifBlank { ERROR_GENERIC })
            }
        }
    }

    fun sendExportToTelegram(file: File) {
        viewModelScope.launch {
            _sendTelegramState.value = SendTelegramState.Loading
            try {
                val chatId = settingsDataStore.getTelegramChatIdOnce()
                if (chatId == null) {
                    _sendTelegramState.value = SendTelegramState.NeedChatId
                    return@launch
                }
                val token = TelegramTokenStore(appContext).getToken()
                if (token.isBlank()) {
                    _sendTelegramState.value = SendTelegramState.Error(ERROR_NO_TOKEN)
                    return@launch
                }
                TelegramBotSyncEngine.sendFileToTelegram(appContext, token, chatId, file)
                    .onSuccess { _sendTelegramState.value = SendTelegramState.Success }
                    .onFailure { e ->
                        _sendTelegramState.value = SendTelegramState.Error(
                            e.message.orEmpty().ifBlank { ERROR_GENERIC }
                        )
                    }
            } catch (e: Exception) {
                _sendTelegramState.value = SendTelegramState.Error(
                    e.message.orEmpty().ifBlank { ERROR_GENERIC }
                )
            }
        }
    }

    fun saveTelegramChatId(raw: String) {
        viewModelScope.launch {
            val id = raw.trim().toLongOrNull()
            if (id != null) {
                settingsDataStore.saveTelegramChatId(id)
            }
        }
    }

    fun openExportsFolder(file: File) {
        LoadExporter.openExportsFolder(appContext, file)
    }

    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }

    fun resetRestoreState() {
        _restoreState.value = RestoreState.Idle
    }

    fun resetSendTelegramState() {
        _sendTelegramState.value = SendTelegramState.Idle
    }

    companion object {
        private const val TAG = "BackupRestore"
        const val ERROR_NO_LOADS = "no_loads"
        const val ERROR_NO_PARSED_LOADS = "no_parsed_loads"
        const val ERROR_NO_TOKEN = "no_token"
        const val ERROR_GENERIC = "failed"

        fun factory(loadRepository: LoadRepository, context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appContext = context.applicationContext
                    return SettingsViewModel(
                        loadRepository = loadRepository,
                        settingsDataStore = SettingsDataStore(appContext),
                        appContext = appContext
                    ) as T
                }
            }
    }
}
