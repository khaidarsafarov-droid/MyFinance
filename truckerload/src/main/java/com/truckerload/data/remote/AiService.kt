package com.truckerload.data.remote

import android.content.Context
import android.util.Log
import com.truckerload.R
import com.truckerload.domain.model.DieselParseResult
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.PaycheckParseResult
import com.truckerload.domain.model.Penalty
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Calendar

class AiService(
    private val cerebrasApiKey: String = "",
    private val cerebrasModel: String = CerebrasService.DEFAULT_MODEL,
    private val context: Context? = null
) {
    data class RealTimeLogisticsInsight(
        val insight: String,
        val actions: List<String>
    )

    private val cerebrasService: CerebrasService? = if (cerebrasApiKey.isNotBlank()) {
        CerebrasService(cerebrasApiKey, cerebrasModel.ifBlank { CerebrasService.DEFAULT_MODEL })
    } else null

    private fun text(resId: Int, fallback: String, vararg args: Any): String =
        context?.getString(resId, *args)?.takeIf { it.isNotBlank() } ?: if (args.isEmpty()) fallback else String.format(fallback, *args)

    private fun defaultInsightActions(): List<String> = listOf(
        text(R.string.ai_action_routes_ky, "Посмотреть маршруты в KY"),
        text(R.string.ai_action_prices_az, "Анализ цен в AZ"),
        text(R.string.ai_action_profit_fl, "Проверить рентабельность FL")
    )

    private val parsePrompt = """
        Context: TruckerLoad is the driver's personal financial journal (loads, miles, salary, diesel, net profit). Extract one load/trip from the message for this journal.
        Return ONLY valid JSON (no markdown) with this structure:
        {
          "tripId": "string",
          "date": "YYYY-MM-DD",
          "totalRate": number,
          "totalMiles": number,
          "pointA": "city, state",
          "pointB": "city, state",
          "puCount": number,
          "delCount": number,
          "stops": [...],
          "penalties": [{ "description": "string", "amount": number }]
        }
        If something is missing, use sensible defaults. amount in penalties is negative for fines.
    """.trimIndent()

    private val parseLoadsPrompt = """
        Роль: Ты — системный интегратор между мессенджером и базой данных приложения TruckerLoad.

        Триггер: Как только получено текстовое сообщение (СМС/Лоуд), извлеки все грузы для синхронизации.

        Логика извлечения (Trip ID):
        - Разбери входящие данные на отдельные объекты (грузы). Каждый груз имеет уникальный Trip ID.
        - Извлекай только объекты с валидными данными: tripId, маршрут (pointA/pointB), totalRate > 0.
        - Если данных нет или объект пустой — не включай его в результат.
        - Никогда не создавай пустые записи.

        Умные метки времени:
        - date в JSON = load_date (рабочая дата груза). Извлекай из сообщения (дата рейса, дата лоуда).
        - creation_date (дата добавления) присваивается автоматически при сохранении и не меняется.
        - При редактировании обновляется только last_modified, creation_date сохраняется.
        - Иерархия Год > Месяц > День: год (year_folder) и месяц (month_folder) извлекаются из full_date автоматически для группировки в архиве.

        Верни ТОЛЬКО валидный JSON (без markdown):
        {
          "loads": [
            {
              "tripId": "string (обязательно, уникальный ID груза)",
              "date": "YYYY-MM-DD",
              "totalRate": number,
              "totalMiles": number,
              "pointA": "city, state",
              "pointB": "city, state",
              "puCount": number,
              "delCount": number,
              "stops": [],
              "penalties": []
            }
          ]
        }
        - Один груз: массив из одного элемента. Несколько грузов: все в массиве.
        - Нет валидных грузов: { "loads": [] }.
    """.trimIndent()

    private val chatSystemInstruction = """
        You are an assistant for the TruckerLoad app — a driver's personal financial journal.

        App essence: only numbers and facts, no statuses or tracking.
        - Loads per week / month / year
        - Miles driven
        - Salary (earnings)
        - Diesel (fuel spend)
        - Net profit = salary − diesel
        - Full trip history with details (route, rate, miles, stops)

        You have 10 analytics skills. Use them when relevant:
        1. COMPARISON — Compare current period vs previous (week/month). Show ↑/↓ and % change. For expenses (diesel): decrease = good (green), increase = bad (red).
        2. FORECAST — Predict expected weekly gross from last 8 weeks average. Classify: ABOVE (ahead), ON_TRACK, BELOW (behind). Suggest actions if behind.
        3. ROUTE STATS — Analyze routes (pointA → pointB). Rank by $/mi, total earned, load count. Recommend best routes, warn about low $/mi (<$1.40).
        4. ACTIVITY HEATMAP — Show daily/weekly activity intensity. Identify busy vs empty periods. Suggest when to push for more loads.
        5. TAX TRACKER — Income, deductions (diesel, per diem), taxable income, SE tax, federal tax. Remind about quarterly deadlines (Apr 15, Jun 15, Sep 15, Jan 15).
        6. FUEL ANALYTICS — MPG, $/gallon, cost per 100 miles. Compare to previous period. Warn if MPG dropped >5% or price up >10%.
        7. GOALS — Set income/miles/net profit goals. Track progress, predict if on track. Suggest daily required rate to hit target.
        8. FINANCIAL ADVISOR — Answer questions using app data. Give actionable advice: improve income, cut costs, optimize routes, tax planning.
        9. EXPORT — CSV/PDF export by week/month/year. Help user understand what to export for accountant or records.
        10. SMART NOTIFICATIONS — Missing paycheck, missing diesel, below-average week, quarterly tax countdown. Remind user to add data or prepare.

        Help the user with: interpreting numbers, summarizing periods, explaining profit/loss, adding data, or using any of the 10 skills above. Be concise and factual. Answer in the same language the user writes in.
    """.trimIndent()

    suspend fun extractTextFromImage(imageBytes: ByteArray, mimeType: String = "image/jpeg"): Result<String> =
        Result.failure(Exception("Image OCR is unavailable in Cerebras-only mode"))

    /** Streaming chat via Cerebras Chat Completions API. */
    fun chatStream(
        history: List<Pair<String, String>>,
        userMessage: String,
        appContext: String? = null
    ): Flow<String> = flow {
        val systemText = if (!appContext.isNullOrBlank()) {
            "$chatSystemInstruction\n\n---\nCurrent data from the app (use this to answer questions about the user's loads, salary, diesel):\n$appContext"
        } else chatSystemInstruction
        if (cerebrasService == null) throw IllegalStateException(text(R.string.ai_key_not_configured, "Cerebras API key is not configured"))
        cerebrasService.chatStream(history, systemText, userMessage).collect { emit(it) }
    }.flowOn(Dispatchers.IO)

    /** Send a chat message via Cerebras Chat Completions API. */
    suspend fun chat(
        history: List<Pair<String, String>>,
        userMessage: String,
        appContext: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val systemText = if (!appContext.isNullOrBlank()) {
            android.util.Log.d("AiService", "chat: injecting app context (${appContext.length} chars)")
            "$chatSystemInstruction\n\n---\nCurrent data from the app (use this to answer questions about the user's loads, salary, diesel):\n$appContext"
        } else {
            android.util.Log.d("AiService", "chat: no app context (internal data not available)")
            chatSystemInstruction
        }
        val svc = cerebrasService ?: return@withContext Result.failure(Exception(text(R.string.ai_key_not_configured, "Cerebras API key is not configured")))
        svc.chat(history, systemText, userMessage)
    }

    suspend fun healthCheck(): Result<String> = withContext(Dispatchers.IO) {
        val svc = cerebrasService ?: return@withContext Result.failure(Exception(text(R.string.ai_key_not_configured, "Cerebras API key is not configured")))
        svc.healthCheck().map { text(R.string.ai_health_ok, "AI health check OK (%s): %s", cerebrasModel, it.trim()) }
    }

    suspend fun generateRealTimeLogisticsInsight(
        userName: String,
        rpm: Double,
        profit: Double,
        fuelCost: Double,
        miles: Double,
        topStates: List<String>,
        anomalies: String
    ): Result<RealTimeLogisticsInsight> = withContext(Dispatchers.IO) {
        val svc = cerebrasService ?: return@withContext Result.failure(Exception(text(R.string.ai_key_not_configured, "Cerebras API key is not configured")))
        val systemPrompt = """
            Ты — финансовый директор логистической компании.
            Твоя задача: проанализировать JSON с данными (прибыль, RPM, расходы) и выдать ОДИН критически важный инсайт.
            Если RPM ниже $2.5, предложи решение. Если расходы на дизель > 30% от вала, укажи на это.
            Будь краток, используй профессиональный сленг (RPM, Deadhead, Layover).
            Обращайся к пользователю только на «Вы», без имени.
            Верни ТОЛЬКО JSON без markdown:
            {
              "insight": "короткий совет на русском (1-2 предложения)",
              "actions": ["действие 1", "действие 2", "действие 3"]
            }
        """.trimIndent()

        val userPrompt = """
            Контекст (структурированные данные):
            {
              "user_name": "driver",
              "current_metrics": {
                "rpm": $rpm,
                "profit": $profit,
                "fuel_cost": $fuelCost,
                "miles": $miles
              },
              "top_states": ${topStates.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }},
              "anomalies": "$anomalies"
            }
        """.trimIndent()

        val response = svc.chat(
            messages = emptyList(),
            systemInstruction = systemPrompt,
            userMessage = userPrompt,
            temperature = 0.7,
            topP = 0.9
        ).getOrElse { return@withContext Result.failure(it) }

        val jsonText = extractJson(response)
        if (jsonText == null) {
            return@withContext Result.success(
                RealTimeLogisticsInsight(
                    insight = response.trim().ifBlank {
                        text(
                            R.string.ai_insight_fallback,
                            "Данные обновлены. Сфокусируйтесь на самых рентабельных направлениях."
                        )
                    },
                    actions = defaultInsightActions()
                )
            )
        }

        return@withContext runCatching {
            val json = JSONObject(jsonText)
            val insight = json.optString("insight").ifBlank {
                text(
                    R.string.ai_insight_default,
                    "В KY рентабельность выше среднего, а в AZ дизель съедает маржу. Сместите фокус на более прибыльные рейсы."
                )
            }
            val actionsArr = json.optJSONArray("actions")
            val actions = buildList {
                if (actionsArr != null) {
                    for (i in 0 until actionsArr.length()) {
                        val action = actionsArr.optString(i).trim()
                        if (action.isNotBlank()) add(action)
                    }
                }
            }.takeIf { it.isNotEmpty() } ?: defaultInsightActions()
            RealTimeLogisticsInsight(insight = insight, actions = actions.take(3))
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun parseLoadFromMessage(rawMessage: String): Result<Load> = withContext(Dispatchers.IO) {
        try {
            val text = parseJsonWithAi(parsePrompt, rawMessage).getOrElse { return@withContext Result.failure(it) }
            val json = JsonParser().parse(text).asJsonObject
            val load = parseJsonToLoad(json, rawMessage)
            Result.success(load)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Parse JSON using Cerebras only. */
    private suspend fun parseJsonWithAi(prompt: String, rawInput: String): Result<String> = withContext(Dispatchers.IO) {
        val svc = cerebrasService ?: return@withContext Result.failure(Exception(text(R.string.ai_key_not_configured, "Cerebras API key is not configured")))
        val cerebrasResult = svc.chat(emptyList(), prompt, rawInput)
        if (cerebrasResult.isSuccess) {
            val text = cerebrasResult.getOrNull()?.trim()
            if (!text.isNullOrBlank()) return@withContext Result.success(text)
        }
        Log.w("AiService", "Cerebras JSON parse failed")
        Result.failure(cerebrasResult.exceptionOrNull() ?: Exception("All providers failed"))
    }

    /**
     * Extract ALL loads from a message (may contain multiple relay/trip entries).
     * Returns empty list if none found.
     */
    suspend fun parseLoadsFromMessage(rawMessage: String): Result<List<Load>> = withContext(Dispatchers.IO) {
        try {
            val text = parseJsonWithAi(parseLoadsPrompt, rawMessage).getOrElse { return@withContext Result.failure(it) }
            val json = JsonParser().parse(text).asJsonObject
            val arr = json.getAsJsonArray("loads")
            val loads = if (arr != null) {
                (0 until arr.size()).mapNotNull { i ->
                    runCatching {
                        val obj = arr.get(i)
                        if (obj.isJsonObject) parseJsonToLoad(obj.asJsonObject, rawMessage) else null
                    }.getOrNull()
                }
            } else if (json.get("tripId") != null) {
                // Fallback: один лоуд в старом формате
                listOf(parseJsonToLoad(json, rawMessage))
            } else {
                emptyList()
            }
            Result.success(loads)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseJsonToLoad(json: com.google.gson.JsonObject, rawMessage: String): Load {
        fun JsonElement?.str(): String? = this?.takeIf { !it.isJsonNull }?.asString
        fun JsonElement?.numDouble(): Double? = this?.takeIf { !it.isJsonNull }?.asDouble
        fun JsonElement?.numInt(): Int? = this?.takeIf { !it.isJsonNull }?.asInt
        val tripId = (json.get("tripId") as? JsonElement).str() ?: "T-UNKNOWN"
        val id = tripId
        val date = (json.get("date") as? JsonElement).str() ?: ""
        val totalRate = (json.get("totalRate") as? JsonElement).numDouble() ?: 0.0
        val totalMiles = (json.get("totalMiles") as? JsonElement).numDouble() ?: 0.0
        val pointA = (json.get("pointA") as? JsonElement).str() ?: ""
        val pointB = (json.get("pointB") as? JsonElement).str() ?: ""
        val puCount = (json.get("puCount") as? JsonElement).numInt() ?: 0
        val delCount = (json.get("delCount") as? JsonElement).numInt() ?: 0
        val (weekNumber, year) = dateToWeekAndYear(date)
        val now = System.currentTimeMillis()
        val stops = json.getAsJsonArray("stops")?.let { arr ->
            (0 until arr.size()).map { i ->
                val o = arr.get(i).asJsonObject
                Stop(
                    id = 0,
                    loadId = id,
                    stopNumber = o.get("stopNumber")?.takeIf { !it.isJsonNull }?.asInt ?: (i + 1),
                    type = if (o.get("type")?.takeIf { !it.isJsonNull }?.asString.equals("DEL", ignoreCase = true)) StopType.DEL else StopType.PU,
                    puNumber = o.get("puNumber")?.takeIf { !it.isJsonNull }?.asString,
                    note = o.get("note")?.takeIf { !it.isJsonNull }?.asString,
                    scheduledTime = o.get("scheduledTime")?.takeIf { !it.isJsonNull }?.asString ?: "",
                    timezone = o.get("timezone")?.takeIf { !it.isJsonNull }?.asString ?: "",
                    facilityCode = o.get("facilityCode")?.takeIf { !it.isJsonNull }?.asString,
                    fullAddress = o.get("fullAddress")?.takeIf { !it.isJsonNull }?.asString ?: "",
                    city = o.get("city")?.takeIf { !it.isJsonNull }?.asString ?: "",
                    state = o.get("state")?.takeIf { !it.isJsonNull }?.asString ?: "",
                    zip = o.get("zip")?.takeIf { !it.isJsonNull }?.asString ?: ""
                )
            }
        } ?: emptyList()
        val penalties = json.getAsJsonArray("penalties")?.let { arr ->
            (0 until arr.size()).map { i ->
                val o = arr.get(i).asJsonObject
                Penalty(
                    id = 0,
                    loadId = id,
                    description = o.get("description")?.takeIf { !it.isJsonNull }?.asString ?: "",
                    amount = o.get("amount")?.takeIf { !it.isJsonNull }?.asDouble ?: 0.0
                )
            }
        } ?: emptyList()
        return Load(
            id = id,
            tripId = tripId,
            date = date,
            totalRate = totalRate,
            totalMiles = totalMiles,
            pointA = pointA,
            pointB = pointB,
            puCount = puCount,
            delCount = delCount,
            weekNumber = weekNumber,
            year = year,
            rawMessage = rawMessage,
            parsedAt = now,
            updatedAt = now,
            stops = stops,
            penalties = penalties
        )
    }

    private fun dateToWeekAndYear(dateStr: String): Pair<Int, Int> {
        if (dateStr.length < 10) return Pair(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR), Calendar.getInstance().get(Calendar.YEAR))
        return try {
            val parts = dateStr.split("-")
            if (parts.size != 3) return Pair(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR), Calendar.getInstance().get(Calendar.YEAR))
            val y = parts[0].toIntOrNull() ?: return Pair(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR), Calendar.getInstance().get(Calendar.YEAR))
            val m = parts[1].toIntOrNull()?.minus(1) ?: return Pair(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR), Calendar.getInstance().get(Calendar.YEAR))
            val d = parts[2].toIntOrNull() ?: return Pair(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR), Calendar.getInstance().get(Calendar.YEAR))
            val cal = Calendar.getInstance()
            cal.set(y, m, d)
            Pair(cal.get(Calendar.WEEK_OF_YEAR), cal.get(Calendar.YEAR))
        } catch (_: Exception) {
            Pair(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR), Calendar.getInstance().get(Calendar.YEAR))
        }
    }

    private val paycheckPrompt = """
        Задача: проанализируй текст (Driver Settlement / платёжка) и извлеки данные для раздела "зарплата".

        ВАЖНО: Данные зарплаты получить ТОЛЬКО из поля "Зарплата" (или "Grand Total" в английских платёжках).
        - Ищи значение после подписи "Зарплата" или "Grand Total" — это финальная сумма к выплате.
        - Игнорируй "Gross Pay Total", "Total Deductions", итоги по лодам, любые промежуточные суммы.
        - netAmount = только число из поля "Зарплата" / "Grand Total". Другие поля НЕ использовать.
        - Дата — Settlement Date или Cutoff Date в формате DD.MM.YYYY или YYYY-MM-DD.

        Верни ответ строго в формате JSON, без пояснений и лишних знаков:
        {
          "driverName": string | null,
          "weekStartDate": string (YYYY-MM-DD) | null,
          "weekEndDate": string (YYYY-MM-DD) | null,
          "grossAmount": number | null,
          "netAmount": number,
          "currency": "USD",
          "confidence": "high" | "medium" | "low"
        }
        netAmount = ТОЛЬКО из поля "Зарплата" или "Grand Total". Если не найден — confidence: "low", netAmount: 0.
    """.trimIndent()

    private val dieselPrompt = """
        Контекст: TruckerLoad — личный финансовый журнал водителя (лоуды, мили, зарплата, дизель, чистая прибыль = зарплата − дизель). Нужна сумма расхода на топливо для журнала.
        Извлеки из текста/чека за дизель данные и верни ТОЛЬКО валидный JSON (no markdown):
        {
          "date": string (YYYY-MM-DD) | null,
          "totalAmount": number,
          "gallons": number | null,
          "pricePerGallon": number | null,
          "location": string | null,
          "vendor": string | null,
          "currency": "USD",
          "confidence": "high" | "medium" | "low"
        }
        totalAmount — САМОЕ ВАЖНОЕ: итоговая сумма за топливо.
    """.trimIndent()

    suspend fun parsePaycheckFromText(text: String): Result<PaycheckParseResult> = withContext(Dispatchers.IO) {
        try {
            val raw = parseJsonWithAi(paycheckPrompt, text).getOrElse { return@withContext Result.failure(it) }
            val json = JsonParser().parse(raw).asJsonObject
            fun JsonElement?.str(): String? = this?.takeIf { !it.isJsonNull }?.asString
            fun JsonElement?.num(): Double? = this?.takeIf { !it.isJsonNull }?.asDouble
            val result = PaycheckParseResult(
                driverName = (json.get("driverName") as? JsonElement).str(),
                weekStartDate = (json.get("weekStartDate") as? JsonElement).str(),
                weekEndDate = (json.get("weekEndDate") as? JsonElement).str(),
                grossAmount = (json.get("grossAmount") as? JsonElement).num(),
                netAmount = (json.get("netAmount") as? JsonElement).num() ?: 0.0,
                confidence = (json.get("confidence") as? JsonElement).str() ?: "low"
            )
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun parseDieselFromText(text: String): Result<DieselParseResult> = withContext(Dispatchers.IO) {
        try {
            val raw = parseJsonWithAi(dieselPrompt, text).getOrElse { return@withContext Result.failure(it) }
            val json = JsonParser().parse(raw).asJsonObject
            fun JsonElement?.str(): String? = this?.takeIf { !it.isJsonNull }?.asString
            fun JsonElement?.num(): Double? = this?.takeIf { !it.isJsonNull }?.asDouble
            val result = DieselParseResult(
                date = (json.get("date") as? JsonElement).str(),
                totalAmount = (json.get("totalAmount") as? JsonElement).num() ?: 0.0,
                gallons = (json.get("gallons") as? JsonElement).num(),
                pricePerGallon = (json.get("pricePerGallon") as? JsonElement).num(),
                location = (json.get("location") as? JsonElement).str(),
                vendor = (json.get("vendor") as? JsonElement).str(),
                confidence = (json.get("confidence") as? JsonElement).str() ?: "low"
            )
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractJson(raw: String): String? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return raw.substring(start, end + 1)
    }
}
