package com.truckerload.data.sync

import android.content.Context
import com.truckerload.BuildConfig
import com.truckerload.data.preferences.AccountIds
import com.truckerload.data.preferences.AuthStore

object MediaCloudPolicy {
    fun enabled(
        releaseFlag: Boolean,
        backendUrl: String,
        localOnlyMode: Boolean,
        userId: String?,
    ): Boolean =
        releaseFlag &&
            backendUrl.isNotBlank() &&
            !localOnlyMode &&
            !userId.isNullOrBlank() &&
            userId != AccountIds.LOCAL_DEV &&
            !userId.startsWith("local_") &&
            !userId.startsWith("google_")
}

class MediaCloudGate(context: Context) {
    private val authStore = AuthStore(context.applicationContext)

    fun isEnabled(): Boolean = MediaCloudPolicy.enabled(
        releaseFlag = BuildConfig.CLOUD_MEDIA_ENABLED,
        backendUrl = BuildConfig.SYNC_BACKEND_URL,
        localOnlyMode = BuildConfig.LOCAL_ONLY_MODE,
        userId = authStore.currentUserIdOrNull(),
    )
}
