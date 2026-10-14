package com.truckerload.presentation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenPhaseTest {

    @Test
    fun phase_isEmptyContentOrError() {
        assertEquals(ScreenPhase.Empty, Result.success(emptyList<Int>()).toPhase(isEmpty = true))
        assertEquals(ScreenPhase.Content, Result.success(listOf(1)).toPhase(isEmpty = false))
        assertEquals(
            ScreenPhase.Error,
            Result.failure<List<Int>>(IllegalStateException("db")).toPhase(isEmpty = true),
        )
    }

    @Test
    fun reload_resubscribesAfterFailure() = runTest {
        var attempts = 0
        val reload = MutableStateFlow(0)
        val results = mutableListOf<Result<List<Int>>>()
        val job = launch {
            reload.reloadList<Int> {
                flow {
                    attempts += 1
                    if (attempts == 1) error("closed")
                    emit(listOf(4))
                }
            }.collect { results += it }
        }
        advanceUntilIdle()
        assertEquals(1, attempts)
        assertTrue(results.single().isFailure)
        reload.value = 1
        advanceUntilIdle()
        job.cancel()
        assertEquals(2, attempts)
        assertEquals(listOf(4), results.last().getOrNull())
    }

    @Test
    fun asListResult_wrapsTheList() = runTest {
        val results = flowOf(listOf("a")).asListResult().toList()
        assertEquals(listOf("a"), results.single().getOrNull())
    }
}
