package com.truckerload.di

import android.content.Context
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.repository.LoadRepository
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
    )

    fun create(
        context: Context,
        db: AppDatabase,
        loadRepository: LoadRepository,
        userProfileStore: UserProfileStore,
    ): Bundle {
        val appContext = context.applicationContext
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
        )
        chatRepository = ChatRepositoryImpl(
            chatStore = chatStore,
            chatDao = chatDao,
            messageDao = messageDao,
            reactionDao = reactionDao,
            peerDao = peerDao,
            recommendations = recommendations,
            appContext = appContext,
            isBlocked = { targetId -> profileRepository.isBlocked(targetId) },
        )
        val groupRepository = GroupRepositoryImpl(
            chatDao = chatDao,
            chatMemberDao = chatMemberDao,
            profileDao = profileDao,
            appContext = appContext,
        )
        val statusRepository = StatusRepositoryImpl(
            driverStatusDao = driverStatusDao,
            blockedUserDao = blockedUserDao,
            attachmentStorage = attachmentStorage,
            appContext = appContext,
        )
        val mediaRepository = MediaRepositoryImpl(
            attachmentStorage = attachmentStorage,
            chatRepository = chatRepository,
        )
        val syncCoordinator = SocialSyncCoordinator(
            db = db,
            userProfileStore = userProfileStore,
            profileRepository = profileRepository,
            statusRepository = statusRepository,
        )
        return Bundle(
            profile = profileRepository,
            chat = chatRepository,
            group = groupRepository,
            status = statusRepository,
            media = mediaRepository,
            syncCoordinator = syncCoordinator,
        )
    }
}
