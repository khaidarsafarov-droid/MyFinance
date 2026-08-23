package com.truckerload.domain.assistant

import org.json.JSONArray
import org.json.JSONObject

/**
 * Gemini function-calling tool schema. Keep this as the single source of truth
 * for names and parameters the model is allowed to invoke.
 */
object AssistantFunctionDeclarations {
    fun toolsArray(): JSONArray = JSONArray().put(
        JSONObject().put("functionDeclarations", functionDeclarations()),
    )

    fun functionDeclarations(): JSONArray = JSONArray()
        .put(addDiesel())
        .put(addPaycheck())
        .put(queryWeeklyGross())

    private fun addDiesel(): JSONObject = JSONObject()
        .put("name", AssistantToolNames.ADD_DIESEL)
        .put(
            "description",
            "Add a diesel / fuel purchase. Use when the driver spent money on diesel or fuel. " +
                "Do not call this for paycheck or load questions.",
        )
        .put(
            "parameters",
            JSONObject()
                .put("type", "OBJECT")
                .put(
                    "properties",
                    JSONObject()
                        .put("amount", number("Total dollars paid for diesel. Required."))
                        .put("gallons", number("Gallons of fuel if spoken. Omit if unknown."))
                        .put("date", string("Purchase date as YYYY-MM-DD. Omit to use today.")),
                )
                .put("required", JSONArray().put("amount")),
        )

    private fun addPaycheck(): JSONObject = JSONObject()
        .put("name", AssistantToolNames.ADD_PAYCHECK)
        .put(
            "description",
            "Add a paycheck / settlement net amount. Use when the driver received pay. " +
                "Trucking week is Sunday–Saturday. Omit week to use the current week.",
        )
        .put(
            "parameters",
            JSONObject()
                .put("type", "OBJECT")
                .put(
                    "properties",
                    JSONObject()
                        .put("amount", number("Net paycheck amount in dollars. Required."))
                        .put("weekNumber", integer("Trucking week number 1–53 if spoken."))
                        .put("year", integer("Week-year (ISO-like trucking week-year) if spoken.")),
                )
                .put("required", JSONArray().put("amount")),
        )

    private fun queryWeeklyGross(): JSONObject = JSONObject()
        .put("name", AssistantToolNames.QUERY_WEEKLY_GROSS)
        .put(
            "description",
            "Read-only: return this driver's weekly gross (sum of load totalRate), miles, " +
                "load count, diesel, and paycheck from local data. Use for questions like " +
                "\"what was my gross this week\". Omit week to use the current trucking week.",
        )
        .put(
            "parameters",
            JSONObject()
                .put("type", "OBJECT")
                .put(
                    "properties",
                    JSONObject()
                        .put("weekNumber", integer("Trucking week number 1–53 if spoken."))
                        .put("year", integer("Week-year if spoken.")),
                ),
        )

    private fun number(description: String): JSONObject =
        JSONObject().put("type", "NUMBER").put("description", description)

    private fun integer(description: String): JSONObject =
        JSONObject().put("type", "INTEGER").put("description", description)

    private fun string(description: String): JSONObject =
        JSONObject().put("type", "STRING").put("description", description)
}
