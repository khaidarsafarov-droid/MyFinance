package com.truckerload.di

import android.content.Context
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.data.repository.WeekRepository

/**
 * Account-scoped database/repository factory kept outside the application Hilt graph.
 *
 * The active user can change without the process restarting. Making these objects
 * `SingletonComponent` bindings would retain the previous user's Room database and
 * repositories after account switches, so callers continue to resolve them per user.
 */
class ActiveDatabaseProvider(
    private val context: Context,
    private val authStore: AuthStore = AuthStore(context),
) {
    fun getOrNull(): AppDatabase? {
        val userId = authStore.currentUserIdOrNull() ?: return null
        return AppDatabase.getInstance(context, userId)
    }

    fun loadRepository(): LoadRepository? = getOrNull()?.let { LoadRepository(it) }
    fun paycheckRepository(): PaycheckRepository? = getOrNull()?.let { PaycheckRepository(it) }
    fun dieselRepository(): DieselRepository? = getOrNull()?.let { DieselRepository(it) }
    fun weekRepository(): WeekRepository? {
        val db = getOrNull() ?: return null
        return WeekRepository(LoadRepository(db), PaycheckRepository(db), DieselRepository(db))
    }
}
