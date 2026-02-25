package com.example.myfinance.ui

object Nav {
    const val SUMMARY = "summary"
    const val LOADS = "loads"
    const val ADD_WEEKLY_TOTAL = "add_weekly_total"
    const val ADD_WEEKLY_TOTAL_FOR_COMPANY = "add_weekly_total/{companyId}"
    const val EDIT_WEEKLY_TOTAL = "edit_weekly_total/{weeklyTotalId}"
    const val EDIT_TRIP = "edit_trip/{tripId}"
    const val ADD_TRIP = "add_trip"
    const val COMPANY = "company/{companyId}"
    const val ANALYTICS = "analytics"
    const val AI_CHAT = "ai_chat"
    const val SETTINGS = "settings"

    fun company(companyId: String) = "company/$companyId"
    fun editWeeklyTotal(id: String) = "edit_weekly_total/$id"
    fun editTrip(id: String) = "edit_trip/$id"
}
