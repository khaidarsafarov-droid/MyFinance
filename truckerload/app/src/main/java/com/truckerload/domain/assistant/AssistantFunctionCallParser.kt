package com.truckerload.domain.assistant

import org.json.JSONObject

/**
 * Maps a Gemini generateContent JSON body to exactly one [AssistantToolCall],
 * or null when the model did not confidently pick a known function.
 */
object AssistantFunctionCallParser {
    fun parseGenerateContentBody(body: String): AssistantToolCall? {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val candidates = root.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null
        val parts = candidates.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?: return null
        val calls = mutableListOf<JSONObject>()
        for (i in 0 until parts.length()) {
            val part = parts.optJSONObject(i) ?: continue
            val call = part.optJSONObject("functionCall") ?: continue
            calls.add(call)
        }
        if (calls.size != 1) return null
        return parseFunctionCall(calls[0])
    }

    fun parseFunctionCall(call: JSONObject): AssistantToolCall? {
        val name = call.optString("name").trim()
        val argsRaw = call.opt("args")
        val args = when (argsRaw) {
            is JSONObject -> argsRaw
            is String -> runCatching { JSONObject(argsRaw) }.getOrNull() ?: JSONObject()
            else -> JSONObject()
        }
        return when (name) {
            AssistantToolNames.ADD_DIESEL -> {
                val amount = positiveNumber(args, "amount") ?: return null
                AssistantToolCall.AddDiesel(
                    amount = amount,
                    gallons = optionalPositiveNumber(args, "gallons"),
                    date = optionalNonBlank(args, "date"),
                )
            }
            AssistantToolNames.ADD_PAYCHECK -> {
                val amount = positiveNumber(args, "amount") ?: return null
                AssistantToolCall.AddPaycheck(
                    amount = amount,
                    weekNumber = optionalInt(args, "weekNumber"),
                    year = optionalInt(args, "year"),
                )
            }
            AssistantToolNames.QUERY_WEEKLY_GROSS -> AssistantToolCall.QueryWeeklyGross(
                weekNumber = optionalInt(args, "weekNumber"),
                year = optionalInt(args, "year"),
            )
            else -> null
        }
    }

    private fun optionalNonBlank(args: JSONObject, key: String): String? {
        if (!args.has(key) || args.isNull(key)) return null
        return args.optString(key).trim().takeIf { it.isNotEmpty() }
    }

    private fun optionalInt(args: JSONObject, key: String): Int? {
        if (!args.has(key) || args.isNull(key)) return null
        val value = args.optInt(key, Int.MIN_VALUE)
        return value.takeIf { it != Int.MIN_VALUE }
    }

    private fun optionalPositiveNumber(args: JSONObject, key: String): Double? {
        if (!args.has(key) || args.isNull(key)) return null
        return positiveNumber(args, key)
    }

    private fun positiveNumber(args: JSONObject, key: String): Double? {
        if (!args.has(key) || args.isNull(key)) return null
        val value = args.optDouble(key, Double.NaN)
        if (value.isNaN() || value.isInfinite() || value <= 0.0) return null
        return value
    }
}
