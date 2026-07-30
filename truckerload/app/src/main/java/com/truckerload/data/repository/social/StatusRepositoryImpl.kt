package com.truckerload.data.repository.social

import android.content.Context
import android.graphics.Bitmap
import com.truckerload.R
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.DriverProfileEntity
import com.truckerload.data.local.entities.DriverStatusEntity
import com.truckerload.di.UserScope
import com.truckerload.domain.social.DriverStatus
import com.truckerload.domain.social.DriverStatusPost
import com.truckerload.domain.social.SocialResult
import com.truckerload.domain.social.StatusType
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@UserScope
class StatusRepositoryImpl @Inject constructor(
    db: AppDatabase,
    private val mediaRepository: MediaRepository,
    context: Context,
) : StatusRepository {
    private val profileDao = db.driverProfileDao()
    private val driverStatusDao = db.driverStatusDao()
    private val blockedUserDao = db.blockedUserDao()
    private val appContext = context.applicationContext

    override suspend fun updateStatus(status: DriverStatus): SocialResult<Unit> = runCatching {
        val existing = profileDao.getProfile() ?: DriverProfileEntity()
        profileDao.upsert(existing.copy(status = status.name))
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_update_status, it), it) }

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

    override suspend fun createTextStatus(text: String, displayName: String): SocialResult<Unit> =
        runCatching {
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
                    expiresAt = now + STATUS_TTL_MS,
                    viewed = false,
                ),
            )
            SocialResult.Success(Unit)
        }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_create_status, it), it) }

    override suspend fun createPhotoStatus(
        bitmap: Bitmap,
        displayName: String,
        caption: String,
    ): SocialResult<Unit> = runCatching {
        val path = mediaRepository.saveStatusPhoto(bitmap)
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
                expiresAt = now + STATUS_TTL_MS,
            ),
        )
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_create_status, it), it) }

    override suspend fun createVoiceStatus(
        audioFile: File,
        durationMs: Long,
        displayName: String,
    ): SocialResult<Unit> = runCatching {
        val path = mediaRepository.saveStatusVoice(audioFile)
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
                expiresAt = now + STATUS_TTL_MS,
                durationMs = durationMs,
            ),
        )
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_create_status, it), it) }

    override suspend fun markStatusViewed(statusId: String): SocialResult<Unit> = runCatching {
        driverStatusDao.markViewed(statusId)
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_mark_viewed, it), it) }

    override suspend fun purgeExpired() {
        driverStatusDao.purgeExpired(System.currentTimeMillis())
    }

    companion object {
        const val STATUS_TTL_MS = 24 * 60 * 60_000L
    }
}
