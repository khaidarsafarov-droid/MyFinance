package com.truckerload.di

import com.truckerload.data.remote.ktor.HttpClientProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

/**
 * Exposes the shared Ktor [HttpClient] for injection.
 * APIs ([com.truckerload.data.remote.ktor.KtorLoadApi], etc.) and
 * [com.truckerload.data.sync.cloud.CloudSyncEngine] are constructor-injected.
 */
@Module
@InstallIn(SingletonComponent::class)
object KtorCloudModule {
    @Provides
    @Singleton
    fun provideHttpClient(provider: HttpClientProvider): HttpClient = provider.client
}
