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
        Result.success("Локальный анализатор активен (без внешних AI API)")

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
                    append("RPM ниже $2.5 — маржа на милю слабая. ")
                rpm >= 3.0 ->
                    append("RPM $rpm — хороший показатель на текущем периоде. ")
                else ->
                    append("RPM $rpm — средний уровень. ")
            }
            if (fuelShare > 0.3) {
                append("Дизель съедает более 30% вала — проверьте маршруты и заправки. ")
            }
            if (profit < 0) {
                append("Чистая прибыль отрицательная — сократите deadhead и пересмотрите ставки. ")
            }
            if (topStates.isNotEmpty()) {
                append("Топ штаты: ${topStates.take(3).joinToString(", ")}. ")
            }
            if (anomalies.isNotBlank()) {
                append("Аномалии: $anomalies")
            }
        }.trim()

        val actions = buildList {
            if (rpm < 2.5) add("Сфокусируйтесь на рейсах с Total Rate / miles > $2.5")
            if (fuelShare > 0.3) add("Сравните цену дизеля по неделям и выберите дешёвые АЗС")
            if (profit < 0) add("Отложите низкомаржинальные направления до восстановления RPM")
            if (isEmpty()) {
                add("Продолжайте синхронизировать лоуды через Telegram")
                add("Сверяйте зарплату и дизель по неделям")
                add("Отслеживайте RPM в разделе Статистика")
            }
        }

        return Result.success(
            LogisticsInsight(
                insight = insight.ifBlank { "Данные обновлены. Следите за RPM и долей дизеля в расходах." },
                actions = actions.take(3)
            )
        )
    }

    private fun buildReply(userMessage: String, appContext: String?): String {
        val msg = userMessage.lowercase(Locale.ROOT)
        val ctx = appContext.orEmpty()

        return when {
            msg.contains("rpm") || msg.contains("рентаб") || msg.contains("ставк") ->
                "RPM считается как доход на милю. Откройте Статистику — там средний RPM и чистая прибыль по выбранному периоду."
            msg.contains("дизел") || msg.contains("топлив") || msg.contains("fuel") ->
                summarizeDiesel(ctx) ?: "Отправьте чек за дизель боту в Telegram (текст с Total и gallons) — сумма попадёт в журнал."
            msg.contains("зарплат") || msg.contains("paycheck") || msg.contains("settlement") ->
                summarizePaychecks(ctx) ?: "Перешлите текст платёжки (Grand Total / Зарплата) боту — выплата сохранится по неделе."
            msg.contains("лоуд") || msg.contains("груз") || msg.contains("trip") || msg.contains("рейс") ->
                summarizeLoads(ctx) ?: "Перешлите сообщение Amazon Relay с Trip ID / PU# / Total Rate — бот добавит лоуд без дубликатов."
            msg.contains("прибыл") || msg.contains("profit") ->
                "Чистая прибыль = зарплата − дизель за период. Смотрите карточки в Финансах и Статистике."
            msg.contains("помощ") || msg.contains("help") || msg.contains("что умеешь") ->
                """Локальный помощник (без облачного AI):
• Синхронизация лоудов из Telegram по Trip ID
• Зарплата из текста платёжки (Grand Total)
• Дизель из текста чека (Total / gallons)
• RPM и прибыль — в разделе Статистика"""
            else ->
                """Я работаю на правилах внутри приложения, без внешних AI.
Спросите про: лоуды, зарплату, дизель, RPM или прибыль.
Или перешлите данные боту в Telegram для автосинхронизации."""
        }
    }

    private fun summarizeLoads(ctx: String): String? {
        val lines = ctx.lines().filter { it.trimStart().startsWith("-") && "→" in it }
        if (lines.isEmpty()) return null
        val last = lines.takeLast(3)
        return "Последние лоуды:\n${last.joinToString("\n")}"
    }

    private fun summarizePaychecks(ctx: String): String? {
        val lines = ctx.lines().filter { it.contains("Week", ignoreCase = true) && "$" in it }
        if (lines.isEmpty()) return null
        return "Зарплаты:\n${lines.takeLast(3).joinToString("\n")}"
    }

    private fun summarizeDiesel(ctx: String): String? {
        val lines = ctx.lines().filter { it.contains("DIESEL", ignoreCase = true) || (it.contains("Week") && "$" in it) }
        if (lines.isEmpty()) return null
        return "Дизель:\n${lines.takeLast(3).joinToString("\n")}"
    }
}
