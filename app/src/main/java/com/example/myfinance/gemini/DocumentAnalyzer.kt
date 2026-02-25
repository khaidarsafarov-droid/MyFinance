package com.example.myfinance.gemini

/**
 * Result of analyzing one message/document. Exactly one type per message.
 * Ensures: diesel/gross/salary only in WeeklyTotal; trip never has diesel.
 */
sealed class AnalysisResult {
    data class WeeklyTotalResult(val data: ParsedWeeklyTotal) : AnalysisResult()
    data class TripResult(val data: ParsedTrip) : AnalysisResult()
    data class RequiresClarification(val message: String) : AnalysisResult()
}
