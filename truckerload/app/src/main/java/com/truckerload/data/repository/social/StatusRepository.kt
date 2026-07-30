package com.truckerload.data.repository.social

import android.graphics.Bitmap
import com.truckerload.domain.social.DriverStatusPost
import com.truckerload.domain.social.SocialResult
import java.io.File
import kotlinx.coroutines.flow.Flow

/** Online/offline presence and ephemeral friend statuses. */
interface StatusRepository {
    fun watchFriendStatuses(): Flow<List<DriverStatusPost>>
    suspend fun createTextStatus(text: String, displayName: String): SocialResult<Unit>
    suspend fun markStatusViewed(statusId: String): SocialResult<Unit>
    suspend fun createPhotoStatus(
        bitmap: Bitmap,
        displayName: String,
        caption: String = "",
    ): SocialResult<Unit>

    suspend fun createVoiceStatus(
        audioFile: File,
        durationMs: Long,
        displayName: String,
    ): SocialResult<Unit>

    suspend fun purgeExpired()
}
