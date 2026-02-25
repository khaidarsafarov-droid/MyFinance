package com.truckerload.data.remote

import com.truckerload.data.remote.dto.Content
import com.truckerload.data.remote.dto.GeminiRequest
import com.truckerload.data.remote.dto.Part
import com.truckerload.domain.model.DieselParseResult
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.PaycheckParseResult
import com.truckerload.domain.model.Penalty
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.util.Log
import java.util.Calendar
import java.util.concurrent.TimeUnit

class GeminiService(private val apiKey: String) {

    companion object {
        private val GEMINI_MODELS = listOf("gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash")
    }

    private suspend fun generateWithFallback(request: com.truckerload.data.remote.dto.GeminiRequest): com.truckerload.data.remote.dto.GeminiResponse? {
        for (model in GEMINI_MODELS) {
            runCatching { api.generateContent(model, request) }
                .onSuccess { return it }
                .onFailure { Log.w("GeminiService", "Model $model failed: ${it.message}") }
        }
        return null
    }

    private val api: GeminiApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .apply {
                addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("x-goog-api-key", apiKey)
                        .addHeader("Content-Type", "application/json")
                        .url(chain.request().url.newBuilder().addQueryParameter("key", apiKey).build())
                        .build()
                    chain.proceed(request)
                }
                addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            }
            .build()
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApi::class.java)
    }

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

        Help the user with: interpreting their numbers, summarizing periods, explaining profit/loss, or adding data (loads, paychecks, diesel). Be concise and factual. Answer in the same language the user writes in.
    """.trimIndent()

    private val imageExtractPrompt = "Extract all text from this image. Return only the extracted text, nothing else. For Driver Settlement / settlement summary: include every label and number, especially: Grand Total, Total (under loads), Total (under Deductions), Gross Pay Total, Loads Total, Miles Total, Settlement Date, Cutoff Date. Important: keep the exact label 'Grand Total' and its value separate from other 'Total' lines so salary can be taken from Grand Total only."

    /**
     * Extract text from an image (photo/document image) using Gemini vision. Used for Telegram file/photo handling.
     */
    suspend fun extractTextFromImage(imageBytes: ByteArray, mimeType: String = "image/jpeg"): Result<String> = withContext(Dispatchers.IO) {
        try {
            val base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            val request = com.truckerload.data.remote.dto.GeminiRequest(
                contents = listOf(
                    Content(
                        role = "user",
                        parts = listOf(
                            Part(text = imageExtractPrompt),
                            Part(inlineData = com.truckerload.data.remote.dto.InlineData(mimeType = mimeType, data = base64))
                        )
                    )
                ),
                generationConfig = com.truckerload.data.remote.dto.GenerationConfig(responseMimeType = null)
            )
            val response = generateWithFallback(request)
                ?: return@withContext Result.failure(Exception("All Gemini models failed"))
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: return@withContext Result.failure(Exception("Empty response"))
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send a chat message with optional history and optional app data context.
     * When [appContext] is not null, it is injected into the system instruction so Gemini can answer using internal app data (loads, paychecks, diesel).
     */
    suspend fun chat(
        history: List<Pair<String, String>>,
        userMessage: String,
        appContext: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val systemText = if (!appContext.isNullOrBlank()) {
                android.util.Log.d("GeminiService", "chat: injecting app context (${appContext.length} chars)")
                "$chatSystemInstruction\n\n---\nCurrent data from the app (use this to answer questions about the user's loads, salary, diesel):\n$appContext"
            } else {
                android.util.Log.d("GeminiService", "chat: no app context (internal data not available)")
                chatSystemInstruction
            }
            val contents = history.map { (role, text) ->
                com.truckerload.data.remote.dto.Content(role = role, parts = listOf(com.truckerload.data.remote.dto.Part(text = text)))
            } + listOf(
                com.truckerload.data.remote.dto.Content(role = "user", parts = listOf(com.truckerload.data.remote.dto.Part(text = userMessage)))
            )
            val request = com.truckerload.data.remote.dto.GeminiRequest(
                contents = contents,
                systemInstruction = com.truckerload.data.remote.dto.Content(role = "user", parts = listOf(com.truckerload.data.remote.dto.Part(text = systemText))),
                generationConfig = com.truckerload.data.remote.dto.GenerationConfig(responseMimeType = null)
            )
            val response = generateWithFallback(request)
                ?: return@withContext Result.failure(Exception("All Gemini models failed"))
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext Result.failure(Exception("Empty response from Gemini"))
            Result.success(text)
        } catch (e: Exception) {
            android.util.Log.w("GeminiService", "chat failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun parseLoadFromMessage(rawMessage: String): Result<Load> = withContext(Dispatchers.IO) {
        try {
            val request = GeminiRequest(
                contents = listOf(
                    Content(role = "user", parts = listOf(Part(text = "$parsePrompt\n\n---\n\n$rawMessage")))
                ),
                generationConfig = com.truckerload.data.remote.dto.GenerationConfig(responseMimeType = "application/json")
            )
            val response = generateWithFallback(request)
                ?: return@withContext Result.failure(Exception("All Gemini models failed"))
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: return@withContext Result.failure(Exception("Empty response"))
            val json = JsonParser().parse(text).asJsonObject
            val load = parseJsonToLoad(json, rawMessage)
            Result.success(load)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Extract ALL loads from a message (may contain multiple relay/trip entries).
     * Returns empty list if none found.
     */
    suspend fun parseLoadsFromMessage(rawMessage: String): Result<List<Load>> = withContext(Dispatchers.IO) {
        try {
            val request = GeminiRequest(
                contents = listOf(
                    Content(role = "user", parts = listOf(Part(text = "$parseLoadsPrompt\n\n---\n\n$rawMessage")))
                ),
                generationConfig = com.truckerload.data.remote.dto.GenerationConfig(responseMimeType = "application/json")
            )
            val response = generateWithFallback(request)
                ?: return@withContext Result.failure(Exception("All Gemini models failed"))
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: return@withContext Result.failure(Exception("Empty response"))
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
            val request = GeminiRequest(
                contents = listOf(Content(role = "user", parts = listOf(Part(text = "$paycheckPrompt\n\n---\n\n$text")))),
                generationConfig = com.truckerload.data.remote.dto.GenerationConfig(responseMimeType = "application/json")
            )
            val response = generateWithFallback(request)
                ?: return@withContext Result.failure(Exception("All Gemini models failed"))
            val raw = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: return@withContext Result.failure(Exception("Empty response"))
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
            val request = GeminiRequest(
                contents = listOf(Content(role = "user", parts = listOf(Part(text = "$dieselPrompt\n\n---\n\n$text")))),
                generationConfig = com.truckerload.data.remote.dto.GenerationConfig(responseMimeType = "application/json")
            )
            val response = generateWithFallback(request)
                ?: return@withContext Result.failure(Exception("All Gemini models failed"))
            val raw = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: return@withContext Result.failure(Exception("Empty response"))
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
}
