package com.truckerload.domain.model.analytics

data class RouteData(
    val origin: String,
    val destination: String,
    val route: String,
    val gross: Double,
    val miles: Double,
    val loadCount: Int,
    val rpm: Double,
)
