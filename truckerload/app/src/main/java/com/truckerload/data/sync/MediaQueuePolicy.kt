package com.truckerload.data.sync

import java.io.IOException

data class MediaQueueFailure(
    val retry: Boolean,
    val status: String,
    val attempts: Int,
    val safeError: String,
)

object MediaQueuePolicy {
    fun afterFailure(previousAttempts: Int, error: Throwable): MediaQueueFailure {
        val cloud = error as? MediaCloudException
        val validation = error as? MediaValidationException
        val retry = when {
            validation != null -> false
            cloud != null -> cloud.retryable
            error is IOException -> true
            else -> false
        }
        val safeCode = when {
            validation != null -> validation.code
            cloud != null -> cloud.errorCode
            error is IOException -> "io_error"
            else -> "unexpected_error"
        }.take(64)
        return MediaQueueFailure(
            retry = retry,
            status = if (retry) "PENDING" else "FAILED",
            attempts = previousAttempts + 1,
            safeError = safeCode,
        )
    }
}
