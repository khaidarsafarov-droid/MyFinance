package com.truckerload.data.repository.social

import com.truckerload.data.community.CommunityInboxSync
import com.truckerload.data.community.CommunityRemoteClient
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
    private val remote: CommunityRemoteClient,
    private val inbox: CommunityInboxSync,
) {
    private val initMutex = Mutex()

    suspend fun ensureInitialized() {
        initMutex.withLock {
            val profileDao = db.driverProfileDao()
            val userProfile = userProfileStore.profile.value
            val displayName = userProfile?.displayName.orEmpty()
            SocialDemoCleanup.purge(db)
            SocialSeedData.ensureLocalProfile(
                profileDao,
                displayName,
                userProfile?.photoUrl,
                userProfile?.phoneNumber,
                customPhoto = userProfile?.customPhoto == true,
            )
            profileRepository.syncIdentityFromUserProfile()
            profileRepository.maybeMarkSetupCompleteFromExistingProfile()
            statusRepository.purgeExpired()
            pullRemoteLocked()
        }
    }

    suspend fun pullRemote() {
        initMutex.withLock { pullRemoteLocked() }
    }

    fun isLive(): Boolean = remote.isReady()

    private suspend fun pullRemoteLocked() {
        if (!remote.isReady()) return
        inbox.pullAll()
    }
}
