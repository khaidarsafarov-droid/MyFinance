package com.truckerload.utils

import kotlinx.coroutines.CancellationException

/** [runCatching] for coroutines: cancellation is rethrown instead of becoming a failure. */
inline fun <T> suspendRunCatching(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
