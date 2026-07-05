package com.example.myfinance.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfinance.gemini.GeminiApi
import com.example.myfinance.gemini.ParsedTrip
import com.example.myfinance.gemini.ParsedWeeklyTotal
import com.example.myfinance.gemini.parseWeeklyTotalSimple
import com.example.myfinance.telegram.TelegramApi
import com.example.myfinance.telegram.TelegramRepository
import com.example.myfinance.telegram.TelegramUpdate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TelegramUiState(
    val chatId: String? = null,
    val linkCode: String? = null,
    val isPolling: Boolean = false,
    val message: String? = null,
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false
)

class TelegramViewModel(application: Application) : AndroidViewModel(application) {

    private val api = TelegramApi()
    private val repo = TelegramRepository(application)
    private val gemini = GeminiApi()

    val isConfigured: Boolean get() = api.isConfigured()

    val chatId: StateFlow<String?> = repo.chatId.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _uiState = MutableStateFlow(TelegramUiState())
    val uiState: StateFlow<TelegramUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var realtimeSyncJob: Job? = null

    /** Throttle auto-sync: don't run more than once per 2 minutes. */
    private var lastAutoSyncTimeMs: Long = 0
    private val autoSyncThrottleMs = 2 * 60 * 1000L

    init {
        viewModelScope.launch {
            chatId.collect { id ->
                _uiState.update { it.copy(chatId = id, linkCode = null, isPolling = false) }
            }
        }
    }

    /**
     * Start real-time sync: long-poll getUpdates so when user sends a message to the bot,
     * we get it within seconds and reply immediately. Call when chat is linked (e.g. from MainNav).
     */
    fun startRealtimeSync(
        getCurrentCompanyId: () -> String?,
        onAddWeeklyTotal: (ParsedWeeklyTotal) -> Boolean,
        onAddTrip: (ParsedTrip) -> Boolean
    ) {
        realtimeSyncJob?.cancel()
        realtimeSyncJob = viewModelScope.launch {
            val cid = repo.getChatId() ?: return@launch
            var lastId = repo.getLastProcessedUpdateId()
            while (isActive) {
                val r = api.getUpdates(offset = if (lastId > 0) lastId + 1 else null, timeoutSeconds = 25)
                if (!r.isSuccess) {
                    delay(2000)
                    continue
                }
                val updates = r.getOrNull()?.filter { it.chatId == cid } ?: emptyList()
                if (updates.isNotEmpty()) {
                    processUpdates(updates, cid, getCurrentCompanyId, onAddWeeklyTotal, onAddTrip)
                    lastId = updates.maxOf { it.updateId }
                    repo.setLastProcessedUpdateId(lastId)
                }
            }
        }
    }

    fun stopRealtimeSync() {
        realtimeSyncJob?.cancel()
        realtimeSyncJob = null
    }

    fun startLinking() {
        if (!api.isConfigured()) {
            _uiState.update { it.copy(message = "Bot token not set in local.properties") }
            return
        }
        val code = (100000..999999).random().toString()
        _uiState.update {
            it.copy(linkCode = code, isPolling = true, message = null)
        }
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            var offset: Long? = null
            while (viewModelScope.coroutineContext.isActive) {
                val r = api.getUpdates(offset)
                if (r.isSuccess) {
                    val updates = r.getOrNull() ?: emptyList()
                        for (u in updates) {
                            offset = u.updateId + 1
                            if (u.text.trim() == code) {
                                repo.setChatId(u.chatId)
                                _uiState.update {
                                    it.copy(linkCode = null, isPolling = false, message = "Linked successfully!")
                                }
                                pollingJob?.cancel()
                                return@launch
                            }
                        }
                }
                delay(2000)
            }
        }
    }

    fun stopLinking() {
        pollingJob?.cancel()
        pollingJob = null
        _uiState.update { it.copy(linkCode = null, isPolling = false) }
    }

    fun sendTestMessage() {
        viewModelScope.launch {
            val cid = repo.getChatId()
            if (cid == null) {
                _uiState.update { it.copy(message = "Connect Telegram first") }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, message = null) }
            val result = api.sendMessage(cid, "✅ <b>Logistics Tracker</b>\nTest message from the app.")
            _uiState.update {
                it.copy(
                    isLoading = false,
                    message = if (result.isSuccess) "Message sent!" else result.exceptionOrNull()?.message ?: "Failed"
                )
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            repo.setChatId(null)
            _uiState.update { it.copy(message = "Disconnected") }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    /**
     * Run sync automatically if chat is linked and throttle allows. Call from app start / resume.
     */
    fun runAutoSyncIfNeeded(
        getCurrentCompanyId: () -> String?,
        onAddWeeklyTotal: (ParsedWeeklyTotal) -> Boolean,
        onAddTrip: (ParsedTrip) -> Boolean
    ) {
        viewModelScope.launch {
            if (repo.getChatId() == null) return@launch
            val now = System.currentTimeMillis()
            if (now - lastAutoSyncTimeMs < autoSyncThrottleMs) return@launch
            lastAutoSyncTimeMs = now
            syncFromTelegram(getCurrentCompanyId, onAddWeeklyTotal, onAddTrip)
        }
    }

    /**
     * Process a batch of updates (group batch or one-by-one), send replies, return (messagesSeen, processed).
     */
    private suspend fun processUpdates(
        updates: List<TelegramUpdate>,
        cid: String,
        getCurrentCompanyId: () -> String?,
        onAddWeeklyTotal: (ParsedWeeklyTotal) -> Boolean,
        onAddTrip: (ParsedTrip) -> Boolean
    ): Pair<Int, Int> {
        var messagesSeen = 0
        var processed = 0
        val isGroup = updates.firstOrNull()?.let { it.chatType == "group" || it.chatType == "supergroup" } ?: false
        if (isGroup && updates.isNotEmpty() && gemini.isConfigured()) {
            val combined = updates.map { it.text.trim() }.filter { it.isNotBlank() }.joinToString("\n\n")
            if (combined.isNotBlank()) {
                messagesSeen += updates.size
                Log.d("TelegramSync", "Group batch: ${updates.size} messages, ${combined.length} chars")
                val result = gemini.parseGroupConversation(combined)
                if (result != null) {
                    var addedW = 0
                    var dupW = 0
                    var addedT = 0
                    var dupT = 0
                    result.weeklyTotals.forEach { if (onAddWeeklyTotal(it)) addedW++ else dupW++ }
                    result.trips.forEach { if (onAddTrip(it)) addedT++ else dupT++ }
                    val msg = buildString {
                        append("✅ Group sync: ")
                        if (addedW > 0 || addedT > 0) append("$addedW week(s), $addedT load(s) added.")
                        if (dupW > 0 || dupT > 0) append(" $dupW / $dupT duplicate(s) skipped.")
                        if (addedW == 0 && addedT == 0 && dupW == 0 && dupT == 0) append("No new data extracted.")
                    }
                    api.sendMessage(cid, msg)
                    processed += addedW + addedT
                }
            }
        } else {
            for (u in updates) {
                val text = u.text.trim()
                if (text.isBlank()) continue
                messagesSeen++
                Log.d("TelegramSync", "Parsing message (${text.length} chars): ${text.take(500)}${if (text.length > 500) "…" else "" }")
                var added = false
                var clarification = false
                if (gemini.isConfigured()) {
                    val relayLike = text.length > 100 && (
                        text.contains("Trip", ignoreCase = true) ||
                        text.contains("Rate", ignoreCase = true) ||
                        text.contains("PU", ignoreCase = true) ||
                        text.contains("DEL", ignoreCase = true) ||
                        text.contains("Pickup", ignoreCase = true) ||
                        text.contains("Delivery", ignoreCase = true) ||
                        text.contains("Pu-time", ignoreCase = true) ||
                        text.contains("Pu-address", ignoreCase = true) ||
                        text.contains("Del-time", ignoreCase = true) ||
                        text.contains("Del-address", ignoreCase = true) ||
                        text.contains("Total Rate", ignoreCase = true) ||
                        text.contains("Total Loaded Miles", ignoreCase = true)
                    )
                    if (relayLike) {
                        val relay = gemini.parseRelayDocument(text)
                        if (relay != null) {
                            val trip = relay.toParsedTrip()
                            val addedTrip = onAddTrip(trip)
                            api.sendMessage(cid, if (addedTrip) "✅ Load added from relay: ${relay.tripDetails.id} — ${relay.tripDetails.miles} mi, $${relay.tripDetails.rate}" else "⏭️ Duplicate (same date/route/cost), skipped. Edit in app if needed.")
                            if (addedTrip) processed++
                            added = true
                        }
                    }
                    if (!added) when (val result = gemini.analyzeMessage(text)) {
                        is com.example.myfinance.gemini.AnalysisResult.WeeklyTotalResult -> {
                            val d = result.data
                            val addedWt = onAddWeeklyTotal(result.data)
                            api.sendMessage(cid, if (addedWt) "✅ Week added: ${d.date} — Gross ${d.gross}, Salary ${d.salaryIn}, Diesel ${d.diesel}, Net ${d.salaryIn - d.diesel}" else "⏭️ Duplicate (same date + totals), skipped. Edit in app if needed.")
                            if (addedWt) processed++
                            added = true
                        }
                        is com.example.myfinance.gemini.AnalysisResult.TripResult -> {
                            val t = result.data
                            val addedTrip = onAddTrip(result.data)
                            api.sendMessage(cid, if (addedTrip) "✅ Load added: ${t.pointA} → ${t.pointB}, ${t.miles} mi, $${t.cost}" else "⏭️ Duplicate (same date/route/cost), skipped. Edit in app if needed.")
                            if (addedTrip) processed++
                            added = true
                        }
                        is com.example.myfinance.gemini.AnalysisResult.RequiresClarification -> {
                            api.sendMessage(cid, "⚠️ ${result.message}")
                            clarification = true
                        }
                        null -> { }
                    }
                }
                if (!added && !clarification) {
                    val simple = parseWeeklyTotalSimple(text)
                    if (simple != null) {
                        val addedWt = onAddWeeklyTotal(simple)
                        api.sendMessage(cid, if (addedWt) "✅ Week added: ${simple.date} — Gross ${simple.gross}, Salary ${simple.salaryIn}, Diesel ${simple.diesel}" else "⏭️ Duplicate (same date + totals), skipped. Edit in app if needed.")
                        if (addedWt) processed++
                    }
                }
            }
        }
        return messagesSeen to processed
    }

    /**
     * Sync: fetch new messages, use Gemini (or simple parser) to extract weekly totals and/or trips, add to app and reply in Telegram.
     */
    fun syncFromTelegram(
        getCurrentCompanyId: () -> String?,
        onAddWeeklyTotal: (ParsedWeeklyTotal) -> Boolean,
        onAddTrip: (ParsedTrip) -> Boolean
    ) {
        viewModelScope.launch {
            val cid = repo.getChatId() ?: run {
                _uiState.update { it.copy(message = "Connect Telegram first") }
                return@launch
            }
            _uiState.update { it.copy(isSyncing = true, message = null) }
            var lastId = repo.getLastProcessedUpdateId()
            var totalProcessed = 0
            var totalMessagesSeen = 0
            while (true) {
                val r = api.getUpdates(offset = if (lastId > 0) lastId + 1 else null, timeoutSeconds = 3)
                if (!r.isSuccess) break
                val updates = r.getOrNull()?.filter { it.chatId == cid } ?: break
                if (updates.isEmpty()) break
                val (messagesSeen, processed) = processUpdates(updates, cid, getCurrentCompanyId, onAddWeeklyTotal, onAddTrip)
                totalMessagesSeen += messagesSeen
                totalProcessed += processed
                lastId = updates.maxOf { it.updateId }
                repo.setLastProcessedUpdateId(lastId)
            }
            val hint = if (totalMessagesSeen > 0 && totalProcessed == 0) " Try sending: \"gross 5000 salary 3000 diesel 500\" or add GEMINI_API_KEY for smart parsing."
            else ""
            _uiState.update {
                it.copy(
                    isSyncing = false,
                    message = when {
                        totalProcessed > 0 -> "Synced: $totalProcessed added"
                        totalMessagesSeen > 0 -> "No data parsed from $totalMessagesSeen message(s).$hint"
                        else -> "No new messages."
                    }
                )
            }
        }
    }

    /**
     * Process one message the same way as Telegram sync (relay / Gemini / simple parser).
     * Returns the reply text that would be sent to the user (e.g. "✅ Week added: ...").
     * Use from the in-app chat to "send a request to the bot" without Telegram.
     */
    suspend fun processMessageLocally(
        text: String,
        getCurrentCompanyId: () -> String?,
        onAddWeeklyTotal: (ParsedWeeklyTotal) -> Boolean,
        onAddTrip: (ParsedTrip) -> Boolean
    ): String {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return "Empty message."
        var added = false
        var clarification = false
        var reply = ""

        if (gemini.isConfigured()) {
            val relayLike = trimmed.length > 100 && (
                trimmed.contains("Trip", ignoreCase = true) ||
                trimmed.contains("Rate", ignoreCase = true) ||
                trimmed.contains("PU", ignoreCase = true) ||
                trimmed.contains("DEL", ignoreCase = true) ||
                trimmed.contains("Pickup", ignoreCase = true) ||
                trimmed.contains("Delivery", ignoreCase = true) ||
                trimmed.contains("Pu-time", ignoreCase = true) ||
                trimmed.contains("Pu-address", ignoreCase = true) ||
                trimmed.contains("Del-time", ignoreCase = true) ||
                trimmed.contains("Del-address", ignoreCase = true) ||
                trimmed.contains("Total Rate", ignoreCase = true) ||
                trimmed.contains("Total Loaded Miles", ignoreCase = true)
            )
            if (relayLike) {
                val relay = gemini.parseRelayDocument(trimmed)
                if (relay != null) {
                    added = onAddTrip(relay.toParsedTrip())
                    reply = if (added) "✅ Load added from relay: ${relay.tripDetails.id} — ${relay.tripDetails.miles} mi, $${relay.tripDetails.rate}" else "⏭️ Duplicate (same date/route/cost), skipped. Edit in app if needed."
                }
            }
            if (!added) when (val result = gemini.analyzeMessage(trimmed)) {
                is com.example.myfinance.gemini.AnalysisResult.WeeklyTotalResult -> {
                    val d = result.data
                    added = onAddWeeklyTotal(result.data)
                    reply = if (added) "✅ Week added: ${d.date} — Gross ${d.gross}, Salary ${d.salaryIn}, Diesel ${d.diesel}, Net ${d.salaryIn - d.diesel}" else "⏭️ Duplicate (same date + totals), skipped. Edit in app if needed."
                }
                is com.example.myfinance.gemini.AnalysisResult.TripResult -> {
                    val t = result.data
                    added = onAddTrip(result.data)
                    reply = if (added) "✅ Load added: ${t.pointA} → ${t.pointB}, ${t.miles} mi, $${t.cost}" else "⏭️ Duplicate (same date/route/cost), skipped. Edit in app if needed."
                }
                is com.example.myfinance.gemini.AnalysisResult.RequiresClarification -> {
                    reply = "⚠️ ${result.message}"
                    clarification = true
                }
                null -> { }
            }
        }
        if (!added && !clarification) {
            val simple = parseWeeklyTotalSimple(trimmed)
            if (simple != null) {
                added = onAddWeeklyTotal(simple)
                reply = if (added) "✅ Week added: ${simple.date} — Gross ${simple.gross}, Salary ${simple.salaryIn}, Diesel ${simple.diesel}" else "⏭️ Duplicate (same date + totals), skipped. Edit in app if needed."
            }
        }

        return when {
            added -> reply
            clarification -> reply
            else -> "No data parsed. Try: \"Total Rate: \$1247, Miles: 218\" or \"gross 5000 salary 3000 diesel 500\", or add GEMINI_API_KEY for smart parsing."
        }
    }
}
