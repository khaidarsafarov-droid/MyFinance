package com.truckerload.data.repository.social

import android.graphics.Bitmap
import com.truckerload.domain.social.SocialResult
import java.io.File

/** Chat attachment upload / local cache paths (image & voice). */
interface MediaRepository {
    suspend fun sendImageMessage(
        chatId: String,
        bitmap: Bitmap,
        caption: String,
        senderName: String,
    ): SocialResult<Unit>

    suspend fun sendVoiceMessage(
        chatId: String,
        audioFile: File,
        durationMs: Long,
        senderName: String,
    ): SocialResult<Unit>
}
