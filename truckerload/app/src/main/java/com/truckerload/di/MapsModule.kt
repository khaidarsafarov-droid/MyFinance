package com.truckerload.di

import com.truckerload.data.remote.GoogleDirectionsService
import com.truckerload.domain.friends.DirectionsProvider
import com.truckerload.domain.friends.RoadRouteResolver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MapsModule {

    @Provides
    @Singleton
    fun provideGoogleDirectionsService(): GoogleDirectionsService = GoogleDirectionsService()

    @Provides
    @Singleton
    fun provideDirectionsProvider(
        service: GoogleDirectionsService,
    ): DirectionsProvider = service

    @Provides
    @Singleton
    fun provideRoadRouteResolver(
        directionsService: DirectionsProvider,
    ): RoadRouteResolver = RoadRouteResolver(directionsService)
}
