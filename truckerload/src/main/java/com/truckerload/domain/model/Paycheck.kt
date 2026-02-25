package com.truckerload.domain.model

data class Paycheck(
    val id: Int,
    val weekNumber: Int,
    val year: Int,
    val weekLabel: String,
    val weekStartDate: String,
    val weekEndDate: String,
    val driverName: String?,
    val grossAmount: Double?,
    val netAmount: Double,
    val rawExtractedText: String,
    val sourceFileName: String?,
    val addedAt: Long
)
