package com.example.myfinance.gemini

import com.example.myfinance.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiApi {

    private val apiKey: String = BuildConfig.GEMINI_API_KEY
    // Use model with free tier quota (gemini-2.5-flash). If quota exceeded, try again later or check AI Studio billing.
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun isConfigured(): Boolean = apiKey.isNotBlank()

    /**
     * Extract week summary: date, gross, miles, salaryIn, diesel, company names (optional).
     * Net profit = salaryIn - diesel.
     */
    suspend fun parseWeeklyTotalFromMessage(message: String): ParsedWeeklyTotal? = withContext(Dispatchers.IO) {
        try {
            val prompt = """
            From this message extract a WEEK'S TOTAL as JSON. Use today's date if no date. Use 0 for missing numbers.
            Reply with ONLY valid JSON: {"date":"YYYY-MM-DD","gross":number,"miles":number,"salaryIn":number,"diesel":number,"companyNames":["optional names"]}
            CRITICAL: gross must be taken ONLY from "Total Rate" or "Gross" field in the message (the dollar amount after that label). Do NOT use numbers from Trip ID, PU#, order number, or other codes (e.g. 1152 from 1152JRP4B is wrong).
            miles must be taken ONLY from "Total Loaded Miles" or "Miles" (e.g. "217.84 mi"). Use 0 if not found.
            salaryIn = salary that came in. Net profit = salaryIn - diesel.
            Message: $message
        """.trimIndent()
            val response = generateContent(prompt)
            parseJsonToWeeklyTotal(response)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseJsonToWeeklyTotal(text: String): ParsedWeeklyTotal? {
        return try {
            val cleaned = text.trim().removeSurrounding("```json", "```").trim()
            val obj = JSONObject(cleaned)
            val date = obj.optString("date").takeIf { it.isNotBlank() } ?: return null
            val gross = obj.optDouble("gross", -1.0)
            val miles = obj.optDouble("miles", -1.0)
            val salaryIn = obj.optDouble("salaryIn", obj.optDouble("profit", -1.0))
            val diesel = obj.optDouble("diesel", -1.0)
            if (gross < 0 && salaryIn < 0 && diesel < 0) return null
            val companyNames = mutableListOf<String>()
            obj.optJSONArray("companyNames")?.let { arr ->
                for (i in 0 until arr.length()) arr.optString(i).takeIf { it.isNotBlank() }?.let { companyNames.add(it) }
            }
            ParsedWeeklyTotal(
                date = date,
                gross = if (gross >= 0) gross else 0.0,
                miles = if (miles >= 0) miles else 0.0,
                salaryIn = if (salaryIn >= 0) salaryIn else 0.0,
                diesel = if (diesel >= 0) diesel else 0.0,
                companyNames = companyNames
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Extract one load/trip: point A, B, miles, cost, start/end time, order number.
     */
    suspend fun parseTripFromMessage(message: String): ParsedTrip? = withContext(Dispatchers.IO) {
        try {
            val prompt = """
            From this message extract ONE LOAD/TRIP (point A to B) as JSON. Use today's date if no date. Use empty string for missing text, 0 for missing numbers.
            Reply with ONLY valid JSON: {"pointA":"city/state","pointB":"city/state","miles":number,"cost":number,"startTime":"ISO or time","endTime":"ISO or time","orderNumber":"string","date":"YYYY-MM-DD"}
            costPerMile can be computed as cost/miles.
            Message: $message
        """.trimIndent()
            val response = generateContent(prompt)
            parseJsonToTrip(response)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseJsonToTrip(text: String): ParsedTrip? {
        return try {
            val cleaned = text.trim().removeSurrounding("```json", "```").trim()
            val obj = JSONObject(cleaned)
            val pointA = obj.optString("pointA").trim().ifBlank { return null }
            val pointB = obj.optString("pointB").trim().ifBlank { return null }
            val miles = obj.optDouble("miles", 0.0)
            val cost = obj.optDouble("cost", 0.0)
            val startTime = obj.optString("startTime").trim().ifBlank { "—" }
            val endTime = obj.optString("endTime").trim().ifBlank { "—" }
            val orderNumber = obj.optString("orderNumber").trim().ifBlank { "—" }
            val date = obj.optString("date").trim().ifBlank { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()) }
            ParsedTrip(
                pointA = pointA,
                pointB = pointB,
                miles = miles,
                cost = cost,
                startTime = startTime,
                endTime = endTime,
                orderNumber = orderNumber,
                date = date
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * System instruction so Gemini understands the full app logic (terminology, entities, flows).
     */
    private val systemInstructionChat = """
        You are the AI assistant for the "Logistics Tracker" / "MyFinance" Android app. You understand its full logic and answer using the context data provided with each message.

        APP PURPOSE: Track income, expenses, and loads for trucking/logistics. Data can be entered manually or by syncing from a Telegram bot (user sends messages, app parses them into weekly totals or loads).

        TERMINOLOGY:
        - Gross = total revenue for the period (e.g. week); often comes from "Total Rate" in relay documents.
        - Salary In = salary or profit received (money that actually came in).
        - Diesel = fuel cost (expense).
        - Net Profit = Salary In minus Diesel (formula: netProfit = salaryIn - diesel). This is the key bottom-line number.
        - Weekly Total = one record per week: date, gross, miles, salaryIn, diesel, and which companies. One row per week.
        - Trip / Load = one haul from point A to point B: pointA, pointB, miles, cost, date, order number. Trips do NOT have gross/salary/diesel; they have cost (rate) and miles. Cost per mile = cost / miles.
        - Company = a trucking company the user works for. User can have multiple companies; one is "current". When they add a weekly total or load, it can be tied to the current company.
        - Company Change = history of when the user switched current company (date + company name).
        - Goal = a target net profit for a date range (periodStart–periodEnd). When total net in that period reaches targetAmount, the app notifies the user. achievedNotifiedAt marks if the notification was already sent.

        DATA FLOWS:
        - Adding a weekly total: user either enters manually (date, gross, miles, salaryIn, diesel) or sends a message to Telegram (e.g. "Total Rate: $1247, Miles: 218, Diesel: $100"); on Sync, the app parses and adds one WeeklyTotal.
        - Adding a load/trip: user enters manually or sends relay-style message to Telegram (pickup/delivery, Total Rate, Total Loaded Miles); app parses and adds one Trip with pointA, pointB, miles, cost, date.
        - All monetary amounts in context are in the same currency (e.g. USD). Use the numbers as given; net = salaryIn - diesel for weekly totals.

        YOUR BEHAVIOR:
        - Answer only from the context data provided. If context is empty, say the user has no data yet and how they can add it (manual or Telegram sync).
        - When asked about totals, net profit, or goals, use the weekly totals and formulas above.
        - When asked about loads or trips, use the trips list (point A→B, miles, cost, date).
        - Be concise and accurate. Use the exact numbers from the context; do not invent data.
    """.trimIndent()

    /**
     * Chat: send user message with app data context. Uses system instruction so Gemini understands app logic; context contains the actual user data.
     */
    suspend fun chat(context: String, userMessage: String): String = withContext(Dispatchers.IO) {
        val userPrompt = if (context.isBlank()) {
            userMessage
        } else {
            """
                Current app data (use this as the source of truth):

                $context

                User question: $userMessage
            """.trimIndent()
        }
        generateContentWithSystem(systemInstructionChat, userPrompt)
    }

    /**
     * Same as generateContentWithSystemAndJson but returns plain text (no JSON response type). For chat.
     */
    private suspend fun generateContentWithSystem(systemInstruction: String, userText: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw Exception("Gemini API key not set.")
        val body = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemInstruction) })
                })
            })
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", userText) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.2)
                put("maxOutputTokens", 1024)
            })
        }.toString()
        val request = Request.Builder()
            .url("$baseUrl?key=$apiKey")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val msg = try {
                    JSONObject(bodyStr).optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                } catch (_: Exception) {
                    "HTTP ${response.code}"
                }
                throw Exception("Gemini API: $msg")
            }
            val json = JSONObject(bodyStr)
            val candidates = json.optJSONArray("candidates")
            val first = candidates?.optJSONObject(0)
            val content = first?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val part = parts?.optJSONObject(0)
            part?.optString("text")?.trim() ?: throw Exception("No text in response")
        }
    }

    private val systemInstructionDocumentAnalyst = """
        You are an expert at extracting structured data for a logistics/finance app. Strict rules:
        PRIORITY — LOADS GO TO LOADS, NOT WEEKS: If the message describes ONE load/shipment (pickup and delivery locations, Total Rate or rate/cost, miles), ALWAYS return type "trip". Never return "weekly_total" for a load. Return "weekly_total" ONLY when the message is explicitly a week summary (e.g. "gross 5000 salary 3000 diesel 500" or "week total: gross X, diesel Y" with no specific route/pickup/delivery).
        1) TRIP = one load from point A to point B: pointA, pointB, miles, cost, startTime, endTime, orderNumber, date. Use cost from "Total Rate" when present. If there is any pickup/delivery/route/Total Rate/Total Loaded Miles, it is a trip.
        2) WEEKLY_TOTAL = one record per week: date, gross, miles, salaryIn, diesel, companyNames. Use ONLY when the message is clearly a week summary (no route, no pickup/delivery addresses).
        3) Return exactly ONE JSON object:
        - For a single load (pickup, delivery, rate, miles): {"type":"trip",...}
        - For week summary only (no route): {"type":"weekly_total",...}
        - Only if genuinely MULTIPLE loads or unclear: {"type":"requires_clarification","message":"..."}
        If the message has pickup/delivery or Total Rate + miles for one shipment, always "trip". Output only valid JSON.
    """.trimIndent()

    /**
     * Single structured analysis: classify message as weekly_total, trip, or requires_clarification.
     * Ensures diesel/gross/salary only in weekly_total; trip never has them.
     * Falls back to legacy two-call parsing if structured call fails (e.g. model doesn't support systemInstruction).
     */
    suspend fun analyzeMessage(text: String): AnalysisResult? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        try {
            val userPrompt = "Analyze this message and return ONE JSON object (weekly_total, trip, or requires_clarification):\n\n$text"
            val response = generateContentWithSystemAndJson(systemInstructionDocumentAnalyst, userPrompt)
            parseAnalysisResult(response)
        } catch (_: Exception) {
            analyzeMessageLegacy(text)
        }
    }

    /** Fallback: try trip first, then weekly total. Trip has NO diesel/gross/salary. */
    private suspend fun analyzeMessageLegacy(text: String): AnalysisResult? {
        val trip = parseTripFromMessage(text)
        if (trip != null) return AnalysisResult.TripResult(trip)
        val wt = parseWeeklyTotalFromMessage(text)
        if (wt != null) return AnalysisResult.WeeklyTotalResult(wt)
        return null
    }

    private fun parseAnalysisResult(jsonText: String): AnalysisResult? {
        return try {
            val cleaned = jsonText.trim().removeSurrounding("```json", "```").trim()
            val obj = JSONObject(cleaned)
            when (obj.optString("type").lowercase()) {
                "weekly_total" -> {
                    val date = obj.optString("date").takeIf { it.isNotBlank() } ?: return null
                    ParsedWeeklyTotal(
                        date = date,
                        gross = obj.optDouble("gross", 0.0).coerceAtLeast(0.0),
                        miles = obj.optDouble("miles", 0.0).coerceAtLeast(0.0),
                        salaryIn = obj.optDouble("salaryIn", 0.0).coerceAtLeast(0.0),
                        diesel = obj.optDouble("diesel", 0.0).coerceAtLeast(0.0),
                        companyNames = mutableListOf<String>().apply {
                            obj.optJSONArray("companyNames")?.let { arr ->
                                for (i in 0 until arr.length()) arr.optString(i).takeIf { it.isNotBlank() }?.let { add(it) }
                            }
                        }
                    ).let { AnalysisResult.WeeklyTotalResult(it) }
                }
                "trip" -> {
                    val pointA = obj.optString("pointA").trim().ifBlank { return null }
                    val pointB = obj.optString("pointB").trim().ifBlank { return null }
                    ParsedTrip(
                        pointA = pointA,
                        pointB = pointB,
                        miles = obj.optDouble("miles", 0.0).coerceAtLeast(0.0),
                        cost = obj.optDouble("cost", 0.0).coerceAtLeast(0.0),
                        startTime = obj.optString("startTime").trim().ifBlank { "—" },
                        endTime = obj.optString("endTime").trim().ifBlank { "—" },
                        orderNumber = obj.optString("orderNumber").trim().ifBlank { "—" },
                        date = obj.optString("date").trim().ifBlank {
                            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                        }
                    ).let { AnalysisResult.TripResult(it) }
                }
                "requires_clarification" -> {
                    val msg = obj.optString("message").trim().ifBlank { "Unclear or contradictory data" }
                    AnalysisResult.RequiresClarification(msg)
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Call with system instruction and JSON response mode. Returns raw text (JSON string).
     */
    private suspend fun generateContentWithSystemAndJson(systemInstruction: String, userText: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw Exception("Gemini API key not set.")
        val body = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemInstruction) })
                })
            })
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", userText) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.1)
                put("maxOutputTokens", 1024)
                put("response_mime_type", "application/json")
            })
        }.toString()
        val request = Request.Builder()
            .url("$baseUrl?key=$apiKey")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val msg = try {
                    JSONObject(bodyStr).optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                } catch (_: Exception) {
                    "HTTP ${response.code}"
                }
                throw Exception("Gemini API: $msg")
            }
            val json = JSONObject(bodyStr)
            val candidates = json.optJSONArray("candidates")
            val first = candidates?.optJSONObject(0)
            val content = first?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val part = parts?.optJSONObject(0)
            part?.optString("text")?.trim() ?: throw Exception("No text in response")
        }
    }

    // --- Relay / transport document parser (Few-Shot + Strict JSON) ---

    private val systemInstructionRelayParser = """
        You are a high-precision parser for Amazon Relay and similar transport documents. Your job is to turn unstructured text into valid JSON only.
        Rules: Ignore decorative symbols (e.g. 📌, ---, ❌). If a Rate field has a long decimal, round to 2 decimal places.
        CRITICAL: total_rate (rate in trip_details) must be taken ONLY from the line "Total Rate: $..." in the document. Do NOT use numbers from Trip ID, PU#, or order number (e.g. 1152 in 1152JRP4B is NOT the rate). total_miles only from "Total Loaded Miles: ... mi".
        Extract trip_id, total_rate, total_miles, and every stop (PU/DEL) with type, location code, full address, time window, and note. Split address into street, city, state, zip when possible. Deductions/penalties go into penalty_policy as separate objects with description and penalty amount (number). Output ONLY valid JSON matching the exact schema requested.
    """.trimIndent()

    private val relayFewShotExample = """
        Example 1 (Trip/ PU | DEL blocks):
        Trip T-1114MHDHR | Rate 1853.83 | 412 mi
        ---
        PU 114W5CCNG | DFW7, 700 Westport Pkwy, Fort Worth, TX 76177 | 2026-02-17 23:01 CST | Note: Empty trailer
        DEL | VENDOR-1007598780, 2340 Providence Drive, Fort Worth, TX 76106 | 2026-02-17 23:59 CST
        Deductions: Late delivery 300; No BOL pictures: No payment; Suspension fault 5000

        Example 1 output (exact schema):
        {"trip_details":{"id":"T-1114MHDHR","rate":1853.83,"miles":412},"itinerary":[{"type":"Pickup","location_code":"114W5CCNG","address":"DFW7, 700 Westport Pkwy, Fort Worth, TX 76177","street":"700 Westport Pkwy","city":"Fort Worth","state":"TX","zip":"76177","time":"2026-02-17T23:01:00","note":"Empty trailer"},{"type":"Delivery","location_code":"VENDOR-1007598780","address":"2340 Providence Drive, Fort Worth, TX 76106","street":"2340 Providence Drive","city":"Fort Worth","state":"TX","zip":"76106","time":"2026-02-17T23:59:00","note":null}],"penalty_policy":[{"description":"Late delivery","penalty":300},{"description":"No BOL pictures","penalty":0},{"description":"Suspension fault","penalty":5000}]}

        Example 2 (PU# / Pu-time / Pu-address / Del-time / Del-address / Total Rate / Total Loaded Miles — ONE trip):
        PU# 111BZL7T2
        Note: Preloaded
        Pu-time: 02/18 00:00 CST
        Pu-address: VENDOR-1007598780 / 2340 Providence Drive / Fort Worth, TX 76106
        Del-time: 02/19 08:30 EST
        Del-address: FWA4 / 9798 Smith Rd / Fort Wayne, IN 46809
        Total Rate: $1853.8299560546875
        Total Loaded Miles: 1068.47 mi
        Amazon relay app use: -$300; Late PU: -$300; No Trailer pictures on PU: -$250

        Example 2 output (exact schema, one trip; trip id from PU#, rate and miles from Total Rate / Total Loaded Miles; itinerary = one Pickup stop from Pu-address/Pu-time, one Delivery from Del-address/Del-time):
        {"trip_details":{"id":"111BZL7T2","rate":1853.83,"miles":1068.47},"itinerary":[{"type":"Pickup","location_code":"VENDOR-1007598780","address":"2340 Providence Drive, Fort Worth, TX 76106","street":"2340 Providence Drive","city":"Fort Worth","state":"TX","zip":"76106","time":"2026-02-18T00:00:00","note":"Preloaded"},{"type":"Delivery","location_code":"FWA4","address":"9798 Smith Rd, Fort Wayne, IN 46809","street":"9798 Smith Rd","city":"Fort Wayne","state":"IN","zip":"46809","time":"2026-02-19T08:30:00","note":null}],"penalty_policy":[{"description":"Amazon relay app use","penalty":300},{"description":"Late PU","penalty":300},{"description":"No Trailer pictures on PU","penalty":250}]}
    """.trimIndent()

    /**
     * Parse transport/relay document (e.g. Amazon Relay) into structured trip_details, itinerary (stops), penalty_policy.
     * Uses Few-Shot prompting + strict JSON schema. Returns null on parse/API failure.
     */
    suspend fun parseRelayDocument(text: String): RelayParseResult? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        try {
            val userPrompt = """
                $relayFewShotExample

                Now parse the following document. Return ONLY valid JSON in this exact schema (no other text):
                {"trip_details":{"id":"string","rate":number,"miles":number},"itinerary":[{"type":"Pickup|Delivery","location_code":"string","address":"string","street":"string","city":"string","state":"string","zip":"string","time":"ISO or string","note":"string|null"}],"penalty_policy":[{"description":"string","penalty":number}]}

                Document to parse:
                $text
            """.trimIndent()
            val response = generateContentWithSystemAndJson(systemInstructionRelayParser, userPrompt)
            parseRelayJson(response)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseRelayJson(jsonText: String): RelayParseResult? {
        return try {
            val cleaned = jsonText.trim().removeSurrounding("```json", "```").trim()
            val obj = JSONObject(cleaned)
            val tripDetailsObj = obj.optJSONObject("trip_details") ?: return null
            val tripDetails = TripDetails(
                id = tripDetailsObj.optString("id").trim().ifBlank { "—" },
                rate = tripDetailsObj.optDouble("rate", 0.0).coerceAtLeast(0.0),
                miles = tripDetailsObj.optDouble("miles", 0.0).coerceAtLeast(0.0)
            )
            val itineraryArr = obj.optJSONArray("itinerary") ?: return null
            val itinerary = (0 until itineraryArr.length()).map { i ->
                val s = itineraryArr.optJSONObject(i) ?: return null
                RelayStop(
                    type = s.optString("type", "Stop"),
                    locationCode = s.optString("location_code").trim().takeIf { it.isNotBlank() },
                    address = s.optString("address").trim().ifBlank { "—" },
                    street = s.optString("street").trim().takeIf { it.isNotBlank() },
                    city = s.optString("city").trim().takeIf { it.isNotBlank() },
                    state = s.optString("state").trim().takeIf { it.isNotBlank() },
                    zip = s.optString("zip").trim().takeIf { it.isNotBlank() },
                    time = s.optString("time").trim().takeIf { it.isNotBlank() },
                    note = s.optString("note").trim().takeIf { it.isNotBlank() }
                )
            }
            val penaltyArr = obj.optJSONArray("penalty_policy") ?: JSONArray()
            val penaltyPolicy = (0 until penaltyArr.length()).map { i ->
                val p = penaltyArr.optJSONObject(i) ?: return@map PenaltyRule("", 0.0)
                PenaltyRule(
                    description = p.optString("description").trim().ifBlank { "—" },
                    penalty = p.optDouble("penalty", 0.0).coerceAtLeast(0.0)
                )
            }
            RelayParseResult(tripDetails = tripDetails, itinerary = itinerary, penaltyPolicy = penaltyPolicy)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Parse a group conversation (e.g. Telegram group chat): extract ALL weekly totals and ALL trips/loads from the combined text.
     * Use when the bot is in a group and you want to pull all orders/shipments from the conversation.
     */
    suspend fun parseGroupConversation(combinedText: String): GroupParseResult? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || combinedText.isBlank()) return@withContext null
        try {
            val systemInstruction = """
                You are a parser for logistics/freight data. From a GROUP CONVERSATION (multiple messages combined), extract EVERY weekly total and EVERY trip/load mentioned.
                Rules: gross only from "Total Rate" or "Gross" (never from Trip ID like 1152JRP4B). miles from "Total Loaded Miles" or "Miles". Use 0 for missing numbers, today's date (YYYY-MM-DD) for missing date.
                Return ONLY valid JSON: {"weeklyTotals":[{"date":"YYYY-MM-DD","gross":number,"miles":number,"salaryIn":number,"diesel":number,"companyNames":[]}],"trips":[{"pointA":"","pointB":"","miles":number,"cost":number,"startTime":"","endTime":"","orderNumber":"","date":"YYYY-MM-DD"}]}
                If nothing found, return {"weeklyTotals":[],"trips":[]}. Do not duplicate the same shipment twice; if the same load appears multiple times, include it once (by date + route + cost).
            """.trimIndent()
            val userPrompt = "Extract all weekly totals and all trips/loads from this conversation:\n\n$combinedText"
            val body = JSONObject().apply {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply { put(JSONObject().apply { put("text", systemInstruction) }) })
                })
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply { put(JSONObject().apply { put("text", userPrompt) }) })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                    put("maxOutputTokens", 4096)
                    put("response_mime_type", "application/json")
                })
            }.toString()
            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()
            val responseStr = client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) return@withContext null
                val json = JSONObject(bodyStr)
                val candidates = json.optJSONArray("candidates")
                val first = candidates?.optJSONObject(0)
                val content = first?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                parts?.optJSONObject(0)?.optString("text")?.trim()
            } ?: return@withContext null
            parseGroupConversationJson(responseStr)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseGroupConversationJson(jsonText: String): GroupParseResult? {
        return try {
            val cleaned = jsonText.trim().removeSurrounding("```json", "```").trim()
            val obj = JSONObject(cleaned)
            val wtArr = obj.optJSONArray("weeklyTotals") ?: JSONArray()
            val weeklyTotals = (0 until wtArr.length()).mapNotNull { i ->
                val o = wtArr.optJSONObject(i) ?: return@mapNotNull null
                ParsedWeeklyTotal(
                    date = o.optString("date").takeIf { it.isNotBlank() } ?: java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()),
                    gross = o.optDouble("gross", 0.0).coerceAtLeast(0.0),
                    miles = o.optDouble("miles", 0.0).coerceAtLeast(0.0),
                    salaryIn = o.optDouble("salaryIn", 0.0).coerceAtLeast(0.0),
                    diesel = o.optDouble("diesel", 0.0).coerceAtLeast(0.0),
                    companyNames = mutableListOf<String>().apply {
                        o.optJSONArray("companyNames")?.let { arr ->
                            for (j in 0 until arr.length()) arr.optString(j).takeIf { it.isNotBlank() }?.let { add(it) }
                        }
                    }
                )
            }
            val tripArr = obj.optJSONArray("trips") ?: JSONArray()
            val trips = (0 until tripArr.length()).mapNotNull { i ->
                val o = tripArr.optJSONObject(i) ?: return@mapNotNull null
                val pointA = o.optString("pointA").trim().ifBlank { return@mapNotNull null }
                val pointB = o.optString("pointB").trim().ifBlank { return@mapNotNull null }
                ParsedTrip(
                    pointA = pointA,
                    pointB = pointB,
                    miles = o.optDouble("miles", 0.0).coerceAtLeast(0.0),
                    cost = o.optDouble("cost", 0.0).coerceAtLeast(0.0),
                    startTime = o.optString("startTime").trim().ifBlank { "—" },
                    endTime = o.optString("endTime").trim().ifBlank { "—" },
                    orderNumber = o.optString("orderNumber").trim().ifBlank { "—" },
                    date = o.optString("date").takeIf { it.isNotBlank() } ?: java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                )
            }
            GroupParseResult(weeklyTotals = weeklyTotals, trips = trips)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Call Gemini generateContent. Returns text on success, or throws with a clear message on failure.
     */
    private suspend fun generateContent(text: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw Exception("Gemini API key not set. Add GEMINI_API_KEY to your project's local.properties file.")
        val body = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", text) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.2)
                put("maxOutputTokens", 1024)
            })
        }.toString()
        val request = Request.Builder()
            .url("$baseUrl?key=$apiKey")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val msg = try {
                    val err = JSONObject(bodyStr)
                    err.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
                        ?: "HTTP ${response.code}"
                } catch (_: Exception) {
                    "HTTP ${response.code}"
                }
                throw Exception("Gemini API: $msg. Check your API key at https://aistudio.google.com/apikey")
            }
            val json = JSONObject(bodyStr)
            val candidates = json.optJSONArray("candidates")
            val first = candidates?.optJSONObject(0)
            val content = first?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val part = parts?.optJSONObject(0)
            val textOut = part?.optString("text")?.trim()
            if (!textOut.isNullOrBlank()) return@withContext textOut
            val blockReason = first?.optJSONObject("finishReason")?.optString("reason")
                ?: first?.optString("finishReason")
            throw Exception(
                if (blockReason != null) "Response blocked: $blockReason"
                else "Gemini returned no text. Try rephrasing your question."
            )
        }
    }
}

/** Result of parsing a group conversation: all weekly totals and trips extracted from the text. */
data class GroupParseResult(
    val weeklyTotals: List<ParsedWeeklyTotal>,
    val trips: List<ParsedTrip>
)

data class ParsedWeeklyTotal(
    val date: String,
    val gross: Double,
    val miles: Double,
    val salaryIn: Double,
    val diesel: Double,
    val companyNames: List<String> = emptyList()
)

data class ParsedTrip(
    val pointA: String,
    val pointB: String,
    val miles: Double,
    val cost: Double,
    val startTime: String,
    val endTime: String,
    val orderNumber: String,
    val date: String
)

// Regex: value after label; supports $1,152.00 or 1247.2 or 217.84 mi
private val regexTotalRate = Regex("""Total\s+Rate\s*:\s*\$?\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
private val regexGross = Regex("""Gross\s*:\s*\$?\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
private val regexMiles = Regex("""(?:Total\s+Loaded\s+)?Miles?\s*:\s*([\d,]+\.?\d*)\s*(?:mi)?|([\d,]+\.?\d*)\s*mi""", RegexOption.IGNORE_CASE)
private val regexDiesel = Regex("""Diesel\s*:\s*\$?\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
private val regexSalaryIn = Regex("""(?:Salary|Salary\s+In)\s*:\s*\$?\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
private val dateRegexSimple = Regex("(20\\d{2})[-/](0?[1-9]|1[0-2])[-/](0?[1-9]|[12]\\d|3[01])")
private fun extractDouble(r: Regex, text: String): Double? {
    val m = r.find(text) ?: return null
    val group = m.groupValues.drop(1).firstOrNull { it.isNotBlank() } ?: return null
    return group.replace(",", "").toDoubleOrNull()
}

/**
 * Simple fallback: extract gross, miles, salary, diesel and optional date from text without Gemini.
 * Prefer keyword-based extraction so Trip ID (e.g. 1152JRP4B) is never used as gross.
 * Handles: "Total Rate: $1247.2", "Total Loaded Miles: 217.84 mi", "Diesel: $100",
 * and legacy "gross 5000 salary 3000 diesel 500", "2025-02-17 5000 3000 500".
 */
fun parseWeeklyTotalSimple(message: String): ParsedWeeklyTotal? {
    val dateStr = dateRegexSimple.find(message)?.value?.replace("/", "-")?.let { d ->
        val parts = d.split("-")
        "${parts[0]}-${parts[1].padStart(2, '0')}-${parts[2].padStart(2, '0')}"
    } ?: java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

    // 1) Keyword-based: use only values explicitly tied to labels (avoids Trip ID / PU# being used as gross)
    val grossFromLabel = regexTotalRate.find(message)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
        ?: regexGross.find(message)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
    val milesFromLabel = extractDouble(regexMiles, message)
    val dieselFromLabel = extractDouble(regexDiesel, message)
    val salaryFromLabel = extractDouble(regexSalaryIn, message)

    if (grossFromLabel != null && grossFromLabel > 0) {
        return ParsedWeeklyTotal(
            date = dateStr,
            gross = grossFromLabel,
            miles = (milesFromLabel ?: 0.0).coerceAtLeast(0.0),
            salaryIn = (salaryFromLabel ?: 0.0).coerceAtLeast(0.0),
            diesel = (dieselFromLabel ?: 0.0).coerceAtLeast(0.0),
            companyNames = emptyList()
        )
    }

    // 2) Legacy: "gross 5000 salary 3000 diesel 500" or "5000 3000 500"
    val nums = Regex("\\d+(?:\\.\\d+)?").findAll(message).map { it.value.toDoubleOrNull() ?: 0.0 }.toList()
    if (nums.size < 3) return null
    val (gross, salaryIn, diesel) = Triple(nums[0], nums.getOrNull(1) ?: 0.0, nums.getOrNull(2) ?: 0.0)
    if (gross <= 0 && salaryIn <= 0 && diesel <= 0) return null
    return ParsedWeeklyTotal(
        date = dateStr,
        gross = gross,
        miles = 0.0,
        salaryIn = salaryIn,
        diesel = diesel,
        companyNames = emptyList()
    )
}
