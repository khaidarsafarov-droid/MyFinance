package com.truckerload.data.repository

import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.toDomain
import com.truckerload.data.local.toEntity
import com.truckerload.domain.model.Diesel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DieselRepository(private val db: AppDatabase) {

    private val dao = db.dieselDao()

    fun getAllDiesel(): Flow<List<Diesel>> =
        dao.getAllDiesel().map { list -> list.map { it.toDomain() } }

    fun getDieselForWeek(weekNumber: Int, year: Int): Flow<List<Diesel>> =
        dao.getDieselForWeek(weekNumber, year).map { list -> list.map { it.toDomain() } }

    suspend fun insertDiesel(diesel: Diesel) {
        dao.insert(diesel.toEntity())
    }

    suspend fun deleteDiesel(id: Int) {
        dao.deleteById(id)
    }
}
