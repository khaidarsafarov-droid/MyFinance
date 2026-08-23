package com.truckerload.di

import android.content.Context
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.crowd.CrowdRpmRepository
import com.truckerload.data.repository.social.ProfileRepository
import com.truckerload.data.repository.social.ProfileRepositoryImpl
import com.truckerload.data.social.AvatarStorage

/**
 * Account-scoped own-profile + crowd RPM wiring.
 *
 * Hilt cannot `@InstallIn(UserComponent::class)` because [UserComponent] is a hand-rolled
 * session graph; this factory is invoked from [UserComponent.create].
 */
object SocialGraphModule {

    @UserScope
    data class Bundle(
        val profile: ProfileRepository,
        val crowdRpm: CrowdRpmRepository,
    )

    fun create(
        context: Context,
        db: AppDatabase,
        loadRepository: LoadRepository,
        userProfileStore: UserProfileStore,
    ): Bundle {
        val appContext = context.applicationContext
        val avatarStorage = AvatarStorage(context)
        val profileRepository = ProfileRepositoryImpl(
            profileDao = db.driverProfileDao(),
            loadRepository = loadRepository,
            userProfileStore = userProfileStore,
            avatarStorage = avatarStorage,
            appContext = appContext,
        )
        return Bundle(
            profile = profileRepository,
            crowdRpm = CrowdRpmRepository(db),
        )
    }
}
