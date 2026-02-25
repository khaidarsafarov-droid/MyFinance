package com.truckerload.presentation.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.GeminiRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatMessage(val role: String, val text: String)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class ChatViewModel(
    private val geminiRepository: GeminiRepository?,
    private val loadRepository: LoadRepository,
    private val paycheckRepository: PaycheckRepository,
    private val dieselRepository: DieselRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val isAvailable: Boolean get() = geminiRepository != null

    fun setInputText(text: String) {
        _uiState.update { it.copy(inputText = text, error = null) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank() || geminiRepository == null || _uiState.value.isLoading) return

        _uiState.update {
            it.copy(
                inputText = "",
                messages = it.messages + ChatMessage("user", text),
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            val messagesNow = _uiState.value.messages
            val history = messagesNow.dropLast(1).map { msg -> msg.role to msg.text }
            val appContext = buildAppContext()
            geminiRepository.chat(history, text, appContext)
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(
                            messages = it.messages + ChatMessage("model", response),
                            isLoading = false,
                            error = null
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            messages = it.messages + ChatMessage("model", "Ошибка: ${e.message}"),
                            isLoading = false,
                            error = e.message
                        )
                    }
                }
        }
    }

    private suspend fun buildAppContext(): String {
        return try {
            val loads = loadRepository.getAllLoads().first().takeLast(25)
            val paychecks = paycheckRepository.getAllPaychecks().first().takeLast(12)
            val diesels = dieselRepository.getAllDiesel().first().takeLast(15)
            buildString {
                append("LOADS (last ${loads.size}):\n")
                loads.forEach { l ->
                    append("- ${l.tripId} | ${l.date} | ${l.pointA} → ${l.pointB} | $${String.format("%,.2f", l.totalRate)} | ${String.format("%,.0f", l.totalMiles)} mi\n")
                }
                append("PAYCHECKS (last ${paychecks.size}):\n")
                paychecks.forEach { p ->
                    append("- Week ${p.weekNumber} ${p.year} | $${String.format("%,.2f", p.netAmount)}\n")
                }
                append("DIESEL (last ${diesels.size}):\n")
                diesels.forEach { d ->
                    append("- Week ${d.weekNumber} ${d.year} | $${String.format("%,.2f", d.totalAmount)}\n")
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("ChatViewModel", "buildAppContext failed: ${e.message}", e)
            ""
        }
    }

    class Factory(
        private val geminiRepository: GeminiRepository?,
        private val loadRepository: LoadRepository,
        private val paycheckRepository: PaycheckRepository,
        private val dieselRepository: DieselRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ChatViewModel(geminiRepository, loadRepository, paycheckRepository, dieselRepository) as T
    }
}
