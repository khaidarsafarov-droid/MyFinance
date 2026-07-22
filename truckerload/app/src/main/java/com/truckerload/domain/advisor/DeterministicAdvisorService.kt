package com.truckerload.domain.advisor

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Locale

data class LogisticsInsight(
    val insight: String,
    val actions: List<String>
)

/**
 * Rule-based assistant: answers from metrics and keywords, no external AI APIs.
 */
class DeterministicAdvisorService {

    fun chatStream(
        history: List<Pair<String, String>>,
        userMessage: String,
        appContext: String?
    ): Flow<String> = flow {
        val reply = buildReply(userMessage, appContext)
        reply.split(" ").forEachIndexed { index, word ->
            if (index > 0) emit(" ")
            emit(word)
            delay(12)
        }
    }

    fun chat(
        history: List<Pair<String, String>>,
        userMessage: String,
        appContext: String?
    ): Result<String> = Result.success(buildReply(userMessage, appContext))

    fun healthCheck(): Result<String> =
        Result.success("Local analyzer active (no external AI APIs)")

    fun generateInsight(
        rpm: Double,
        profit: Double,
        fuelCost: Double,
        miles: Double,
        topStates: List<String>,
        anomalies: String
    ): Result<LogisticsInsight> {
        val fuelShare = if (profit + fuelCost > 0) fuelCost / (profit + fuelCost) else 0.0
        val insight = buildString {
            when {
                rpm < 2.5 && miles > 0 ->
                    append("RPM below $2.5 — per-mile margin is weak. ")
                rpm >= 3.0 ->
                    append("RPM $rpm — strong result for the current period. ")
                else ->
                    append("RPM $rpm — average level. ")
            }
            if (fuelShare > 0.3) {
                append("Diesel eats more than 30% of gross — review routes and fuel stops. ")
            }
            if (profit < 0) {
                append("Net profit is negative — cut deadhead and revisit rates. ")
            }
            if (topStates.isNotEmpty()) {
                append("Top states: ${topStates.take(3).joinToString(", ")}. ")
            }
            if (anomalies.isNotBlank()) {
                append("Anomalies: $anomalies")
            }
        }.trim()

        val actions = buildList {
            if (rpm < 2.5) add("Focus on loads with Total Rate / miles > $2.5")
            if (fuelShare > 0.3) add("Compare diesel prices week over week and pick cheaper truck stops")
            if (profit < 0) add("Pause low-margin lanes until RPM recovers")
            if (isEmpty()) {
                add("Keep syncing loads via Telegram")
                add("Reconcile paychecks and diesel by week")
                add("Track RPM in the Statistics section")
            }
        }

        return Result.success(
            LogisticsInsight(
                insight = insight.ifBlank { "Data updated. Watch RPM and diesel share of expenses." },
                actions = actions.take(3)
            )
        )
    }

    private fun buildReply(userMessage: String, appContext: String?): String {
        val msg = userMessage.lowercase(Locale.ROOT)
        val ctx = appContext.orEmpty()

        return when {
            msg.contains("rpm") || msg.contains("рентаб") || msg.contains("ставк") || msg.contains("rate") ->
                "RPM is revenue per mile. Open Statistics — average RPM and net profit for the selected period are there."
            msg.contains("дизел") || msg.contains("топлив") || msg.contains("fuel") || msg.contains("diesel") ->
                summarizeDiesel(ctx) ?: "Send a diesel receipt to the Telegram bot (text with Total and gallons) — the amount goes into the journal."
            msg.contains("зарплат") || msg.contains("paycheck") || msg.contains("settlement") || msg.contains("pay") ->
                summarizePaychecks(ctx) ?: "Forward settlement text (Grand Total / Net Pay) to the bot — payout is saved by week."
            msg.contains("лоуд") || msg.contains("груз") || msg.contains("trip") || msg.contains("рейс") || msg.contains("load") ->
                summarizeLoads(ctx) ?: "Forward an Amazon Relay message with Trip ID / PU# / Total Rate — the bot adds the load without duplicates."
            msg.contains("прибыл") || msg.contains("profit") ->
                "Net profit = paycheck − diesel for the period. See cards in Finances and Statistics."
            msg.contains("помощ") || msg.contains("help") || msg.contains("что умеешь") ->
                """Local assistant (no cloud AI):
• Sync loads from Telegram by Trip ID
• Paycheck from settlement text (Grand Total)
• Diesel from receipt text (Total / gallons)
• RPM and profit — in Statistics"""
            else ->
                """I run on in-app rules, without external AI.
Ask about: loads, paycheck, diesel, RPM, or profit.
Or forward data to the Telegram bot for auto-sync."""
        }
    }

    private fun summarizeLoads(ctx: String): String? {
        val lines = ctx.lines().filter { it.trimStart().startsWith("-") && "→" in it }
        if (lines.isEmpty()) return null
        val last = lines.takeLast(3)
        return "Recent loads:\n${last.joinToString("\n")}"
    }

    private fun summarizePaychecks(ctx: String): String? {
        val lines = ctx.lines().filter { it.contains("Week", ignoreCase = true) && "$" in it }
        if (lines.isEmpty()) return null
        return "Paychecks:\n${lines.takeLast(3).joinToString("\n")}"
    }

    private fun summarizeDiesel(ctx: String): String? {
        val lines = ctx.lines().filter { it.contains("DIESEL", ignoreCase = true) || (it.contains("Week") && "$" in it) }
        if (lines.isEmpty()) return null
        return "Diesel:\n${lines.takeLast(3).joinToString("\n")}"
    }
}
