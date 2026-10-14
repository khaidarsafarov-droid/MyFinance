package com.truckerload.domain.model

import com.truckerload.domain.money.Money

data class Paycheck(
    val id: Int,
    val weekNumber: Int,
    val year: Int,
    val weekLabel: String,
    val weekStartDate: String,
    val weekEndDate: String,
    val driverName: String?,
    val grossAmount: Money?,
    val netAmount: Money,
    val rawExtractedText: String,
    val sourceFileName: String?,
    val addedAt: Long,
    /** App-private relative path such as `paychecks/uuid.pdf`, if the original was copied. */
    val sourceFilePath: String? = null,
)
