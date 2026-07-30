package com.truckerload.data.repository.social

import android.content.Context
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.SocialChatStore
import com.truckerload.data.repository.SocialRepository
import com.truckerload.data.repository.SocialSeedHelper
import com.truckerload.data.social.AvatarStorage
import com.truckerload.data.social.ChatAttachmentStorage

/** Builds the account-scoped social repository stack for [com.truckerload.di.UserComponent]. */
@Suppress("DEPRECATION")
internal object SocialStackFactory {
    fun create(
        db: AppDatabase,
        loadRepository: LoadRepository,
        userProfileStore: UserProfileStore,
        context: Context,
    ): SocialRepository {
        val profileDao = db.driverProfileDao()
        val chatDao = db.socialChatDao()
        val messageDao = db.socialMessageDao()
        val blockedUserDao = db.blockedUserDao()
        val driverStatusDao = db.driverStatusDao()
        val challengeDao = db.challengeParticipationDao()
        val reactionDao = db.messageReactionDao()
        val followDao = db.driverFollowDao()
        val chatMemberDao = db.chatMemberDao()
        val peerDao = db.socialPeerDao()
        val seedHelper = SocialSeedHelper(
            chatDao = chatDao,
            chatMemberDao = chatMemberDao,
            driverStatusDao = driverStatusDao,
        )
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
        val profile = ProfileRepositoryImpl(
            profileDao = profileDao,
            loadRepository = loadRepository,
            userProfileStore = userProfileStore,
            avatarStorage = avatarStorage,
            socialGraph = ProfileSocialGraph(
                profileDao = profileDao,
                blockedUserDao = blockedUserDao,
                followDao = followDao,
                peerDao = peerDao,
                chatDao = chatDao,
                context = context,
            ),
            context = context,
        )
        val chat = ChatRepositoryImpl(
            chatDao = chatDao,
            messageDao = messageDao,
            reactionDao = reactionDao,
            blockedUserDao = blockedUserDao,
            peerDao = peerDao,
            chatStore = chatStore,
            context = context,
        )
        val group = GroupRepositoryImpl(
            chatDao = chatDao,
            chatMemberDao = chatMemberDao,
            profileDao = profileDao,
            chatStore = chatStore,
            context = context,
        )
        val status = StatusRepositoryImpl(
            driverStatusDao = driverStatusDao,
            blockedUserDao = blockedUserDao,
            attachmentStorage = attachmentStorage,
            context = context,
        )
        val media = MediaRepositoryImpl(
            chatRepository = chat,
            attachmentStorage = attachmentStorage,
        )
        val sync = SocialSyncCoordinator(
            profileDao = profileDao,
            chatDao = chatDao,
            messageDao = messageDao,
            peerDao = peerDao,
            blockedUserDao = blockedUserDao,
            challengeDao = challengeDao,
            loadRepository = loadRepository,
            userProfileStore = userProfileStore,
            profileRepository = profile,
            statusRepository = status,
            seedHelper = seedHelper,
            context = context,
        )
        return SocialRepository(
            profileRepository = profile,
            chatRepository = chat,
            groupRepository = group,
            statusRepository = status,
            mediaRepository = media,
            syncCoordinator = sync,
        )
    }
}
