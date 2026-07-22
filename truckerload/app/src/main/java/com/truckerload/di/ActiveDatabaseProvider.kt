package com.truckerload.di

import android.content.Context
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.data.repository.WeekRepository

/**
 * Lightweight DI foundation (Hilt-ready).
 *
 * Full `com.google.dagger.hilt.android` is blocked on AGP 9.3 until Dagger ships
 * BaseExtension-compatible plugin support. This provider is the migration seam:
 * swap to `@Module` / `@Inject` when upgrading Hilt.
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
