package com.truckerload.data.repository.social

import android.content.Context
import android.graphics.Bitmap
import com.truckerload.R
import com.truckerload.data.community.CommunityRemoteClient
import com.truckerload.data.community.CommunityStorageClient
import com.truckerload.data.local.dao.BlockedUserDao
import com.truckerload.data.local.dao.DriverStatusDao
import com.truckerload.data.local.entities.DriverStatusEntity
import com.truckerload.data.repository.toDomain
import com.truckerload.data.social.ChatAttachmentStorage
import com.truckerload.di.UserScope
import com.truckerload.domain.social.DriverStatusPost
import com.truckerload.domain.social.SocialResult
import com.truckerload.domain.social.StatusType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.util.UUID

@UserScope
class StatusRepositoryImpl(
    private val driverStatusDao: DriverStatusDao,
    private val blockedUserDao: BlockedUserDao,
    private val attachmentStorage: ChatAttachmentStorage,
    private val appContext: Context,
    private val actorId: () -> String,
    private val remote: CommunityRemoteClient,
    private val storage: CommunityStorageClient,
) : StatusRepository {

    override fun watchFriendStatuses(): Flow<List<DriverStatusPost>> {
        val me = actorId()
        return combine(
            driverStatusDao.watchActiveStatuses(System.currentTimeMillis()),
            blockedUserDao.watchBlockedIds(me),
        ) { statuses, blockedIds ->
            val blockedSet = blockedIds.toSet()
            statuses
                .filter { it.userId !in blockedSet }
                .map { it.toDomain() }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun createTextStatus(text: String, displayName: String): SocialResult<Unit> =
        persistStatus(
            displayName = displayName,
            type = StatusType.TEXT,
            text = text.trim(),
            mediaPath = null,
            durationMs = 0,
        )

    override suspend fun markStatusViewed(statusId: String): SocialResult<Unit> = runCatching {
        driverStatusDao.markViewed(statusId)
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_mark_viewed, it), it) }

    override suspend fun createPhotoStatus(
        bitmap: Bitmap,
        displayName: String,
        caption: String
    ): SocialResult<Unit> {
        val path = attachmentStorage.saveStatusPhoto(bitmap)
        val uploaded = uploadIfRemote(path, "status", "image/jpeg")
        return persistStatus(
            displayName,
            StatusType.PHOTO,
            caption.ifBlank { null },
            uploaded ?: path,
            0
        )
    }

    override suspend fun createVoiceStatus(
        audioFile: File,
        durationMs: Long,
        displayName: String
    ): SocialResult<Unit> {
        val path = attachmentStorage.saveStatusVoice(audioFile)
        val uploaded = uploadIfRemote(path, "status", "audio/mp4")
        return persistStatus(displayName, StatusType.VOICE, null, uploaded ?: path, durationMs)
    }

    override suspend fun purgeExpired() {
        driverStatusDao.purgeExpired(System.currentTimeMillis())
    }

    private suspend fun persistStatus(
        displayName: String,
        type: StatusType,
        text: String?,
        mediaPath: String?,
        durationMs: Long,
    ): SocialResult<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val expires = now + SocialConstants.STATUS_TTL_MS
        val me = actorId()
        if (remote.isReady()) {
            remote.createStatus(
                id = id,
                displayName = displayName,
                type = type.name,
                text = text,
                mediaPath = mediaPath,
                durationMs = durationMs,
                createdAt = now,
                expiresAt = expires,
            ).onFailure { err ->
                return SocialResult.Error(
                    socialError(
                        appContext,
                        R.string.social_error_create_status,
                        err
                    ), err
                )
            }
        }
        driverStatusDao.insert(
            DriverStatusEntity(
                id = id,
                userId = me,
                displayName = displayName,
                type = type.name,
                text = text,
                mediaPath = mediaPath,
                createdAt = now,
                expiresAt = expires,
                viewed = false,
                durationMs = durationMs,
            ),
        )
        SocialResult.Success(Unit)
    }.getOrElse {
        SocialResult.Error(
            socialError(
                appContext,
                R.string.social_error_create_status,
                it
            ), it
        )
    }

    private suspend fun uploadIfRemote(localPath: String, folder: String, mime: String): String? {
        if (!storage.isReady()) return null
        val file = File(localPath)
        if (!file.exists()) return null
        val objectPath = "${actorId()}/$folder/${file.name}"
        return storage.upload(file, objectPath, mime).getOrNull()
    }
}
