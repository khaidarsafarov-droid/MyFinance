package com.truckerload.data.repository.social

import android.content.Context
import android.graphics.Bitmap
import com.truckerload.R
import com.truckerload.data.local.dao.BlockedUserDao
import com.truckerload.data.local.dao.DriverStatusDao
import com.truckerload.data.local.entities.DriverProfileEntity
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
) : StatusRepository {

    override fun watchFriendStatuses(): Flow<List<DriverStatusPost>> =
        combine(
            driverStatusDao.watchActiveStatuses(System.currentTimeMillis()),
            blockedUserDao.watchBlockedIds(DriverProfileEntity.LOCAL_USER_ID),
        ) { statuses, blockedIds ->
            val blockedSet = blockedIds.toSet()
            statuses
                .filter { it.userId !in blockedSet }
                .map { it.toDomain() }
        }.flowOn(Dispatchers.IO)

    override suspend fun createTextStatus(text: String, displayName: String): SocialResult<Unit> = runCatching {
        val now = System.currentTimeMillis()
        driverStatusDao.insert(
            DriverStatusEntity(
                id = UUID.randomUUID().toString(),
                userId = DriverProfileEntity.LOCAL_USER_ID,
                displayName = displayName,
                type = StatusType.TEXT.name,
                text = text.trim(),
                mediaPath = null,
                createdAt = now,
                expiresAt = now + SocialConstants.STATUS_TTL_MS,
                viewed = false,
            ),
        )
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_create_status, it), it) }

    override suspend fun markStatusViewed(statusId: String): SocialResult<Unit> = runCatching {
        driverStatusDao.markViewed(statusId)
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_mark_viewed, it), it) }

    override suspend fun createPhotoStatus(bitmap: Bitmap, displayName: String, caption: String): SocialResult<Unit> =
        runCatching {
            val path = attachmentStorage.saveStatusPhoto(bitmap)
            val now = System.currentTimeMillis()
            driverStatusDao.insert(
                DriverStatusEntity(
                    id = UUID.randomUUID().toString(),
                    userId = DriverProfileEntity.LOCAL_USER_ID,
                    displayName = displayName,
                    type = StatusType.PHOTO.name,
                    text = caption.ifBlank { null },
                    mediaPath = path,
                    createdAt = now,
                    expiresAt = now + SocialConstants.STATUS_TTL_MS,
                ),
            )
            SocialResult.Success(Unit)
        }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_create_status, it), it) }

    override suspend fun createVoiceStatus(audioFile: File, durationMs: Long, displayName: String): SocialResult<Unit> =
        runCatching {
            val path = attachmentStorage.saveStatusVoice(audioFile)
            val now = System.currentTimeMillis()
            driverStatusDao.insert(
                DriverStatusEntity(
                    id = UUID.randomUUID().toString(),
                    userId = DriverProfileEntity.LOCAL_USER_ID,
                    displayName = displayName,
                    type = StatusType.VOICE.name,
                    text = null,
                    mediaPath = path,
                    createdAt = now,
                    expiresAt = now + SocialConstants.STATUS_TTL_MS,
                    durationMs = durationMs,
                ),
            )
            SocialResult.Success(Unit)
        }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_create_status, it), it) }

    override suspend fun purgeExpired() {
        driverStatusDao.purgeExpired(System.currentTimeMillis())
    }
}
