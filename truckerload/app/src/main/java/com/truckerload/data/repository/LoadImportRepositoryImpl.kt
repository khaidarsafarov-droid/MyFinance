package com.truckerload.data.repository

import com.truckerload.data.local.dao.LoadDao
import com.truckerload.domain.import.repository.LoadImportRepository
import com.truckerload.domain.model.Load

class LoadImportRepositoryImpl(
    private val loadRepository: LoadRepository,
    private val loadDao: LoadDao,
) : LoadImportRepository {

    override suspend fun exists(tripId: String): Boolean =
        loadDao.getExistingTripIds(listOf(tripId)).isNotEmpty()

    override suspend fun insertLoad(load: Load, playFeedback: Boolean) {
        loadRepository.insertLoad(load, playFeedback = playFeedback)
    }
}
