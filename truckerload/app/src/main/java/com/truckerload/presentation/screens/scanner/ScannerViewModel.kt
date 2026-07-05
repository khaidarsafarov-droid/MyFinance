package com.truckerload.presentation.screens.scanner

import android.content.Context
import android.content.Intent
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.truckerload.data.repository.ScanRepository
import com.truckerload.utils.OCRService
import com.truckerload.utils.PDFGenerator
import com.truckerload.utils.StorageHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.io.File

data class PendingScan(
    val file: File,
    val pageCount: Int,
    val ocrText: String,
    val timestamp: Long,
    val usedRussianEngine: Boolean = false,
    val savedToDb: Boolean = false,
)

data class ScannerUiState(
    val isProcessing: Boolean = false,
    val pendingScan: PendingScan? = null,
    val statusMessage: String? = null,
    val errorKey: String? = null,
)

class ScannerViewModel(
    private val context: Context,
    private val scanRepository: ScanRepository,
) : ViewModel() {

    private val pdfGenerator = PDFGenerator(context)
    private val ocrService = OCRService(context)

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun onScanResult(intent: Intent?) {
        val result = intent?.let { GmsDocumentScanningResult.fromActivityResultIntent(it) }
        if (result == null) {
            _uiState.update { it.copy(errorKey = "scan_error") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorKey = null) }
            try {
                val timestamp = System.currentTimeMillis()
                val saved = pdfGenerator.saveScanFromResult(result, timestamp)
                val ocrResult = ocrService.recognizeScanResult(context, result)
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        pendingScan = PendingScan(
                            file = saved.file,
                            pageCount = saved.pageCount,
                            ocrText = ocrResult.text,
                            timestamp = timestamp,
                            usedRussianEngine = ocrResult.usedRussianEngine,
                        ),
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isProcessing = false, errorKey = "scan_error") }
            }
        }
    }

    fun onScanCancelled() {
        _uiState.update { it.copy(errorKey = "scan_cancelled") }
    }

    fun clearPendingScan() {
        val pending = _uiState.value.pendingScan ?: return
        if (!pending.savedToDb) {
            pending.file.delete()
        }
        _uiState.update { it.copy(pendingScan = null, statusMessage = null, errorKey = null) }
    }

    fun onScanStartFailed() {
        _uiState.update { it.copy(errorKey = "scan_error") }
    }

    fun saveToApp() {
        val pending = _uiState.value.pendingScan ?: return
        if (pending.savedToDb) {
            _uiState.update { it.copy(statusMessage = "scan_success") }
            return
        }
        viewModelScope.launch {
            try {
                scanRepository.saveScan(
                    fileName = pending.file.name,
                    filePath = pending.file.absolutePath,
                    timestamp = pending.timestamp,
                    fileSizeBytes = pending.file.length(),
                    pageCount = pending.pageCount,
                    ocrText = pending.ocrText,
                )
                _uiState.update {
                    it.copy(
                        pendingScan = pending.copy(savedToDb = true),
                        statusMessage = "scan_success",
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(errorKey = "scan_error") }
            }
        }
    }

    fun saveToPhone() {
        val pending = _uiState.value.pendingScan ?: return
        viewModelScope.launch {
            try {
                val storageHelper = StorageHelper(context)
                val result = storageHelper.saveToPublicDownloads(
                    fileName = pending.file.name,
                    mimeType = "application/pdf",
                ) { out ->
                    pending.file.inputStream().use { it.copyTo(out) }
                } ?: throw IllegalStateException("MediaStore save failed")
                saveToApp()
                _uiState.update { it.copy(statusMessage = "scan_saved_phone:${result.displayPath}") }
            } catch (_: Exception) {
                _uiState.update { it.copy(errorKey = "scan_error") }
            }
        }
    }

    override fun onCleared() {
        ocrService.close()
        super.onCleared()
    }

    class Factory(
        private val context: Context,
        private val scanRepository: ScanRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScannerViewModel(context.applicationContext, scanRepository) as T
        }
    }
}
