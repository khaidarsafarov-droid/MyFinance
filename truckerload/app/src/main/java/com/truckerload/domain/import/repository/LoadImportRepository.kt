package com.truckerload.domain.import.repository

import com.truckerload.domain.model.Load

interface LoadImportRepository {
    suspend fun exists(tripId: String): Boolean
    suspend fun insertLoad(load: Load, playFeedback: Boolean = true)
}
