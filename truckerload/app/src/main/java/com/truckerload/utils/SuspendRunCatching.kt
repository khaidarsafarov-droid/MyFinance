package com.truckerload.utils

/** Cancellation is rethrown. Only known storage, parse, and IO failures become [Result.failure]. */
inline fun <T> suspendRunCatching(block: () -> T): Result<T> =
    runReported(block = { Result.success(block()) }, onFailure = { Result.failure(it) })
