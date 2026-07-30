package com.truckerload.di

import com.truckerload.data.repository.SocialRepository
import com.truckerload.data.repository.social.ChatRepository
import com.truckerload.data.repository.social.GroupRepository
import com.truckerload.data.repository.social.MediaRepository
import com.truckerload.data.repository.social.ProfileRepository
import com.truckerload.data.repository.social.SocialSyncCoordinator
import com.truckerload.data.repository.social.StatusRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Bridges account-scoped social repositories into SingletonComponent.
 *
 * [UserComponent] is not a Hilt `@DefineComponent` (ViewModelComponent cannot parent
 * a custom user graph), so these bindings resolve the active session via
 * [UserComponentManager] — the same pattern as [UserAccountModule].
 *
 * Bindings are **unscoped**: never cache a previous user's repository as `@Singleton`.
 * [SocialSyncCoordinator] is account-scoped for the same reason (Room DAOs).
 */
@Module
@InstallIn(SingletonComponent::class)
object SocialRepositoryModule {

    @Provides
    fun provideProfileRepository(manager: UserComponentManager): ProfileRepository =
        manager.require().profileRepository

    @Provides
    fun provideChatRepository(manager: UserComponentManager): ChatRepository =
        manager.require().chatRepository

    @Provides
    fun provideGroupRepository(manager: UserComponentManager): GroupRepository =
        manager.require().groupRepository

    @Provides
    fun provideStatusRepository(manager: UserComponentManager): StatusRepository =
        manager.require().statusRepository

    @Provides
    fun provideMediaRepository(manager: UserComponentManager): MediaRepository =
        manager.require().mediaRepository

    @Provides
    fun provideSocialSyncCoordinator(manager: UserComponentManager): SocialSyncCoordinator =
        manager.require().socialSyncCoordinator

    @Suppress("DEPRECATION")
    @Provides
    fun provideSocialRepository(manager: UserComponentManager): SocialRepository =
        manager.require().socialRepository
}
