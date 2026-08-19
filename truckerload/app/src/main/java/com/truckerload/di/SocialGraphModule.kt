package com.truckerload.di

import android.content.Context
import com.truckerload.data.community.CommunityInboxSync
import com.truckerload.data.community.CommunityMessageNotifier
import com.truckerload.data.community.CommunityRemoteClient
import com.truckerload.data.community.CommunityStorageClient
import com.truckerload.data.community.FriendSafetyClient
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.crowd.CrowdRpmRepository
import com.truckerload.data.repository.social.ChatRepository
import com.truckerload.data.repository.social.ChatRepositoryImpl
import com.truckerload.data.repository.social.GroupRepository
import com.truckerload.data.repository.social.GroupRepositoryImpl
import com.truckerload.data.repository.social.MediaRepository
import com.truckerload.data.repository.social.MediaRepositoryImpl
import com.truckerload.data.repository.social.ProfileRepository
import com.truckerload.data.repository.social.ProfileRepositoryImpl
import com.truckerload.data.repository.social.SocialChatStore
import com.truckerload.data.repository.social.SocialSyncCoordinator
import com.truckerload.data.repository.social.StatusRepository
import com.truckerload.data.repository.social.StatusRepositoryImpl
import com.truckerload.data.social.AvatarStorage
import com.truckerload.data.social.ChatAttachmentStorage
import com.truckerload.data.social.RecommendationService
import com.truckerload.domain.social.SocialIdentity
import java.io.File

/**
 * Account-scoped social graph wiring.
 *
 * Hilt cannot `@InstallIn(UserComponent::class)` because [UserComponent] is a hand-rolled
 * session graph; this factory is invoked from [UserComponent.create].
 */
object SocialGraphModule {

    @UserScope
    data class Bundle(
        val profile: ProfileRepository,
        val chat: ChatRepository,
        val group: GroupRepository,
        val status: StatusRepository,
        val media: MediaRepository,
        val syncCoordinator: SocialSyncCoordinator,
        val remote: CommunityRemoteClient,
        val voiceRemote: com.truckerload.data.community.CommunityVoiceRemote,
        val crowdRpm: CrowdRpmRepository,
        val actorId: () -> String,
    )

    fun create(
        context: Context,
        db: AppDatabase,
        loadRepository: LoadRepository,
        userProfileStore: UserProfileStore,
    ): Bundle {
        val appContext = context.applicationContext
        val authStore = AuthStore(appContext)
        val actorId = { SocialIdentity.actorId(authStore.currentUserIdOrNull()) }
        val remote = CommunityRemoteClient(authStore)
        val safety = FriendSafetyClient(authStore)
        val storage = CommunityStorageClient(authStore)
        val voiceRemote = com.truckerload.data.community.CommunityVoiceRemote(authStore)
        val inbox = CommunityInboxSync(
            db = db,
            remote = remote,
            storage = storage,
            cacheDir = File(appContext.filesDir, "community_cache").apply { mkdirs() },
            notifier = CommunityMessageNotifier(appContext),
            safety = safety,
        )
        val chatDao = db.socialChatDao()
        val messageDao = db.socialMessageDao()
        val blockedUserDao = db.blockedUserDao()
        val peerDao = db.socialPeerDao()
        val reactionDao = db.messageReactionDao()
        val chatMemberDao = db.chatMemberDao()
        val profileDao = db.driverProfileDao()
        val driverStatusDao = db.driverStatusDao()

        val chatStore = SocialChatStore(
            chatDao = chatDao,
            chatMemberDao = chatMemberDao,
            blockedUserDao = blockedUserDao,
            peerDao = peerDao,
            messageDao = messageDao,
            reactionDao = reactionDao,
            actorId = actorId,
        )
        val avatarStorage = AvatarStorage(context)
        val attachmentStorage = ChatAttachmentStorage(context)
        val recommendations = RecommendationService()

        lateinit var chatRepository: ChatRepository
        val profileRepository = ProfileRepositoryImpl(
            profileDao = profileDao,
            blockedUserDao = blockedUserDao,
            followDao = db.driverFollowDao(),
            challengeDao = db.challengeParticipationDao(),
            peerDao = peerDao,
            loadRepository = loadRepository,
            userProfileStore = userProfileStore,
            avatarStorage = avatarStorage,
            appContext = appContext,
            onPeerBlocked = { peerId -> chatRepository.archivePrivateChatForPeer(peerId) },
            actorId = actorId,
            remote = remote,
            safety = safety,
        )
        chatRepository = ChatRepositoryImpl(
            chatStore = chatStore,
            chatDao = chatDao,
            messageDao = messageDao,
            reactionDao = reactionDao,
            peerDao = peerDao,
            chatMemberDao = chatMemberDao,
            recommendations = recommendations,
            appContext = appContext,
            isBlocked = { targetId -> profileRepository.isBlocked(targetId) },
            actorId = actorId,
            remote = remote,
            inbox = inbox,
        )
        val groupRepository = GroupRepositoryImpl(
            chatDao = chatDao,
            chatMemberDao = chatMemberDao,
            messageDao = messageDao,
            profileDao = profileDao,
            appContext = appContext,
            actorId = actorId,
            remote = remote,
            inbox = inbox,
        )
        val statusRepository = StatusRepositoryImpl(
            driverStatusDao = driverStatusDao,
            blockedUserDao = blockedUserDao,
            attachmentStorage = attachmentStorage,
            appContext = appContext,
            actorId = actorId,
            remote = remote,
            storage = storage,
        )
        val mediaRepository = MediaRepositoryImpl(
            attachmentStorage = attachmentStorage,
            chatRepository = chatRepository,
            storage = storage,
            actorId = actorId,
        )
        val syncCoordinator = SocialSyncCoordinator(
            db = db,
            userProfileStore = userProfileStore,
            profileRepository = profileRepository,
            statusRepository = statusRepository,
            remote = remote,
            inbox = inbox,
        )
        val crowdRpm = CrowdRpmRepository(db = db, remote = remote)
        return Bundle(
            profile = profileRepository,
            chat = chatRepository,
            group = groupRepository,
            status = statusRepository,
            media = mediaRepository,
            syncCoordinator = syncCoordinator,
            remote = remote,
            voiceRemote = voiceRemote,
            crowdRpm = crowdRpm,
            actorId = actorId,
        )
    }
}
