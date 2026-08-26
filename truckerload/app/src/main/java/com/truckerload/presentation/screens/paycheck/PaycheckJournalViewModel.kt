package com.truckerload.presentation.screens.paycheck

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.domain.model.Paycheck
import com.truckerload.domain.model.PaycheckJournalFilter
import com.truckerload.domain.paycheck.PaycheckSalaryFields
import com.truckerload.utils.PaycheckSourceFiles
import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.utils.getWeekRange
import com.truckerload.utils.shiftWeekNumberAndYear
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PaycheckEditorState(
    val paycheck: Paycheck,
    val netText: String,
    val grossText: String,
    val sourceFileName: String?,
    val sourceFilePath: String?,
    val originalSourceFilePath: String?,
    val error: PaycheckSalaryFields.Error? = null,
    val attachFailed: Boolean = false,
)

data class PaycheckJournalUiState(
    val weekNumber: Int,
    val year: Int,
    val weekLabel: String,
    val showAllWeeks: Boolean,
    val searchQuery: String,
    val entries: List<Paycheck>,
    val total: Double,
    val editor: PaycheckEditorState? = null,
    val isSaving: Boolean = false,
)

@HiltViewModel
class PaycheckJournalViewModel @Inject constructor(
    private val paycheckRepository: PaycheckRepository,
    @param:ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val initialWeek = getCurrentWeekNumberAndYear()
    private val weekNumber = MutableStateFlow(initialWeek.first)
    private val year = MutableStateFlow(initialWeek.second)
    private val allWeeks = MutableStateFlow(true)
    private val searchQuery = MutableStateFlow("")
    private val editor = MutableStateFlow<PaycheckEditorState?>(null)
    private val isSaving = MutableStateFlow(false)

    private val journal = combine(
        paycheckRepository.getAllPaychecks(),
        weekNumber,
        year,
        allWeeks,
        searchQuery,
    ) { all, week, yr, showAll, query ->
        val scoped = if (showAll) {
            PaycheckJournalFilter.all(all)
        } else {
            PaycheckJournalFilter.forWeek(all, week, yr)
        }
        val visible = PaycheckJournalFilter.matching(scoped, query)
        val (_, _, weekLabel) = getWeekRange(week, yr)
        JournalSlice(
            weekNumber = week,
            year = yr,
            weekLabel = weekLabel,
            showAllWeeks = showAll,
            searchQuery = query,
            entries = visible,
            total = scoped.sumOf { it.netAmount },
        )
    }

    val uiState: StateFlow<PaycheckJournalUiState> = combine(
        journal,
        editor,
        isSaving,
    ) { slice, openEditor, saving ->
        PaycheckJournalUiState(
            weekNumber = slice.weekNumber,
            year = slice.year,
            weekLabel = slice.weekLabel,
            showAllWeeks = slice.showAllWeeks,
            searchQuery = slice.searchQuery,
            entries = slice.entries,
            total = slice.total,
            editor = openEditor,
            isSaving = saving,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PaycheckJournalUiState(
            weekNumber = initialWeek.first,
            year = initialWeek.second,
            weekLabel = getWeekRange(initialWeek.first, initialWeek.second).third,
            showAllWeeks = true,
            searchQuery = "",
            entries = emptyList(),
            total = 0.0,
        ),
    )

    fun selectPreviousWeek() = shiftWeek(-1)

    fun selectNextWeek() = shiftWeek(1)

    fun showAllWeeks() {
        allWeeks.value = true
    }

    fun setSearchQuery(value: String) {
        searchQuery.value = value
    }

    fun openEditor(paycheck: Paycheck) {
        editor.value = PaycheckEditorState(
            paycheck = paycheck,
            netText = PaycheckSalaryFields.formatAmount(paycheck.netAmount),
            grossText = paycheck.grossAmount
                ?.takeIf { it > 0.0 }
                ?.let { PaycheckSalaryFields.formatAmount(it) }
                .orEmpty(),
            sourceFileName = paycheck.sourceFileName,
            sourceFilePath = paycheck.sourceFilePath,
            originalSourceFilePath = paycheck.sourceFilePath,
        )
    }

    fun dismissEditor() {
        if (isSaving.value) return
        val current = editor.value
        val extraPath = current?.sourceFilePath
            ?.takeIf { it != current.originalSourceFilePath }
        if (extraPath != null) {
            PaycheckSourceFiles.delete(appContext, extraPath)
        }
        editor.value = null
    }

    fun updateEditor(transform: (PaycheckEditorState) -> PaycheckEditorState) {
        val current = editor.value ?: return
        editor.value = transform(current).copy(error = null, attachFailed = false)
    }

    fun attachOriginal(uri: Uri) {
        val current = editor.value ?: return
        if (isSaving.value) return
        PaycheckSourceFiles.takePersistableRead(appContext, uri)
        viewModelScope.launch {
            val displayName = withContext(Dispatchers.IO) {
                PaycheckSourceFiles.displayName(appContext, uri)
            }
            val copied = withContext(Dispatchers.IO) {
                PaycheckSourceFiles.copyFromUri(appContext, uri, displayName)
            }
            val latest = editor.value ?: return@launch
            if (copied == null) {
                editor.value = latest.copy(attachFailed = true)
                return@launch
            }
            latest.sourceFilePath
                ?.takeIf { it != latest.originalSourceFilePath && it != copied }
                ?.let { PaycheckSourceFiles.delete(appContext, it) }
            editor.value = latest.copy(
                sourceFilePath = copied,
                sourceFileName = displayName.ifBlank { latest.sourceFileName },
                attachFailed = false,
            )
        }
    }

    fun saveEditor() {
        val current = editor.value ?: return
        val error = PaycheckSalaryFields.validate(current.netText, current.grossText)
        if (error != null) {
            editor.value = current.copy(error = error)
            return
        }
        val net = PaycheckSalaryFields.parseAmount(current.netText) ?: return
        val gross = PaycheckSalaryFields.parseOptionalAmount(current.grossText)
        viewModelScope.launch {
            isSaving.value = true
            paycheckRepository.updatePaycheck(
                current.paycheck.copy(
                    netAmount = net,
                    grossAmount = gross,
                    sourceFileName = current.sourceFileName,
                    sourceFilePath = current.sourceFilePath,
                ),
            )
            val previousPath = current.originalSourceFilePath
            val newPath = current.sourceFilePath
            if (previousPath != null && previousPath != newPath) {
                PaycheckSourceFiles.delete(appContext, previousPath)
            }
            isSaving.value = false
            editor.value = null
        }
    }

    private fun shiftWeek(delta: Int) {
        val (week, yr) = shiftWeekNumberAndYear(weekNumber.value, year.value, delta)
        weekNumber.value = week
        year.value = yr
        allWeeks.value = false
    }

    private data class JournalSlice(
        val weekNumber: Int,
        val year: Int,
        val weekLabel: String,
        val showAllWeeks: Boolean,
        val searchQuery: String,
        val entries: List<Paycheck>,
        val total: Double,
    )
}
