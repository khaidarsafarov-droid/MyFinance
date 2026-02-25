package com.example.myfinance.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfinance.data.AppData
import com.example.myfinance.gemini.GeminiApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatMessage(val role: String, val text: String) // role: "user" | "model"

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val gemini = GeminiApi()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val isConfigured: Boolean get() = gemini.isConfigured()

    /**
     * Builds context string for Gemini chat: current app data so the model can answer using real numbers.
     * Gemini already receives app logic via system instruction; this block is only the data.
     */
    fun buildContextFromAppData(data: AppData): String {
        val sb = StringBuilder()
        if (data.companies.isEmpty() && data.weeklyTotals.isEmpty() && data.trips.isEmpty() && data.goal == null) {
            sb.append("The user has no companies, weekly totals, loads, or goals yet. They can add data manually in the app or by sending messages to the Telegram bot and tapping Sync from Telegram.")
            return sb.toString()
        }

        sb.append("=== COMPANIES ===\n")
        if (data.companies.isEmpty()) {
            sb.append("None.\n")
        } else {
            data.companies.forEach { c ->
                sb.append("- ").append(c.name)
                if (c.isCurrent) sb.append(" (current)")
                sb.append("\n")
            }
        }

        sb.append("\n=== GOAL ===\n")
        if (data.goal == null) {
            sb.append("No goal set.\n")
        } else {
            val g = data.goal
            val netInPeriod = data.weeklyTotals
                .filter { it.date >= g.periodStart && it.date <= g.periodEnd }
                .sumOf { it.netProfit }
            val achieved = netInPeriod >= g.targetAmount
            sb.append("Target net profit: ").append(formatCurrency(g.targetAmount))
            sb.append(" for period ").append(g.periodStart).append(" to ").append(g.periodEnd)
            sb.append("\nNet in period so far: ").append(formatCurrency(netInPeriod))
            if (achieved) {
                sb.append(" — goal reached.")
                if (g.achievedNotifiedAt != null) sb.append(" (user already notified)")
            } else {
                sb.append(" — ").append(formatCurrency(g.targetAmount - netInPeriod)).append(" to go.")
            }
            sb.append("\n")
        }

        sb.append("\n=== COMPANY CHANGES (last 5) ===\n")
        data.companyChanges.sortedByDescending { it.date }.take(5).forEach { cc ->
            sb.append("- ").append(cc.date).append(" → ").append(cc.companyName).append("\n")
        }
        if (data.companyChanges.isEmpty()) sb.append("None.\n")

        val totalGross = data.weeklyTotals.sumOf { it.gross }
        val totalNet = data.weeklyTotals.sumOf { it.netProfit }
        val totalDiesel = data.weeklyTotals.sumOf { it.diesel }
        val totalMiles = data.weeklyTotals.sumOf { it.miles }
        val tripsMiles = data.trips.sumOf { it.miles }
        val tripsCost = data.trips.sumOf { it.cost }

        sb.append("\n=== AGGREGATES (from weekly totals) ===\n")
        sb.append("Weekly totals count: ").append(data.weeklyTotals.size)
        sb.append("\nTotal Gross: ").append(formatCurrency(totalGross))
        sb.append(", Total Net profit (salaryIn - diesel): ").append(formatCurrency(totalNet))
        sb.append(", Total Diesel: ").append(formatCurrency(totalDiesel))
        sb.append(", Total Miles (from weekly totals): ").append(String.format(java.util.Locale.US, "%.1f", totalMiles))
        sb.append("\n\nLoads (trips) count: ").append(data.trips.size)
        sb.append("\nTrips total miles: ").append(String.format(java.util.Locale.US, "%.1f", tripsMiles))
        sb.append(", Trips total cost: ").append(formatCurrency(tripsCost))
        if (tripsMiles > 0) sb.append(", Avg $/mi: ").append(formatCurrency(tripsCost / tripsMiles))
        sb.append("\n")

        sb.append("\n=== LAST 15 WEEKLY TOTALS (newest first) ===\n")
        data.weeklyTotals.sortedByDescending { it.date }.take(15).forEach { wt ->
            val names = wt.companyIds.mapNotNull { id -> data.companies.find { it.id == id }?.name }.joinToString(", ")
            sb.append("- ").append(wt.date).append(" | ").append(if (names.isEmpty()) "—" else names)
            sb.append(" | Gross ").append(formatCurrency(wt.gross))
            sb.append(", SalaryIn ").append(formatCurrency(wt.salaryIn))
            sb.append(", Diesel ").append(formatCurrency(wt.diesel))
            sb.append(", Net ").append(formatCurrency(wt.netProfit))
            if (wt.miles > 0) sb.append(", Miles ").append(String.format(java.util.Locale.US, "%.1f", wt.miles))
            sb.append("\n")
        }

        sb.append("\n=== LAST 10 LOADS (trips, newest first) ===\n")
        data.trips.sortedByDescending { it.date }.take(10).forEach { t ->
            sb.append("- ").append(t.date).append(" | ").append(t.pointA).append(" → ").append(t.pointB)
            sb.append(" | ").append(String.format(java.util.Locale.US, "%.0f", t.miles)).append(" mi, ").append(formatCurrency(t.cost))
            if (t.miles > 0) sb.append(" ($").append(String.format(java.util.Locale.US, "%.2f", t.cost / t.miles)).append("/mi)")
            sb.append(" | ").append(t.orderNumber)
            sb.append("\n")
        }

        return sb.toString()
    }

    fun sendMessage(userText: String, context: String) {
        if (userText.isBlank()) return
        if (!gemini.isConfigured()) {
            _messages.update { it + ChatMessage("user", userText) + ChatMessage("model", "Gemini is not configured. Add GEMINI_API_KEY to your project's local.properties, then rebuild.") }
            return
        }
        viewModelScope.launch {
            _messages.update { it + ChatMessage("user", userText) }
            _isLoading.value = true
            val result = runCatching { gemini.chat(context, userText) }
            _isLoading.value = false
            _messages.update { list ->
                val toShow = result.getOrElse { e ->
                    val msg = e.message ?: "Could not get a response."
                    when {
                        msg.contains("quota", ignoreCase = true) || msg.contains("exceeded", ignoreCase = true) ->
                            "Quota exceeded. Wait a minute and try again, or check https://ai.google.dev/gemini-api/docs/rate-limits"
                        else -> "Error: $msg"
                    }
                }
                list + ChatMessage("model", toShow)
            }
        }
    }

    /**
     * Send the text to the "bot" (same parsing as Telegram sync): add weekly total or trip if parsed, show reply in chat.
     * [process] is typically TelegramViewModel.processMessageLocally(...).
     */
    fun sendToBot(text: String, process: suspend (String) -> String) {
        val t = text.trim()
        if (t.isBlank()) return
        viewModelScope.launch {
            _messages.update { it + ChatMessage("user", t) }
            _isLoading.value = true
            val reply = runCatching { process(t) }.getOrElse { e -> "Error: ${e.message}" }
            _isLoading.value = false
            _messages.update { it + ChatMessage("model", reply) }
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
    }
}
