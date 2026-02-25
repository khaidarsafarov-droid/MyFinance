package com.example.myfinance.data

/**
 * One row per week: gross, miles, salary in, diesel, net profit (salary - diesel), and which companies.
 */
data class WeeklyTotal(
    val id: String,
    val date: String, // ISO or YYYY-MM-DD week start
    val gross: Double,
    val miles: Double,
    val salaryIn: Double,
    val diesel: Double,
    val companyIds: List<String> = emptyList()
) {
    val netProfit: Double get() = salaryIn - diesel
}
