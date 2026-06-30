package com.truckerload.domain.model

data class Penalty(
    val id: Int,
    val loadId: String,
    val description: String,
    val amount: Double
)
