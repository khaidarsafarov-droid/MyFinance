package com.truckerload.data.repository.social

import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.di.UserScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@UserScope
class SocialSyncCoordinator(
    private val db: AppDatabase,
    private val userProfileStore: UserProfileStore,
    private val profileRepository: ProfileRepository,
    private val statusRepository: StatusRepository,
    private val seedHelper: SocialSeedHelper,
) {
    private val initMutex = Mutex()

    suspend fun ensureInitialized() {
        initMutex.withLock {
            val chatDao = db.socialChatDao()
            val messageDao = db.socialMessageDao()
            val profileDao = db.driverProfileDao()
            val userProfile = userProfileStore.profile.value
            val displayName = userProfile?.displayName.orEmpty()
            com.truckerload.data.social.SocialSeedData.seedIfEmpty(
                chatDao,
                messageDao,
                profileDao,
                displayName,
                userProfile?.photoUrl,
                userProfile?.phoneNumber,
            )
            profileRepository.syncIdentityFromUserProfile()
            profileRepository.maybeMarkSetupCompleteFromExistingProfile()
            com.truckerload.data.social.SocialPeerSeedData.seedIfEmpty(db.socialPeerDao())
            seedHelper.seedDemoStatuses(displayName, SocialConstants.STATUS_TTL_MS)
            seedHelper.seedGroupMemberships(displayName)
            seedHelper.backfillGroupInviteCodes()
            statusRepository.purgeExpired()
            profileRepository.refreshMyChallengeScore()
        }
    }
}
