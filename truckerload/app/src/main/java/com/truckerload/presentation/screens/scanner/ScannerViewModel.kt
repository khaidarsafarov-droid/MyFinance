package com.truckerload.presentation.screens.scanner

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.truckerload.data.repository.ScanRepository
import com.truckerload.domain.model.ScanDocumentCategory
import com.truckerload.domain.model.ScanDocumentFinder
import com.truckerload.utils.OCRService
import com.truckerload.utils.PDFGenerator
import com.truckerload.utils.StorageHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PendingScan(
    val file: File,
    val pageCount: Int,
    val ocrText: String,
    val timestamp: Long,
    val usedRussianEngine: Boolean = false,
    val savedToDb: Boolean = false,
    val isMerged: Boolean = false,
    val category: ScanDocumentCategory = ScanDocumentCategory.OTHER,
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

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val app: Application,
    private val scanRepository: ScanRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val attachLoadId = savedStateHandle.get<String>("loadId")
        ?.let { Uri.decode(it) }
        ?.takeIf { it.isNotBlank() && it != "_" }
    private val attachTripId = savedStateHandle.get<String>("tripId")
        ?.let { Uri.decode(it) }
        ?.takeIf { it.isNotBlank() && it != "_" }
    private val attachLoadDate = savedStateHandle.get<String>("loadDate")
        ?.let { Uri.decode(it) }
        ?.takeIf { it.isNotBlank() }

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
                    category = ScanDocumentFinder.infer(attachLoadId, saved.file.name, ocrResult.text),
                )
                val session = _uiState.value.sessionScans + newScan
                val oldMerged = _uiState.value.pendingScan?.takeIf { it.isMerged && !it.savedToDb }
                val display = buildMergedPending(session, timestamp)
                oldMerged?.file?.delete()
                // Attached-to-load: show result first (share, then save). Do not auto-persist.
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        sessionScans = session,
                        pendingScan = display,
                    )
                }
            } catch (_: Exception) {
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

    fun setScanCategory(category: ScanDocumentCategory) {
        _uiState.update { state ->
            val pending = state.pendingScan ?: return@update state
            state.copy(pendingScan = pending.copy(category = category))
        }
    }

    fun saveToApp() {
        viewModelScope.launch {
            persistPendingToApp(
                showSuccess = true,
                finishAfter = isAttachedToLoad,
            )
        }
    }

    /** After the user returns from the share sheet, persist and leave the scanner. */
    fun saveAfterShare() {
        viewModelScope.launch {
            persistPendingToApp(showSuccess = true, finishAfter = isAttachedToLoad)
        }
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
            } catch (_: Exception) {
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
            // Always emit scan_success when finishing so the UI can exit (never leave
            // autoAttachedAndDone=true without a status the LaunchedEffect handles).
            if (showSuccess || finishAfter) {
                _uiState.update {
                    it.copy(
                        statusMessage = "scan_success",
                        autoAttachedAndDone = finishAfter,
                    )
                }
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
                category = pending.category.name,
            )
            val markedSession = if (pending.isMerged) {
                state.sessionScans.map { it.copy(savedToDb = true) }
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
                    statusMessage = if (showSuccess || finishAfter) "scan_success" else it.statusMessage,
                    autoAttachedAndDone = finishAfter,
                )
            }
        } catch (_: Exception) {
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
        val previousCategory = _uiState.value.pendingScan?.category
        val inferred = ScanDocumentFinder.infer(
            attachLoadId,
            mergedFile.name,
            session.joinToString("\n") { it.ocrText },
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
            category = previousCategory ?: inferred,
        )
    }

    override fun onCleared() {
        ocrService.close()
        super.onCleared()
    }
}
