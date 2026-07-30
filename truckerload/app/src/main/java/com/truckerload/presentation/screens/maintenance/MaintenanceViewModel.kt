package com.truckerload.presentation.screens.maintenance

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.MaintenanceRepository
import com.truckerload.domain.model.MaintenanceArchiveEntry
import com.truckerload.domain.model.MaintenanceProgress
import com.truckerload.domain.model.MaintenanceReminderType
import com.truckerload.domain.model.MaintenanceTask
import com.truckerload.domain.model.ReceiptData
import com.truckerload.domain.parser.ServiceReceiptTextParser
import com.truckerload.utils.OCRService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class MaintenanceUiState(
    val activeProgress: List<MaintenanceProgress> = emptyList(),
    val completedTasks: List<MaintenanceTask> = emptyList(),
    val archive: List<MaintenanceArchiveEntry> = emptyList(),
    val showAddTask: Boolean = false,
    val showAddArchive: Boolean = false,
    val showReceiptSourcePicker: Boolean = false,
    val viewingReceiptPath: String? = null,
    val taskDraft: TaskDraft = TaskDraft(),
    val archiveDraft: ArchiveDraft = ArchiveDraft(),
    val isSaving: Boolean = false,
    val isProcessingPhoto: Boolean = false,
    val errorMessage: String? = null,
)

data class TaskDraft(
    val title: String = "",
    val startDate: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val reminderType: MaintenanceReminderType = MaintenanceReminderType.MILES,
    val intervalMiles: String = "",
    val odometerAtStart: String = "",
    val dueDate: String = LocalDate.now().plusMonths(1).format(DateTimeFormatter.ISO_LOCAL_DATE),
)

data class ArchiveDraft(
    val serviceName: String = "",
    val serviceDate: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val description: String = "",
    val amount: String = "",
    val photoPath: String? = null,
    val ocrText: String? = null,
) {
    fun toReceiptData(): ReceiptData =
        ReceiptData(
            imageUri = photoPath.orEmpty(),
            serviceName = serviceName,
            date = ReceiptData.isoDateToEpochMillis(serviceDate),
            totalAmount = amount.replace(',', '.').toDoubleOrNull() ?: 0.0,
            description = description,
            rawText = ocrText,
        )
}

class MaintenanceViewModel(
    app: Application,
    private val repository: MaintenanceRepository,
) : AndroidViewModel(app) {

    private val ocrService = OCRService(app)

    private val _formState = MutableStateFlow(MaintenanceUiState())
    val formState: StateFlow<MaintenanceUiState> = _formState.asStateFlow()

    val uiState: StateFlow<MaintenanceUiState> = combine(
        repository.watchActiveProgress(),
        repository.watchTasks(),
        repository.watchArchive(),
        _formState,
    ) { progress, tasks, archive, form ->
        form.copy(
            activeProgress = progress,
            completedTasks = tasks.filter { it.isCompleted },
            archive = archive,
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MaintenanceUiState(),
        )

    fun openAddTask() {
        _formState.update {
            it.copy(
                showAddTask = true,
                taskDraft = TaskDraft(),
                errorMessage = null,
            )
        }
    }

    fun dismissAddTask() {
        _formState.update { it.copy(showAddTask = false, errorMessage = null) }
    }

    fun updateTaskDraft(transform: (TaskDraft) -> TaskDraft) {
        _formState.update { it.copy(taskDraft = transform(it.taskDraft), errorMessage = null) }
    }

    fun saveTask() {
        val draft = _formState.value.taskDraft
        val title = draft.title.trim()
        if (title.isBlank()) {
            _formState.update { it.copy(errorMessage = "empty_title") }
            return
        }
        when (draft.reminderType) {
            MaintenanceReminderType.MILES -> {
                val interval = draft.intervalMiles.replace(',', '.').toDoubleOrNull()
                val odo = draft.odometerAtStart.replace(',', '.').toDoubleOrNull()
                if (interval == null || interval <= 0) {
                    _formState.update { it.copy(errorMessage = "invalid_interval") }
                    return
                }
                if (odo == null || odo < 0) {
                    _formState.update { it.copy(errorMessage = "invalid_odometer") }
                    return
                }
                persistTask(
                    MaintenanceTask(
                        title = title,
                        startDate = draft.startDate,
                        reminderType = MaintenanceReminderType.MILES,
                        intervalMiles = interval,
                        odometerAtStart = odo,
                    ),
                )
            }
            MaintenanceReminderType.DATE -> {
                if (draft.dueDate.isBlank()) {
                    _formState.update { it.copy(errorMessage = "invalid_due_date") }
                    return
                }
                val odo = draft.odometerAtStart.replace(',', '.').toDoubleOrNull()
                persistTask(
                    MaintenanceTask(
                        title = title,
                        startDate = draft.startDate,
                        reminderType = MaintenanceReminderType.DATE,
                        dueDate = draft.dueDate,
                        odometerAtStart = odo,
                    ),
                )
            }
        }
    }

    private fun persistTask(task: MaintenanceTask) {
        viewModelScope.launch(Dispatchers.IO) {
            _formState.update { it.copy(isSaving = true) }
            runCatching { repository.insertTask(task) }
                .onSuccess {
                    _formState.update {
                        it.copy(isSaving = false, showAddTask = false, taskDraft = TaskDraft())
                    }
                }
                .onFailure { e ->
                    _formState.update {
                        it.copy(isSaving = false, errorMessage = e.message ?: "save_failed")
                    }
                }
        }
    }

    fun completeTask(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { repository.markCompleted(id) }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteTask(id) }
    }

    fun openReceiptSourcePicker() {
        _formState.update {
            it.copy(showReceiptSourcePicker = true, errorMessage = null)
        }
    }

    fun dismissReceiptSourcePicker() {
        _formState.update { it.copy(showReceiptSourcePicker = false) }
    }

    fun openAddArchive() {
        _formState.update {
            it.copy(
                showAddArchive = true,
                archiveDraft = ArchiveDraft(),
                errorMessage = null,
            )
        }
    }

    fun dismissAddArchive() {
        _formState.update { it.copy(showAddArchive = false, errorMessage = null) }
    }

    fun updateArchiveDraft(transform: (ArchiveDraft) -> ArchiveDraft) {
        _formState.update { it.copy(archiveDraft = transform(it.archiveDraft), errorMessage = null) }
    }

    fun openReceiptViewer(path: String) {
        _formState.update { it.copy(viewingReceiptPath = path) }
    }

    fun dismissReceiptViewer() {
        _formState.update { it.copy(viewingReceiptPath = null) }
    }

    /**
     * Runs ML Kit (via [OCRService]) on the receipt image, parses key fields, and opens the
     * editable preview dialog. Photo is copied into protected app storage (`filesDir/receipts`).
     *
     * @param deleteAfterCopy optional camera cache file to remove after a successful copy
     */
    fun processReceiptPhoto(uri: Uri, deleteAfterCopy: File? = null) {
        viewModelScope.launch {
            _formState.update {
                it.copy(
                    isProcessingPhoto = true,
                    showReceiptSourcePicker = false,
                    errorMessage = null,
                )
            }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val saved = copyPhotoToProtectedStorage(uri)
                    deleteAfterCopy?.delete()
                    // OCR the protected copy (source may be a transient cache file).
                    val ocr = ocrService.recognizeFromUri(
                        getApplication(),
                        android.net.Uri.fromFile(saved),
                    )
                    val rawText = ocr.text
                    val parsed = ServiceReceiptTextParser.parse(rawText)
                    val receipt = ReceiptData(
                        imageUri = saved.absolutePath,
                        serviceName = parsed.serviceName.orEmpty(),
                        date = ReceiptData.isoDateToEpochMillis(
                            parsed.date ?: java.time.LocalDate.now()
                                .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE),
                        ),
                        totalAmount = parsed.amount ?: 0.0,
                        description = parsed.descriptionHint.orEmpty(),
                        rawText = rawText,
                    )
                    receipt
                }.also { outcome ->
                    if (outcome.isFailure) {
                        deleteAfterCopy?.delete()
                    }
                }
            }
            result
                .onSuccess { receipt ->
                    _formState.update { state ->
                        state.copy(
                            isProcessingPhoto = false,
                            showAddArchive = true,
                            archiveDraft = ArchiveDraft(
                                serviceName = receipt.serviceName,
                                serviceDate = ReceiptData.epochMillisToIsoDate(receipt.date),
                                description = receipt.description,
                                amount = if (receipt.totalAmount > 0) {
                                    "%.2f".format(receipt.totalAmount)
                                } else {
                                    ""
                                },
                                photoPath = receipt.imageUri,
                                ocrText = receipt.rawText,
                            ),
                        )
                    }
                }
                .onFailure { e ->
                    _formState.update {
                        it.copy(
                            isProcessingPhoto = false,
                            errorMessage = e.message ?: "photo_failed",
                        )
                    }
                }
        }
    }

    fun saveArchive() {
        val draft = _formState.value.archiveDraft
        val receipt = draft.toReceiptData()
        val description = receipt.description.trim().ifBlank { receipt.serviceName.trim() }
        if (description.isBlank()) {
            _formState.update { it.copy(errorMessage = "empty_description") }
            return
        }
        if (receipt.totalAmount < 0 || draft.amount.isBlank()) {
            _formState.update { it.copy(errorMessage = "invalid_amount") }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _formState.update { it.copy(isSaving = true) }
            runCatching {
                repository.insertArchive(
                    receipt.copy(description = description).toArchiveEntry(),
                )
            }
                .onSuccess {
                    _formState.update {
                        it.copy(isSaving = false, showAddArchive = false, archiveDraft = ArchiveDraft())
                    }
                }
                .onFailure { e ->
                    _formState.update {
                        it.copy(isSaving = false, errorMessage = e.message ?: "save_failed")
                    }
                }
        }
    }

    fun deleteArchive(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteArchive(id) }
    }

    /** Saves receipt photo under [Application.getFilesDir]/receipts — app-private storage. */
    private fun copyPhotoToProtectedStorage(uri: Uri): File {
        val context = getApplication<Application>()
        val dir = File(context.filesDir, "receipts").apply { mkdirs() }
        val dest = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(dest).use { output -> input.copyTo(output) }
        } ?: error("Cannot open photo")
        BitmapFactory.decodeFile(dest.absolutePath)
            ?: error("Invalid image")
        return dest
    }

    override fun onCleared() {
        ocrService.close()
        super.onCleared()
    }

    class Factory(
        private val app: Application,
        private val repository: MaintenanceRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MaintenanceViewModel::class.java)) {
                return MaintenanceViewModel(app, repository) as T
            }
            error("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
