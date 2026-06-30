package com.truckerload.domain.model

data class Stop(
    val id: Int,
    val loadId: String,
    val stopNumber: Int,
    val type: StopType,
    val puNumber: String?,
    val note: String?,
    val scheduledTime: String,
    val timezone: String,
    val facilityCode: String?,
    val fullAddress: String,
    val city: String,
    val state: String,
    val zip: String
)

enum class StopType { PU, DEL }
