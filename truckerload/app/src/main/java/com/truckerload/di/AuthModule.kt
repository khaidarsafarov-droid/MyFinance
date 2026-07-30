package com.truckerload.di

import android.content.Context
import com.truckerload.data.preferences.AuthCredentialsStore
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Process-scoped auth orchestration (Supabase / Google / local credentials).
 *
 * Not account-Room scoped — uses [AuthStore] / profile / credentials only.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        @ApplicationContext context: Context,
        authStore: AuthStore,
        userProfileStore: UserProfileStore,
        credentialsStore: AuthCredentialsStore,
    ): AuthRepository = AuthRepository(
        appContext = context,
        authStore = authStore,
        userProfileStore = userProfileStore,
        credentialsStore = credentialsStore,
    )
}
