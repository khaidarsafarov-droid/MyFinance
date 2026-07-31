package com.truckerload.data.repository.social

import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.social.SocialDemoCleanup
import com.truckerload.data.social.SocialSeedData
import com.truckerload.di.UserScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@UserScope
class SocialSyncCoordinator(
    private val db: AppDatabase,
    private val userProfileStore: UserProfileStore,
    private val profileRepository: ProfileRepository,
    private val statusRepository: StatusRepository,
) {
    private val initMutex = Mutex()

    suspend fun ensureInitialized() {
        initMutex.withLock {
            val profileDao = db.driverProfileDao()
            val userProfile = userProfileStore.profile.value
            val displayName = userProfile?.displayName.orEmpty()
            // Strip prototype chats/peers/voice rooms left by older builds.
            SocialDemoCleanup.purge(db)
            SocialSeedData.ensureLocalProfile(
                profileDao,
                displayName,
                userProfile?.photoUrl,
                userProfile?.phoneNumber,
            )
            profileRepository.syncIdentityFromUserProfile()
            profileRepository.maybeMarkSetupCompleteFromExistingProfile()
            statusRepository.purgeExpired()
            profileRepository.refreshMyChallengeScore()
        }
    }
}
