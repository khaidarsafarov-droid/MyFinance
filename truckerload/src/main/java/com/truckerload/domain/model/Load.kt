package com.truckerload.domain.model

data class Load(
    val id: String,
    val tripId: String,
    val date: String,
    val totalRate: Double,
    val totalMiles: Double,
    val pointA: String,
    val pointB: String,
    val puCount: Int,
    val delCount: Int,
    val weekNumber: Int,
    val year: Int,
    val rawMessage: String,
    val parsedAt: Long,
    val updatedAt: Long,
    val stops: List<Stop> = emptyList(),
    val penalties: List<Penalty> = emptyList()
)
