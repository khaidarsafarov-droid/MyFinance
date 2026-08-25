package com.truckerload.presentation.screens.diesel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.R
import com.truckerload.domain.importing.DieselImportAction
import com.truckerload.domain.importing.DieselImportReview
import com.truckerload.domain.importing.DieselImportUseCase
import com.truckerload.domain.importing.DieselSpreadsheetParser
import com.truckerload.widget.WidgetDataUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface DieselImportUiState {
    data object Idle : DieselImportUiState
    data object Loading : DieselImportUiState
    data class Review(val review: DieselImportReview, val isApplying: Boolean = false) : DieselImportUiState
    data class Success(val message: String) : DieselImportUiState
    data class Error(val message: String) : DieselImportUiState
}

@HiltViewModel
class DieselImportViewModel @Inject constructor(
    private val application: Application,
    private val importUseCase: DieselImportUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DieselImportUiState>(DieselImportUiState.Idle)
    val uiState: StateFlow<DieselImportUiState> = _uiState.asStateFlow()

    fun parseFile(uri: Uri) {
        if (_uiState.value is DieselImportUiState.Loading) return
        _uiState.value = DieselImportUiState.Loading
        viewModelScope.launch {
            runCatching {
                val fileName = resolveDisplayName(uri)
                val bytes = withContext(Dispatchers.IO) { readBytes(uri) }
                require(bytes.isNotEmpty()) { "empty_file" }
                val parsed = DieselSpreadsheetParser.parse(bytes, fileName)
                importUseCase.buildReview(parsed)
            }.onSuccess { review ->
                _uiState.value = DieselImportUiState.Review(review)
            }.onFailure { error ->
                _uiState.value = DieselImportUiState.Error(
                    mapError(error),
                )
            }
        }
    }

    fun apply(action: DieselImportAction) {
        val current = _uiState.value as? DieselImportUiState.Review ?: return
        _uiState.value = current.copy(isApplying = true)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    importUseCase.apply(current.review, action)
                    WidgetDataUpdater.updateWidgetData(application)
                }
                val msg = when (action) {
                    DieselImportAction.ADD_FROM_FILE ->
                        application.getString(R.string.diesel_import_added, current.review.importedPreview.size)
                    DieselImportAction.REPLACE_WEEK ->
                        application.getString(R.string.diesel_import_replaced, current.review.importedPreview.size)
                }
                msg
            }.onSuccess { message ->
                _uiState.value = DieselImportUiState.Success(message)
            }.onFailure { error ->
                _uiState.value = DieselImportUiState.Error(
                    application.getString(
                        R.string.diesel_import_error,
                        error.message ?: error.javaClass.simpleName,
                    ),
                )
            }
        }
    }

    fun dismiss() {
        _uiState.value = DieselImportUiState.Idle
    }

    fun clearSuccess() {
        if (_uiState.value is DieselImportUiState.Success) {
            _uiState.value = DieselImportUiState.Idle
        }
    }

    private fun mapError(error: Throwable): String = when (error.message) {
        "empty_workbook", "header_not_found", "no_fuel_rows", "empty_file", "week_unknown" ->
            application.getString(R.string.diesel_import_unreadable)
        else -> application.getString(
            R.string.diesel_import_error,
            error.message ?: error.javaClass.simpleName,
        )
    }

    private fun resolveDisplayName(uri: Uri): String {
        val fromProvider = runCatching {
            application.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
        }.getOrNull()
        return fromProvider ?: uri.lastPathSegment?.substringAfterLast('/').orEmpty()
    }

    private fun readBytes(uri: Uri): ByteArray {
        val stream = application.contentResolver.openInputStream(uri) ?: return ByteArray(0)
        return stream.use { it.readBytes().take(MAX_BYTES).toByteArray() }
    }

    companion object {
        private const val MAX_BYTES = 16 * 1024 * 1024
    }
}
