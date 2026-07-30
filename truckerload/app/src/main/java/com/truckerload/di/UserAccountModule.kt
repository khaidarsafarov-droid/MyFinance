package com.truckerload.di

import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.RpmThresholdsStore
import com.truckerload.data.preferences.SelectedStateStore
import com.truckerload.data.preferences.StatsSelectionStore
import com.truckerload.data.preferences.WeeklyProfitGoalStore
import com.truckerload.data.repository.AiRepository
import com.truckerload.data.repository.AnalyticsRepository
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.MaintenanceRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.data.repository.PhotoRepository
import com.truckerload.data.repository.ScanRepository
import com.truckerload.data.repository.VoiceRepository
import com.truckerload.data.repository.WeekRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Bridges the active [UserComponent] into SingletonComponent so `@HiltViewModel`
 * and `@HiltWorker` can inject account-scoped types.
 *
 * Bindings are **unscoped**: each injection resolves the current session from
 * [UserComponentManager] (never cache a previous user's repository as `@Singleton`).
 */
@Module
@InstallIn(SingletonComponent::class)
object UserAccountModule {

    @Provides
    @UserId
    fun provideUserId(manager: UserComponentManager): String = manager.require().userId

    @Provides
    fun provideAppDatabase(manager: UserComponentManager): AppDatabase =
        manager.require().database

    @Provides
    fun provideLoadRepository(manager: UserComponentManager): LoadRepository =
        manager.require().loadRepository

    @Provides
    fun providePaycheckRepository(manager: UserComponentManager): PaycheckRepository =
        manager.require().paycheckRepository

    @Provides
    fun provideDieselRepository(manager: UserComponentManager): DieselRepository =
        manager.require().dieselRepository

    @Provides
    fun provideWeekRepository(manager: UserComponentManager): WeekRepository =
        manager.require().weekRepository

    @Provides
    fun provideRpmThresholdsStore(manager: UserComponentManager): RpmThresholdsStore =
        manager.require().rpmThresholdsStore

    @Provides
    fun provideSelectedStateStore(manager: UserComponentManager): SelectedStateStore =
        manager.require().selectedStateStore

    @Provides
    fun provideStatsSelectionStore(manager: UserComponentManager): StatsSelectionStore =
        manager.require().statsSelectionStore

    @Provides
    fun provideWeeklyProfitGoalStore(manager: UserComponentManager): WeeklyProfitGoalStore =
        manager.require().weeklyProfitGoalStore

    @Provides
    fun provideAnalyticsRepository(manager: UserComponentManager): AnalyticsRepository =
        manager.require().analyticsRepository

    @Provides
    fun providePhotoRepository(manager: UserComponentManager): PhotoRepository =
        manager.require().photoRepository

    @Provides
    fun provideScanRepository(manager: UserComponentManager): ScanRepository =
        manager.require().scanRepository

    // Social repositories are provided by [SocialRepositoryModule].

    @Provides
    fun provideVoiceRepository(manager: UserComponentManager): VoiceRepository =
        manager.require().voiceRepository

    @Provides
    fun provideAiRepository(manager: UserComponentManager): AiRepository =
        manager.require().aiRepository

    @Provides
    fun provideMaintenanceRepository(manager: UserComponentManager): MaintenanceRepository =
        manager.require().maintenanceRepository
}
