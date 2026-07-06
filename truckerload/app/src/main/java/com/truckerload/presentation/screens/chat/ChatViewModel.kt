package com.truckerload.presentation.screens.chat

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.truckerload.R
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.AiRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatMessage(val role: String, val text: String)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val streamingText: String = "",
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class ChatViewModel(
    private val aiRepository: AiRepository?,
    private val loadRepository: LoadRepository,
    private val paycheckRepository: PaycheckRepository,
    private val dieselRepository: DieselRepository,
    private val app: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val isAvailable: Boolean get() = aiRepository != null

    fun setInputText(text: String) {
        _uiState.update { it.copy(inputText = text, error = null) }
    }

    private var streamJob: Job? = null

    fun sendMessage(text: String? = null) {
        val msg = (text ?: _uiState.value.inputText).trim()
        if (msg.isBlank() || aiRepository == null || _uiState.value.isLoading) return

        streamJob?.cancel()
        _uiState.update {
            it.copy(
                inputText = "",
                messages = it.messages + ChatMessage("user", msg) + ChatMessage("model", ""),
                streamingText = "",
                isLoading = true,
                error = null
            )
        }

        streamJob = viewModelScope.launch {
            val messagesNow = _uiState.value.messages
            val history = messagesNow.dropLast(2).map { m -> m.role to m.text }
            val appPromptContext = buildAppContext()
            try {
                val buffer = StringBuilder()
                var lastFlush = System.currentTimeMillis()
                aiRepository.chatStream(history, msg, appPromptContext).collect { token ->
                    buffer.append(token)
                    val now = System.currentTimeMillis()
                    if (buffer.length >= 20 || now - lastFlush > 50) {
                        val toAppend = buffer.toString()
                        buffer.clear()
                        lastFlush = now
                        _uiState.update {
                            val last = it.messages.lastOrNull()
                            val updated = if (last != null && last.role == "model") {
                                it.messages.dropLast(1) + ChatMessage("model", last.text + toAppend)
                            } else it.messages + ChatMessage("model", toAppend)
                            it.copy(messages = updated)
                        }
                    }
                }
                val finalText = buffer.toString()
                val streamedText = (_uiState.value.messages.lastOrNull()?.takeIf { it.role == "model" }?.text ?: "") + finalText
                val resolvedText = if (streamedText.isBlank()) {
                    requestReliableChatReply(history, msg, appPromptContext)
                } else streamedText

                if (resolvedText.isBlank()) {
                    throw IllegalStateException(app.getString(R.string.advisor_error_no_response))
                }

                _uiState.update {
                    val last = it.messages.lastOrNull()
                    val updated = if (last != null && last.role == "model") {
                        it.messages.dropLast(1) + ChatMessage("model", resolvedText)
                    } else it.messages + ChatMessage("model", resolvedText)
                    it.copy(messages = updated, streamingText = "", isLoading = false, error = null)
                }
            } catch (e: Exception) {
                val received = _uiState.value.messages.lastOrNull()?.text ?: ""
                val friendly = toUserFriendlyError(e)
                _uiState.update {
                    val errMsg = if (received.isNotEmpty()) "$received\n\n⚠ $friendly" else "⚠ $friendly"
                    val updated = it.messages.dropLast(1) + ChatMessage("model", errMsg)
                    it.copy(messages = updated, streamingText = "", isLoading = false, error = friendly)
                }
            }
        }
    }

    fun runAiHealthCheck() {
        if (aiRepository == null || _uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = aiRepository.healthCheck()
            val stamp = nowTimeStamp()
            val text = result.fold(
                onSuccess = { "✅ [$stamp] AI health: ${it.trim()}" },
                onFailure = { error ->
                    val httpHint = extractHttpCodeHint(error)
                    val friendly = toUserFriendlyError(error)
                    if (httpHint != null) {
                        "❌ [$stamp] AI health: $friendly ($httpHint)"
                    } else {
                        "❌ [$stamp] AI health: $friendly"
                    }
                }
            )
            _uiState.update {
                it.copy(
                    messages = it.messages + ChatMessage("model", text),
                    isLoading = false
                )
            }
        }
    }

    private suspend fun requestReliableChatReply(
        history: List<Pair<String, String>>,
        msg: String,
        appContext: String?
    ): String {
        // 1) Regular chat with app context.
        aiRepository?.chat(history, msg, appContext)?.getOrNull()?.trim()?.takeIf { it.isNotBlank() }?.let {
            return it
        }
        // 2) Fallback without app context (smaller prompt, safer against 400/limit issues).
        aiRepository?.chat(history, msg, null)?.getOrNull()?.trim()?.takeIf { it.isNotBlank() }?.let {
            return it
        }
        return ""
    }

    private suspend fun buildAppContext(): String {
        return try {
            val loads = loadRepository.getAllLoads().first().takeLast(25)
            val paychecks = paycheckRepository.getAllPaychecks().first().takeLast(12)
            val diesels = dieselRepository.getAllDiesel().first().takeLast(15)
            buildString {
                append("LOADS (last ${loads.size}):\n")
                loads.forEach { l ->
                    append("- ${l.tripId} | ${l.date} | ${l.pointA} → ${l.pointB} | $${String.format(Locale.US, "%,.2f", l.totalRate)} | ${String.format(Locale.US, "%,.0f", l.totalMiles)} mi\n")
                }
                append("PAYCHECKS (last ${paychecks.size}):\n")
                paychecks.forEach { p ->
                    append("- Week ${p.weekNumber} ${p.year} | $${String.format(Locale.US, "%,.2f", p.netAmount)}\n")
                }
                append("DIESEL (last ${diesels.size}):\n")
                diesels.forEach { d ->
                    append("- Week ${d.weekNumber} ${d.year} | $${String.format(Locale.US, "%,.2f", d.totalAmount)}\n")
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("ChatViewModel", "buildAppContext failed: ${e.message}", e)
            ""
        }
    }

    private fun toUserFriendlyError(error: Throwable): String {
        val message = (error.message ?: "").lowercase(Locale.ROOT)
        val noResponseKeyword = app.getString(R.string.advisor_error_keyword_no_response)
        return when {
            "401" in message || "403" in message || "api key" in message ->
                app.getString(R.string.advisor_error_api_key)
            "429" in message || "rate" in message || "quota" in message || "limit" in message ->
                app.getString(R.string.advisor_error_rate_limit)
            "500" in message || "502" in message || "503" in message || "504" in message ->
                app.getString(R.string.advisor_error_unavailable)
            "timeout" in message || "timed out" in message ->
                app.getString(R.string.advisor_error_timeout)
            "unable to resolve host" in message || "network" in message || "failed to connect" in message ->
                app.getString(R.string.advisor_error_network)
            "empty" in message || noResponseKeyword in message ->
                app.getString(R.string.advisor_error_empty)
            else ->
                app.getString(R.string.advisor_error_generic)
        }
    }

    private fun extractHttpCodeHint(error: Throwable): String? {
        val raw = error.message.orEmpty()
        val regex = Regex("""\b([45]\d{2})\b""")
        val code = regex.find(raw)?.groupValues?.getOrNull(1) ?: return null
        return "HTTP $code"
    }

    private fun nowTimeStamp(): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    class Factory(
        private val aiRepository: AiRepository?,
        private val loadRepository: LoadRepository,
        private val paycheckRepository: PaycheckRepository,
        private val dieselRepository: DieselRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ChatViewModel(aiRepository, loadRepository, paycheckRepository, dieselRepository, context.applicationContext as Application) as T
    }
}
