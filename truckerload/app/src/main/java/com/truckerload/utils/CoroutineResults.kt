package com.truckerload.utils

/**
 * Like [runCatching], but cancellation still cancels the coroutine.
 * A cancelled screen must not be reported as a failed save or a missing load.
 */
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
    runReported(block = { Result.success(block()) }, onFailure = { Result.failure(it) })
