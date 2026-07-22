package com.truckerload.presentation.screens.scanner

import android.app.Application
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
    val isMerged: Boolean = false,
)

data class ScannerUiState(
    val isProcessing: Boolean = false,
    val pendingScan: PendingScan? = null,
    val sessionScans: List<PendingScan> = emptyList(),
    val scanLaunchKey: Int = 0,
    val statusMessage: String? = null,
    val errorKey: String? = null,
    val autoAttachedAndDone: Boolean = false,
)

class ScannerViewModel(
    private val app: Application,
    private val scanRepository: ScanRepository,
    private val attachLoadId: String? = null,
    private val attachTripId: String? = null,
    private val attachLoadDate: String? = null,
) : ViewModel() {

    private val pdfGenerator = PDFGenerator(app)
    private val ocrService = OCRService(app)

    val isAttachedToLoad: Boolean = !attachLoadId.isNullOrBlank()

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun onScanResult(intent: Intent?) {
        val result = intent?.let { GmsDocumentScanningResult.fromActivityResultIntent(it) }
        if (result == null) {
            if (_uiState.value.sessionScans.isEmpty()) {
                _uiState.update { it.copy(errorKey = "scan_error") }
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorKey = null) }
            try {
                val timestamp = System.currentTimeMillis()
                val saved = pdfGenerator.saveScanFromResult(
                    result = result,
                    timestamp = timestamp,
                    tripId = attachTripId,
                    loadDate = attachLoadDate,
                )
                val ocrResult = ocrService.recognizeScanResult(app, result)
                val newScan = PendingScan(
                    file = saved.file,
                    pageCount = saved.pageCount,
                    ocrText = ocrResult.text,
                    timestamp = timestamp,
                    usedRussianEngine = ocrResult.usedRussianEngine,
                )
                val session = _uiState.value.sessionScans + newScan
                val oldMerged = _uiState.value.pendingScan?.takeIf { it.isMerged && !it.savedToDb }
                val display = buildMergedPending(session, timestamp)
                oldMerged?.file?.delete()
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        sessionScans = session,
                        pendingScan = display,
                    )
                }
                if (isAttachedToLoad) {
                    persistPendingToApp(showSuccess = true, finishAfter = true)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorKey = if (it.sessionScans.isEmpty()) "scan_error" else "scan_error_keep",
                    )
                }
            }
        }
    }

    fun requestAnotherScan() {
        _uiState.update { it.copy(scanLaunchKey = it.scanLaunchKey + 1, statusMessage = null, errorKey = null) }
    }

    fun onScanCancelled() {
        if (_uiState.value.sessionScans.isEmpty()) {
            _uiState.update { it.copy(errorKey = "scan_cancelled") }
        }
    }

    fun clearPendingScan() {
        val state = _uiState.value
        state.sessionScans.forEach { scan ->
            if (!scan.savedToDb) scan.file.delete()
        }
        state.pendingScan?.takeIf { it.isMerged && !it.savedToDb }?.file?.delete()
        _uiState.update {
            it.copy(
                pendingScan = null,
                sessionScans = emptyList(),
                statusMessage = null,
                errorKey = null,
                autoAttachedAndDone = false,
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorKey = null) }
    }

    fun clearStatus() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun onScanStartFailed() {
        if (_uiState.value.sessionScans.isEmpty()) {
            _uiState.update { it.copy(errorKey = "scan_error") }
        }
    }

    fun mergedShareFile(): File? = _uiState.value.pendingScan?.file?.takeIf { it.exists() }

    fun onShareUnavailable() {
        _uiState.update { it.copy(errorKey = "share_failed") }
    }

    fun saveToApp() {
        viewModelScope.launch { persistPendingToApp(showSuccess = true) }
    }

    /** Persists the current scan (if needed), then invokes [onReady] on the main thread. */
    fun saveThenOpenGallery(onReady: () -> Unit) {
        viewModelScope.launch {
            persistPendingToApp(showSuccess = false)
            if (_uiState.value.pendingScan?.savedToDb == true) {
                onReady()
            }
        }
    }

    fun saveToPhone() {
        val pending = _uiState.value.pendingScan ?: return
        viewModelScope.launch {
            try {
                val storageHelper = StorageHelper(app)
                val result = storageHelper.saveToPublicDownloads(
                    fileName = pending.file.name,
                    mimeType = "application/pdf",
                ) { out ->
                    pending.file.inputStream().use { it.copyTo(out) }
                } ?: throw IllegalStateException("MediaStore save failed")
                persistPendingToApp(showSuccess = false)
                _uiState.update { it.copy(statusMessage = "scan_saved_phone:${result.displayPath}") }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorKey = if (it.sessionScans.isEmpty() && it.pendingScan == null) {
                            "scan_error"
                        } else {
                            "scan_error_keep"
                        },
                    )
                }
            }
        }
    }

    private suspend fun persistPendingToApp(showSuccess: Boolean, finishAfter: Boolean = false) {
        val state = _uiState.value
        val pending = state.pendingScan ?: return
        if (pending.savedToDb) {
            if (showSuccess) {
                _uiState.update {
                    it.copy(
                        statusMessage = "scan_success",
                        autoAttachedAndDone = finishAfter,
                    )
                }
            } else if (finishAfter) {
                _uiState.update { it.copy(autoAttachedAndDone = true) }
            }
            return
        }
        try {
            scanRepository.saveScan(
                fileName = pending.file.name,
                filePath = pending.file.absolutePath,
                timestamp = pending.timestamp,
                fileSizeBytes = pending.file.length(),
                pageCount = pending.pageCount,
                ocrText = pending.ocrText,
                loadId = attachLoadId,
            )
            val markedSession = if (pending.isMerged) {
                state.sessionScans
            } else {
                state.sessionScans.map { scan ->
                    if (scan.file.absolutePath == pending.file.absolutePath) {
                        scan.copy(savedToDb = true)
                    } else {
                        scan
                    }
                }
            }
            _uiState.update {
                it.copy(
                    pendingScan = pending.copy(savedToDb = true),
                    sessionScans = markedSession,
                    statusMessage = if (showSuccess) "scan_success" else it.statusMessage,
                    autoAttachedAndDone = finishAfter,
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(errorKey = "scan_error_keep") }
        }
    }

    private fun buildMergedPending(session: List<PendingScan>, timestamp: Long): PendingScan {
        if (session.size == 1) return session.first()
        val existing = session.map { it.file }.filter { it.exists() }
        require(existing.isNotEmpty()) { "No PDF files to merge" }
        val mergedFile = pdfGenerator.mergePdfFiles(
            sources = existing,
            fileName = pdfGenerator.buildScanFileName(
                timestamp = timestamp,
                tripId = attachTripId,
                loadDate = attachLoadDate,
            ),
        )
        return PendingScan(
            file = mergedFile,
            pageCount = session.sumOf { it.pageCount },
            ocrText = session.mapIndexed { index, scan ->
                if (index == 0) scan.ocrText else "---\n\n${scan.ocrText}"
            }.joinToString("\n\n"),
            timestamp = timestamp,
            usedRussianEngine = session.any { it.usedRussianEngine },
            isMerged = true,
        )
    }

    override fun onCleared() {
        ocrService.close()
        super.onCleared()
    }

    class Factory(
        private val context: Context,
        private val scanRepository: ScanRepository,
        private val attachLoadId: String? = null,
        private val attachTripId: String? = null,
        private val attachLoadDate: String? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScannerViewModel(
                context.applicationContext as Application,
                scanRepository,
                attachLoadId = attachLoadId,
                attachTripId = attachTripId,
                attachLoadDate = attachLoadDate,
            ) as T
        }
    }
}
