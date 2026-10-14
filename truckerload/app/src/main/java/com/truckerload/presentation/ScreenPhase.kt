package com.truckerload.presentation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/** What a screen should draw while a Room list is in flight. */
enum class ScreenPhase {
    Loading,
    Empty,
    Content,
    Error,
}

/** Room list, or the failure that should show Retry. */
fun <T> Flow<List<T>>.asListResult(): Flow<Result<List<T>>> =
    map { Result.success(it) }.catch { emit(Result.failure(it)) }

/** Resubscribe to [source] each time [this] changes, so Retry can run again. */
fun <T> MutableStateFlow<Int>.reloadList(source: () -> Flow<List<T>>): Flow<Result<List<T>>> =
    flatMapLatest { source().asListResult() }

fun Result<*>.toPhase(isEmpty: Boolean): ScreenPhase = fold(
    onSuccess = { if (isEmpty) ScreenPhase.Empty else ScreenPhase.Content },
    onFailure = { ScreenPhase.Error },
)
