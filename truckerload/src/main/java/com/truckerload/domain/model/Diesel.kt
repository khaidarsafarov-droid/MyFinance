package com.truckerload.domain.model

data class Diesel(
    val id: Int,
    val weekNumber: Int,
    val year: Int,
    val weekLabel: String,
    val weekStartDate: String,
    val weekEndDate: String,
    val totalAmount: Double,
    val gallons: Double?,
    val pricePerGallon: Double?,
    val location: String?,
    val rawExtractedText: String,
    val sourceFileName: String?,
    val addedAt: Long
)
