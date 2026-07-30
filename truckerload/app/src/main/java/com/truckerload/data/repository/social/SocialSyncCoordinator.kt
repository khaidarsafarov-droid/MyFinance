package com.truckerload.data.repository.social

import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.social.SocialPeerSeedData
import com.truckerload.data.social.SocialSeedData
import com.truckerload.di.UserScope
import javax.inject.Inject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Cross-repository social bootstrap (seed + identity sync + challenge score).
 *
 * Account-scoped via [UserComponent] (needs Room). Bridged into SingletonComponent
 * through [com.truckerload.di.SocialRepositoryModule] like other user repos.
 */
@UserScope
class SocialSyncCoordinator @Inject constructor(
    db: AppDatabase,
    private val userProfileStore: UserProfileStore,
    private val profileRepository: ProfileRepository,
    private val statusRepository: StatusRepository,
) {
    private val profileDao = db.driverProfileDao()
    private val chatDao = db.socialChatDao()
    private val messageDao = db.socialMessageDao()
    private val chatMemberDao = db.chatMemberDao()
    private val driverStatusDao = db.driverStatusDao()
    private val peerDao = db.socialPeerDao()
    private val seedHelper = SocialSeedHelper(
        chatDao = chatDao,
        chatMemberDao = chatMemberDao,
        driverStatusDao = driverStatusDao,
    )
    private val initMutex = Mutex()

    suspend fun ensureInitialized() {
        initMutex.withLock {
            val userProfile = userProfileStore.profile.value
            val displayName = userProfile?.displayName.orEmpty()
            SocialSeedData.seedIfEmpty(
                chatDao,
                messageDao,
                profileDao,
                displayName,
                userProfile?.photoUrl,
                userProfile?.phoneNumber,
            )
            profileRepository.syncIdentityFromUserProfile()
            profileRepository.maybeMarkSetupCompleteFromExistingProfile()
            SocialPeerSeedData.seedIfEmpty(peerDao)
            seedHelper.seedDemoStatuses(displayName, StatusRepositoryImpl.STATUS_TTL_MS)
            seedHelper.seedGroupMemberships(displayName)
            seedHelper.backfillGroupInviteCodes()
            statusRepository.purgeExpired()
            profileRepository.refreshMyChallengeScore()
        }
    }
}
