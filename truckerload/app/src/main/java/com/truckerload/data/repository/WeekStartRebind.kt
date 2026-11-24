package com.truckerload.data.repository

import android.content.Context
import com.truckerload.data.local.AppDatabase
import com.truckerload.domain.week.WeekStartRebinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Opens the active user's database and asks [WeekStartRebinder] to rewrite week labels.
 * The domain object does not see Android or Room.
 */
object WeekStartRebind {
    suspend fun rebindIfPossible(context: Context) {
        val app = context.applicationContext
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getInstanceForActiveUser(app) ?: return@withContext
            WeekStartRebinder.rebind(
                loads = RoomLoadStore(LoadRepository(db)),
                diesel = DieselRepository(db),
                paychecks = PaycheckRepository(db),
            )
        }
    }
}
