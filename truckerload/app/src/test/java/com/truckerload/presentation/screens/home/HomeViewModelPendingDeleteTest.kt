package com.truckerload.presentation.screens.home

import android.app.Application
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.model.Load
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HomeViewModelPendingDeleteTest {

    private val loadsFlow = MutableStateFlow<List<Load>>(emptyList())
    private val dispatcher = UnconfinedTestDispatcher()
    private val deletedIds = mutableListOf<String>()
    private lateinit var loadRepository: LoadRepository
    private lateinit var app: Application
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        deletedIds.clear()
        loadRepository = mock()
        app = RuntimeEnvironment.getApplication()
        whenever(loadRepository.watchLoads()).thenReturn(loadsFlow)
        whenever(loadRepository.getLoadsByWeek(any(), any())).thenReturn(loadsFlow)
        whenever(loadRepository.getLoadsByDateRange(any(), any())).thenReturn(loadsFlow)
        loadRepository.stub {
            onBlocking { deleteLoad(any()) } doAnswer { inv: org.mockito.invocation.InvocationOnMock ->
                deletedIds += inv.getArgument<String>(0)
                Unit
            }
        }
        viewModel = HomeViewModel(loadRepository, app)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun requestDeleteLoad_softHidesAndSetsUndoId() {
        viewModel.requestDeleteLoad("load-1")
        assertEquals("load-1", viewModel.pendingUndoDeleteId.value)
        assertTrue("load-1" in viewModel.pendingDeleteIds.value)
        assertTrue(deletedIds.isEmpty())
    }

    @Test
    fun requestDeleteLoad_blank_isIgnored() {
        viewModel.requestDeleteLoad("  ")
        assertNull(viewModel.pendingUndoDeleteId.value)
    }

    @Test
    fun undoDeleteLoad_restoresWithoutDeleting() {
        viewModel.requestDeleteLoad("load-1")
        val genBefore = viewModel.swipeSettleGeneration.value

        viewModel.undoDeleteLoad()

        assertNull(viewModel.pendingUndoDeleteId.value)
        assertTrue(viewModel.pendingDeleteIds.value.isEmpty())
        assertTrue(viewModel.swipeSettleGeneration.value > genBefore)
        assertTrue(deletedIds.isEmpty())
    }

    @Test
    fun dismissDeleteLoad_undoesPendingDelete() {
        viewModel.requestDeleteLoad("load-1")
        viewModel.dismissDeleteLoad()
        assertNull(viewModel.pendingUndoDeleteId.value)
        assertTrue(deletedIds.isEmpty())
    }

    @Test
    fun confirmDeleteLoad_commitsImmediately() = runTest(dispatcher) {
        viewModel.requestDeleteLoad("load-1")

        viewModel.confirmDeleteLoad()

        assertNull(viewModel.pendingUndoDeleteId.value)
        assertEquals(listOf("load-1"), deletedIds)
    }

    @Test
    fun confirmDeleteLoad_withoutPending_isNoOp() = runTest(dispatcher) {
        viewModel.confirmDeleteLoad()
        assertTrue(deletedIds.isEmpty())
    }

    @Test
    fun confirmDeleteLoad_failure_surfacesDeleteError() = runTest(dispatcher) {
        loadRepository.stub {
            onBlocking { deleteLoad(any()) } doThrow RuntimeException("db locked")
        }
        viewModel.requestDeleteLoad("load-1")

        viewModel.confirmDeleteLoad()

        assertEquals("db locked", viewModel.deleteError.value)
        assertTrue(viewModel.swipeSettleGeneration.value >= 1)
    }
}
