package com.truckerload.data.repository

import android.content.Context
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.toDomain
import com.truckerload.data.local.toEntity
import com.truckerload.domain.model.MiscExpense
import com.truckerload.utils.MiscExpenseReceiptStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MiscExpenseRepository(
    private val db: AppDatabase,
    private val appContext: Context,
) {
    private val dao = db.miscExpenseDao()

    fun observeAll(): Flow<List<MiscExpense>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }.flowOn(Dispatchers.IO)

    suspend fun getAllOnce(): List<MiscExpense> = withContext(Dispatchers.IO) {
        dao.getAll().map { it.toDomain() }
    }

    suspend fun upsert(expense: MiscExpense) = withContext(Dispatchers.IO) {
        if (expense.id > 0) {
            val previous = dao.getById(expense.id)?.receiptPhotoPath
            if (!previous.isNullOrBlank() && previous != expense.receiptPhotoPath) {
                MiscExpenseReceiptStore.deleteIfManaged(appContext, previous)
            }
        }
        dao.upsert(expense.toEntity())
    }

    suspend fun delete(id: Int) = withContext(Dispatchers.IO) {
        val previous = dao.getById(id)?.receiptPhotoPath
        dao.deleteById(id)
        MiscExpenseReceiptStore.deleteIfManaged(appContext, previous)
    }

    /** Drop a camera/gallery file that was never saved to Room. */
    suspend fun discardUnsavedReceipt(currentPath: String?, initialPath: String?) =
        withContext(Dispatchers.IO) {
            if (!currentPath.isNullOrBlank() && currentPath != initialPath) {
                MiscExpenseReceiptStore.deleteIfManaged(appContext, currentPath)
            }
        }
}
