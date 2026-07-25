package com.truckerload.di

import android.content.Context
import com.truckerload.data.preferences.AuthCredentialsStore
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.PushTokenStore
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.preferences.UserProfileStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Process-scoped wrappers whose storage is already owned by the application context.
 *
 * Account databases and repositories deliberately do not belong here: their lifetime
 * follows the active user and they must be rebuilt when that user changes.
 */
@Module
@InstallIn(SingletonComponent::class)
object ApplicationStoreModule {

    @Provides
    @Singleton
    fun provideAuthStore(@ApplicationContext context: Context): AuthStore = AuthStore(context)

    @Provides
    @Singleton
    fun provideAuthCredentialsStore(
        @ApplicationContext context: Context,
    ): AuthCredentialsStore = AuthCredentialsStore(context)

    @Provides
    @Singleton
    fun provideUserProfileStore(
        @ApplicationContext context: Context,
    ): UserProfileStore = UserProfileStore(context)

    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context,
    ): SettingsDataStore = SettingsDataStore(context)

    @Provides
    @Singleton
    fun providePushTokenStore(
        @ApplicationContext context: Context,
    ): PushTokenStore = PushTokenStore(context)
}
