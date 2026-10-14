package com.truckerload.presentation.screens.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.MiscExpenseRepository
import com.truckerload.domain.expense.MiscExpenseFields
import com.truckerload.domain.model.MiscExpense
import com.truckerload.presentation.ScreenPhase
import com.truckerload.presentation.reloadList
import com.truckerload.presentation.toPhase
import com.truckerload.presentation.utils.launchExclusive
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
    val receiptPhotoPath: String? = null,
    /** Path already saved in Room; used to avoid deleting it on Cancel. */
    val initialReceiptPhotoPath: String? = null,
    val createdAt: Long = 0L,
    val error: MiscExpenseFields.Error? = null,
)

data class MiscExpenseUiState(
    val entries: List<MiscExpense> = emptyList(),
    val total: Double = 0.0,
    val editor: MiscExpenseEditorState? = null,
    val confirmDeleteId: Int? = null,
    val isSaving: Boolean = false,
    val phase: ScreenPhase = ScreenPhase.Loading,
)

@HiltViewModel
class MiscExpenseViewModel @Inject constructor(
    private val repository: MiscExpenseRepository,
) : ViewModel() {

    private val editor = MutableStateFlow<MiscExpenseEditorState?>(null)
    private val confirmDeleteId = MutableStateFlow<Int?>(null)
    private val isSaving = MutableStateFlow(false)
    private val reload = MutableStateFlow(0)
    private val entriesResult = reload.reloadList { repository.observeAll() }

    val uiState: StateFlow<MiscExpenseUiState> = combine(
        entriesResult,
        editor,
        confirmDeleteId,
        isSaving,
    ) { result, openEditor, deleteId, saving ->
        val entries = result.getOrDefault(emptyList())
        MiscExpenseUiState(
            entries = entries,
            total = if (result.isSuccess) entries.sumOf { it.amount } else 0.0,
            editor = openEditor,
            confirmDeleteId = deleteId,
            isSaving = saving,
            phase = result.toPhase(entries.isEmpty()),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MiscExpenseUiState(),
    )

    fun retry() {
        reload.value += 1
    }

    fun openNew() {
        editor.value = MiscExpenseEditorState(dateIso = formatIsoDate(System.currentTimeMillis()))
    }

    fun openExisting(expense: MiscExpense) {
        editor.value = MiscExpenseEditorState(
            id = expense.id,
            amountText = String.format(Locale.US, "%.2f", expense.amount),
            description = expense.description,
            dateIso = expense.date,
            receiptPhotoPath = expense.receiptPhotoPath,
            initialReceiptPhotoPath = expense.receiptPhotoPath,
            createdAt = expense.createdAt,
        )
    }

    fun dismissEditor() {
        if (isSaving.value) return
        val current = editor.value
        editor.value = null
        confirmDeleteId.value = null
        if (current != null) {
            viewModelScope.launch {
                repository.discardUnsavedReceipt(
                    currentPath = current.receiptPhotoPath,
                    initialPath = current.initialReceiptPhotoPath,
                )
            }
        }
    }

    fun updateEditor(transform: (MiscExpenseEditorState) -> MiscExpenseEditorState) {
        val current = editor.value ?: return
        val next = transform(current)
        val keptAmountError = current.error == MiscExpenseFields.Error.AMOUNT &&
            MiscExpenseFields.parseAmount(next.amountText) == null
        editor.value = next.copy(
            error = if (keptAmountError) MiscExpenseFields.Error.AMOUNT else null,
        )
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
        viewModelScope.launchExclusive(isSaving, TAG) {
            repository.upsert(
                MiscExpense(
                    id = current.id,
                    amount = amount,
                    description = current.description.trim(),
                    date = current.dateIso.trim(),
                    receiptPhotoPath = current.receiptPhotoPath,
                    createdAt = current.createdAt.takeIf { it > 0L } ?: now,
                    updatedAt = now,
                ),
            )
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
        viewModelScope.launchExclusive(isSaving, TAG) {
            repository.delete(id)
            confirmDeleteId.value = null
            editor.value = null
        }
    }

    private companion object {
        const val TAG = "MiscExpenseVM"
    }
}
