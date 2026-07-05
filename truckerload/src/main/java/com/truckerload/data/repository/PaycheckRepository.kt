package com.truckerload.data.repository

import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.toDomain
import com.truckerload.data.local.toEntity
import com.truckerload.domain.model.Paycheck
import com.truckerload.utils.getWeekRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PaycheckRepository(private val db: AppDatabase) {

    private val dao = db.paycheckDao()

    fun getAllPaychecks(): Flow<List<Paycheck>> =
        dao.getAllPaychecks().map { list -> list.map { it.toDomain() } }

    suspend fun getPaycheckForWeek(weekNumber: Int, year: Int): Paycheck? =
        dao.getPaycheckForWeek(weekNumber, year)?.toDomain()

    fun getPaychecksForWeek(weekNumber: Int, year: Int): Flow<List<Paycheck>> =
        dao.getPaychecksForWeek(weekNumber, year).map { list -> list.map { it.toDomain() } }

    suspend fun insertPaycheck(paycheck: Paycheck) {
        dao.insert(paycheck.toEntity())
    }

    suspend fun deletePaycheck(id: Int) {
        dao.deleteById(id)
    }

    suspend fun getPaychecksForYear(year: Int): List<Paycheck> =
        getAllPaychecks().first().filter { it.year == year }

    suspend fun getAllPaychecksOnce(): List<Paycheck> = getAllPaychecks().first()
}
