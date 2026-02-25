package com.example.myfinance.data

data class AppData(
    val companies: List<Company> = emptyList(),
    val weeklyTotals: List<WeeklyTotal> = emptyList(),
    val trips: List<Trip> = emptyList(),
    val companyChanges: List<CompanyChange> = emptyList(),
    val goal: Goal? = null
)
