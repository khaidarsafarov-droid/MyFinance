package com.truckerload.di

import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.LastUsedDefaultsStore
import com.truckerload.data.preferences.RpmThresholdsStore
import com.truckerload.data.preferences.SelectedStateStore
import com.truckerload.data.preferences.WeeklyProfitGoalStore
import com.truckerload.data.repository.AiRepository
import com.truckerload.data.repository.AnalyticsRepository
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.crowd.CrowdRpmRepository
import com.truckerload.data.repository.MaintenanceRepository
import com.truckerload.data.repository.MiscExpenseRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.data.repository.PerDiemOverrideRepository
import com.truckerload.data.repository.PhotoRepository
import com.truckerload.data.repository.ScanRepository
import com.truckerload.data.repository.WeekRepository
import com.truckerload.data.repository.account.AccountDeletionService
import com.truckerload.data.repository.account.DriverProfessionalRepository
import com.truckerload.data.repository.account.RegistrationService
import com.truckerload.data.repository.social.ProfileRepository
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
    fun provideWeeklyProfitGoalStore(manager: UserComponentManager): WeeklyProfitGoalStore =
        manager.require().weeklyProfitGoalStore

    @Provides
    fun provideLastUsedDefaultsStore(manager: UserComponentManager): LastUsedDefaultsStore =
        manager.require().lastUsedDefaultsStore

    @Provides
    fun provideAnalyticsRepository(manager: UserComponentManager): AnalyticsRepository =
        manager.require().analyticsRepository

    @Provides
    fun providePhotoRepository(manager: UserComponentManager): PhotoRepository =
        manager.require().photoRepository

    @Provides
    fun provideScanRepository(manager: UserComponentManager): ScanRepository =
        manager.require().scanRepository

    @Provides
    fun provideProfileRepository(manager: UserComponentManager): ProfileRepository =
        manager.require().profileRepository

    @Provides
    fun provideCrowdRpmRepository(manager: UserComponentManager): CrowdRpmRepository =
        manager.require().crowdRpmRepository

    @Provides
    fun provideAiRepository(manager: UserComponentManager): AiRepository =
        manager.require().aiRepository

    @Provides
    fun provideMaintenanceRepository(manager: UserComponentManager): MaintenanceRepository =
        manager.require().maintenanceRepository

    @Provides
    fun providePerDiemOverrideRepository(manager: UserComponentManager): PerDiemOverrideRepository =
        manager.require().perDiemOverrideRepository

    @Provides
    fun provideMiscExpenseRepository(manager: UserComponentManager): MiscExpenseRepository =
        manager.require().miscExpenseRepository

    @Provides
    fun provideRegistrationService(manager: UserComponentManager): RegistrationService =
        manager.require().registrationService

    @Provides
    fun provideDriverProfessionalRepository(manager: UserComponentManager): DriverProfessionalRepository =
        manager.require().driverProfessionalRepository

    @Provides
    fun provideAccountDeletionService(manager: UserComponentManager): AccountDeletionService =
        manager.require().accountDeletionService
}
