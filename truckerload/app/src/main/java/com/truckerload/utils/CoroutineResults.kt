package com.truckerload.utils

import kotlinx.coroutines.CancellationException

/**
 * Like [runCatching], but cancellation still cancels the coroutine.
 * A cancelled screen must not be reported as a failed save or a missing load.
 */
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
