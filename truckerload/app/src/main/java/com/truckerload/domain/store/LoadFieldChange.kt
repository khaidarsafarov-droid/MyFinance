package com.truckerload.domain.store

/** One field change recorded when a saved load is updated. Persistence maps this to history rows. */
data class LoadFieldChange(
    val loadId: String,
    val field: String,
    val oldValue: String,
    val newValue: String,
    val timestamp: Long,
)
