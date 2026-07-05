package com.truckerload.domain.model

/** Штат с агрегированным доходом за период. */
data class StateRevenue(
    val state: String,
    val revenue: Double,
    val trips: Int,
    val shareOfTotal: Float
)
