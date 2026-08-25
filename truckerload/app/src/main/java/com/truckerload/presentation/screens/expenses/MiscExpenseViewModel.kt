package com.truckerload.presentation.screens.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.MiscExpenseRepository
import com.truckerload.domain.expense.MiscExpenseFields
import com.truckerload.domain.model.MiscExpense
import com.truckerload.utils.formatIsoDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class MiscExpenseEditorState(
    val id: Int = 0,
    val amountText: String = "",
    val description: String = "",
    val dateIso: String,
    val createdAt: Long = 0L,
    val error: MiscExpenseFields.Error? = null,
)

data class MiscExpenseUiState(
    val entries: List<MiscExpense> = emptyList(),
    val total: Double = 0.0,
    val editor: MiscExpenseEditorState? = null,
    val confirmDeleteId: Int? = null,
    val isSaving: Boolean = false,
)

@HiltViewModel
class MiscExpenseViewModel @Inject constructor(
    private val repository: MiscExpenseRepository,
) : ViewModel() {

    private val editor = MutableStateFlow<MiscExpenseEditorState?>(null)
    private val confirmDeleteId = MutableStateFlow<Int?>(null)
    private val isSaving = MutableStateFlow(false)

    val uiState: StateFlow<MiscExpenseUiState> = combine(
        repository.observeAll(),
        editor,
        confirmDeleteId,
        isSaving,
    ) { entries, openEditor, deleteId, saving ->
        MiscExpenseUiState(
            entries = entries,
            total = entries.sumOf { it.amount },
            editor = openEditor,
            confirmDeleteId = deleteId,
            isSaving = saving,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MiscExpenseUiState(),
    )

    fun openNew() {
        editor.value = MiscExpenseEditorState(dateIso = formatIsoDate(System.currentTimeMillis()))
    }

    fun openExisting(expense: MiscExpense) {
        editor.value = MiscExpenseEditorState(
            id = expense.id,
            amountText = String.format(Locale.US, "%.2f", expense.amount),
            description = expense.description,
            dateIso = expense.date,
            createdAt = expense.createdAt,
        )
    }

    fun dismissEditor() {
        if (isSaving.value) return
        editor.value = null
        confirmDeleteId.value = null
    }

    fun updateEditor(transform: (MiscExpenseEditorState) -> MiscExpenseEditorState) {
        val current = editor.value ?: return
        editor.value = transform(current).copy(error = null)
    }

    fun save() {
        val current = editor.value ?: return
        val error = MiscExpenseFields.validate(
            current.amountText,
            current.description,
            current.dateIso,
        )
        if (error != null) {
            editor.value = current.copy(error = error)
            return
        }
        val amount = MiscExpenseFields.parseAmount(current.amountText) ?: return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            isSaving.value = true
            repository.upsert(
                MiscExpense(
                    id = current.id,
                    amount = amount,
                    description = current.description.trim(),
                    date = current.dateIso.trim(),
                    createdAt = current.createdAt.takeIf { it > 0L } ?: now,
                    updatedAt = now,
                ),
            )
            isSaving.value = false
            editor.value = null
            confirmDeleteId.value = null
        }
    }

    fun requestDelete() {
        val id = editor.value?.id?.takeIf { it > 0 } ?: return
        confirmDeleteId.value = id
    }

    fun dismissDeleteConfirm() {
        confirmDeleteId.value = null
    }

    fun confirmDelete() {
        val id = confirmDeleteId.value ?: return
        viewModelScope.launch {
            isSaving.value = true
            repository.delete(id)
            isSaving.value = false
            confirmDeleteId.value = null
            editor.value = null
        }
    }
}
