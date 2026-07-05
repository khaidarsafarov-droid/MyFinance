package com.truckerload.data.backup

import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Paycheck

/** Структура резервной копии БД для JSON экспорта/импорта. */
data class BackupData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val loads: List<Load> = emptyList(),
    val paychecks: List<Paycheck> = emptyList(),
    val diesel: List<Diesel> = emptyList()
)
