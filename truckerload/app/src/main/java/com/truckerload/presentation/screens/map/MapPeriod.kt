package com.truckerload.presentation.screens.map

import java.util.concurrent.TimeUnit

/** Analytics window for the geographic efficiency map. */
enum class MapPeriod {
    WEEK,
    MONTH,
    YEAR,
    ;

    val windowMs: Long
        get() = when (this) {
            WEEK -> TimeUnit.DAYS.toMillis(7)
            MONTH -> TimeUnit.DAYS.toMillis(30)
            YEAR -> TimeUnit.DAYS.toMillis(365)
        }
}
