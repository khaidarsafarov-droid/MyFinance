package com.truckerload.domain.model

import com.truckerload.domain.money.Money

data class Penalty(
    val id: Int,
    val loadId: String,
    val description: String,
    val amount: Money,
)
