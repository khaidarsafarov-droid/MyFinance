package com.truckerload.domain.assistant

sealed class AssistantToolCall {
    data class AddDiesel(
        val amount: Double,
        val gallons: Double?,
        val date: String?,
    ) : AssistantToolCall()

    data class AddPaycheck(
        val amount: Double,
        val weekNumber: Int?,
        val year: Int?,
    ) : AssistantToolCall()

    data class QueryWeeklyGross(
        val weekNumber: Int?,
        val year: Int?,
    ) : AssistantToolCall()
}
