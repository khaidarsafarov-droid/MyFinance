package com.truckerload.data.repository

import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.toDomain
import com.truckerload.data.local.toEntity
import com.truckerload.domain.model.MiscExpense
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MiscExpenseRepository(
    private val db: AppDatabase,
) {
    private val dao = db.miscExpenseDao()

    fun observeAll(): Flow<List<MiscExpense>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }.flowOn(Dispatchers.IO)

    suspend fun getAllOnce(): List<MiscExpense> = withContext(Dispatchers.IO) {
        dao.getAll().map { it.toDomain() }
    }

    suspend fun upsert(expense: MiscExpense) = withContext(Dispatchers.IO) {
        dao.upsert(expense.toEntity())
    }

    suspend fun delete(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }
}
