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
import com.truckerload.domain.parser.ServiceReceiptTextParser
import com.truckerload.utils.OCRService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    val serviceDate: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val description: String = "",
    val amount: String = "",
    val photoPath: String? = null,
    val ocrText: String? = null,
)

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
    }.stateIn(
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
        viewModelScope.launch {
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
        viewModelScope.launch { repository.markCompleted(id) }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch { repository.deleteTask(id) }
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

    fun processReceiptPhoto(uri: Uri) {
        viewModelScope.launch {
            _formState.update { it.copy(isProcessingPhoto = true, errorMessage = null) }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val saved = copyPhotoToMaintenanceDir(uri)
                    val ocr = ocrService.recognizeFromUri(getApplication(), uri)
                    val parsed = ServiceReceiptTextParser.parse(ocr.text)
                    Triple(saved.absolutePath, ocr.text, parsed)
                }
            }
            result
                .onSuccess { (path, ocrText, parsed) ->
                    _formState.update { state ->
                        state.copy(
                            isProcessingPhoto = false,
                            showAddArchive = true,
                            archiveDraft = state.archiveDraft.copy(
                                photoPath = path,
                                ocrText = ocrText,
                                amount = parsed.amount?.let { "%.2f".format(it) }
                                    ?: state.archiveDraft.amount,
                                serviceDate = parsed.date?.takeIf { it.isNotBlank() }
                                    ?: state.archiveDraft.serviceDate,
                                description = parsed.descriptionHint
                                    ?: state.archiveDraft.description,
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
        val description = draft.description.trim()
        val amount = draft.amount.replace(',', '.').toDoubleOrNull()
        if (description.isBlank()) {
            _formState.update { it.copy(errorMessage = "empty_description") }
            return
        }
        if (amount == null || amount < 0) {
            _formState.update { it.copy(errorMessage = "invalid_amount") }
            return
        }
        viewModelScope.launch {
            _formState.update { it.copy(isSaving = true) }
            runCatching {
                repository.insertArchive(
                    MaintenanceArchiveEntry(
                        serviceDate = draft.serviceDate,
                        description = description,
                        amount = amount,
                        photoPath = draft.photoPath,
                        ocrText = draft.ocrText,
                    ),
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
        viewModelScope.launch { repository.deleteArchive(id) }
    }

    private fun copyPhotoToMaintenanceDir(uri: Uri): File {
        val context = getApplication<Application>()
        val dir = File(context.getExternalFilesDir(null), "maintenance").apply { mkdirs() }
        val dest = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(dest).use { output -> input.copyTo(output) }
        } ?: error("Cannot open photo")
        // Validate image
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
